package com.hexapass.domain.model;

import com.hexapass.domain.common.DateRange;
import com.hexapass.domain.type.MemberStatus;
import com.hexapass.domain.type.ResourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 회원을 나타내는 엔티티
 * 서비스를 이용하는 고객으로, 고유한 식별자와 멤버십을 가짐
 * memberId를 기준으로 동일성 판단
 */
public class Member {

    private final String memberId;
    private final String name;
    private final String email;
    private final String phone;
    private final LocalDateTime createdAt;
    private MemberStatus status;
    private MembershipPlan currentPlan;
    private DateRange membershipPeriod;
    private LocalDateTime lastStatusChangedAt;
    private String suspensionReason;

    // 이메일과 전화번호 검증용 정규표현식
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$");

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private Member(String memberId, String name, String email, String phone) {
        this.memberId = validateNotBlank(memberId, "회원 ID");
        this.name = validateNotBlank(name, "회원명");
        this.email = validateEmail(email);
        this.phone = validatePhone(phone);
        this.createdAt = LocalDateTime.now();
        this.status = MemberStatus.ACTIVE; // 기본값: 활성
        this.lastStatusChangedAt = this.createdAt;
    }

    /**
     * 회원 생성 팩토리 메서드
     */
    public static Member create(String memberId, String name, String email, String phone) {
        return new Member(memberId, name, email, phone);
    }

    // =========================
    // 멤버십 관리 메서드들
    // =========================

    /**
     * 멤버십 할당 (최초 가입 또는 플랜 변경)
     */
    public void assignMembership(MembershipPlan plan, DateRange period) {
        validateNotNull(plan, "멤버십 플랜");
        validateNotNull(period, "멤버십 기간");
        validateMembershipPeriod(period);
        validatePlanIsActive(plan);

        this.currentPlan = plan;
        this.membershipPeriod = period;
    }

    /**
     * 멤버십 연장
     */
    public void extendMembership(int additionalDays) {
        if (currentPlan == null || membershipPeriod == null) {
            throw new IllegalStateException("멤버십이 할당되지 않은 상태에서는 연장할 수 없습니다");
        }

        if (additionalDays <= 0) {
            throw new IllegalArgumentException("연장 일수는 0보다 커야 합니다. 입력값: " + additionalDays);
        }

        this.membershipPeriod = membershipPeriod.extend(additionalDays);
    }

    /**
     * 멤버십 플랜 변경 (기간 유지)
     */
    public void changePlan(MembershipPlan newPlan) {
        validateNotNull(newPlan, "새로운 멤버십 플랜");
        validatePlanIsActive(newPlan);

        if (membershipPeriod == null) {
            throw new IllegalStateException("멤버십 기간이 설정되지 않은 상태에서는 플랜을 변경할 수 없습니다");
        }

        this.currentPlan = newPlan;
    }

    /**
     * 멤버십 만료 여부 확인
     */
    public boolean isMembershipExpired() {
        if (membershipPeriod == null) {
            return true; // 멤버십이 없으면 만료된 것으로 간주
        }
        return membershipPeriod.isPast();
    }

    /**
     * 멤버십 활성 여부 확인 (상태 + 기간 모두 고려)
     */
    public boolean hasMembershipActive() {
        return status == MemberStatus.ACTIVE &&
                currentPlan != null &&
                currentPlan.isActive() &&
                membershipPeriod != null &&
                !isMembershipExpired();
    }

    // =========================
    // 예약 권한 확인 메서드들
    // =========================

    /**
     * 예약 가능 여부 확인 (포괄적 검사)
     */
    public boolean canMakeReservation() {
        return hasMembershipActive();
    }

    /**
     * 특정 리소스 타입 예약 권한 확인
     */
    public boolean canReserve(ResourceType resourceType, LocalDate reservationDate) {
        if (!canMakeReservation()) {
            return false;
        }

        // 예약 날짜가 멤버십 기간 내인지 확인
        if (!membershipPeriod.contains(reservationDate)) {
            return false;
        }

        // 플랜에서 해당 리소스 타입 이용 권한 확인
        return currentPlan.hasPrivilege(resourceType);
    }

    /**
     * 선예약 가능 여부 확인
     */
    public boolean canReserveInAdvance(LocalDate reservationDate) {
        if (!canMakeReservation()) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (!reservationDate.isAfter(today)) {
            return true; // 오늘이거나 과거 날짜는 선예약 제한 없음
        }

        int daysFromToday = (int) today.until(reservationDate).getDays();
        return currentPlan.canReserveInAdvance(daysFromToday);
    }

    /**
     * 동시 예약 가능 여부 확인
     */
    public boolean canReserveSimultaneously(int currentActiveReservations) {
        if (!canMakeReservation()) {
            return false;
        }

        return currentPlan.canReserve(currentActiveReservations);
    }

    // =========================
    // 회원 상태 관리 메서드들
    // =========================

    /**
     * 회원 정지
     */
    public void suspend(String reason) {
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴한 회원은 정지할 수 없습니다");
        }

        if (!status.canTransitionTo(MemberStatus.SUSPENDED)) {
            throw new IllegalStateException("현재 상태에서 정지 상태로 전환할 수 없습니다");
        }

        this.status = MemberStatus.SUSPENDED;
        this.suspensionReason = validateNotBlank(reason, "정지 사유");
        this.lastStatusChangedAt = LocalDateTime.now();
    }

    /**
     * 회원 활성화 (정지 해제)
     */
    public void activate() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴한 회원은 활성화할 수 없습니다");
        }

        if (!status.canTransitionTo(MemberStatus.ACTIVE)) {
            throw new IllegalStateException("현재 상태에서 활성 상태로 전환할 수 없습니다");
        }

        this.status = MemberStatus.ACTIVE;
        this.suspensionReason = null; // 정지 사유 초기화
        this.lastStatusChangedAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴
     */
    public void withdraw() {
        if (!status.canTransitionTo(MemberStatus.WITHDRAWN)) {
            throw new IllegalStateException("현재 상태에서 탈퇴할 수 없습니다");
        }

        this.status = MemberStatus.WITHDRAWN;
        this.lastStatusChangedAt = LocalDateTime.now();

        // 탈퇴 시 멤버십 정보는 유지 (이력 관리용)
        // 실제로는 예약 등 다른 비즈니스 로직에서 탈퇴 회원의 행위를 제한
    }

    // =========================
    // 정보 조회 메서드들
    // =========================

    /**
     * 회원 정보 요약
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("회원 %s (%s) - %s", name, memberId, status.getDisplayName()));

        if (currentPlan != null && membershipPeriod != null) {
            summary.append(String.format(" | 플랜: %s (%s)",
                    currentPlan.getName(), membershipPeriod));
        }

        if (status == MemberStatus.SUSPENDED && suspensionReason != null) {
            summary.append(String.format(" | 정지사유: %s", suspensionReason));
        }

        return summary.toString();
    }

    /**
     * 멤버십 남은 일수 계산
     */
    public int getRemainingMembershipDays() {
        if (membershipPeriod == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        if (membershipPeriod.getEndDate().isBefore(today)) {
            return 0; // 이미 만료됨
        }

        if (membershipPeriod.getStartDate().isAfter(today)) {
            return (int) membershipPeriod.getDays(); // 아직 시작 전
        }

        return (int) today.until(membershipPeriod.getEndDate()).getDays() + 1;
    }

    /**
     * 멤버십 만료까지 남은 일수가 경고 수준인지 확인
     */
    public boolean isMembershipExpiryWarning(int warningDays) {
        int remainingDays = getRemainingMembershipDays();
        return remainingDays > 0 && remainingDays <= warningDays;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Member member = (Member) obj;
        return Objects.equals(memberId, member.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }

    @Override
    public String toString() {
        return String.format("Member{id='%s', name='%s', email='%s', status=%s}",
                memberId, name, email, status);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public MembershipPlan getCurrentPlan() {
        return currentPlan;
    }

    public DateRange getMembershipPeriod() {
        return membershipPeriod;
    }

    public LocalDateTime getLastStatusChangedAt() {
        return lastStatusChangedAt;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }

    private String validateEmail(String email) {
        String cleanEmail = validateNotBlank(email, "이메일");
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다: " + cleanEmail);
        }
        return cleanEmail;
    }

    private String validatePhone(String phone) {
        String cleanPhone = validateNotBlank(phone, "전화번호");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("전화번호는 010-XXXX-XXXX 형식이어야 합니다: " + cleanPhone);
        }
        return cleanPhone;
    }

    private void validateMembershipPeriod(DateRange period) {
        LocalDate today = LocalDate.now();
        if (period.getEndDate().isBefore(today)) {
            throw new IllegalArgumentException("멤버십 종료일은 현재 날짜 이후여야 합니다");
        }
    }

    private void validatePlanIsActive(MembershipPlan plan) {
        if (!plan.isActive()) {
            throw new IllegalArgumentException("비활성화된 플랜은 할당할 수 없습니다: " + plan.getName());
        }
    }
}