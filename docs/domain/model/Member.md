# Member.java - 상세 주석 및 설명

## 클래스 개요
`Member`는 서비스를 이용하는 회원을 나타내는 **엔티티(Entity)**입니다.
`memberId`를 기준으로 동일성을 판단하며, 멤버십과 예약 권한 관리를 담당합니다.

## 엔티티 vs 값 객체의 차이점
- **엔티티**: 식별자(ID)로 구별, 상태 변경 가능, 생명주기 존재
- **값 객체**: 값으로 구별, 불변, 교체 가능

Member는 회원 ID로 식별되고, 상태(멤버십, 상태 등)가 변경될 수 있어 엔티티입니다.

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.model;

import com.hexapass.domain.common.DateRange; // 값 객체 - 날짜 범위
import com.hexapass.domain.type.MemberStatus; // 열거형 - 회원 상태
import com.hexapass.domain.type.ResourceType; // 열거형 - 리소스 타입

import java.time.LocalDate; // 날짜를 나타내는 불변 클래스
import java.time.LocalDateTime; // 날짜+시간을 나타내는 불변 클래스
import java.util.Objects; // equals, hashCode 유틸리티
import java.util.regex.Pattern; // 정규표현식 패턴 매칭

/**
 * 회원을 나타내는 엔티티
 * 서비스를 이용하는 고객으로, 고유한 식별자와 멤버십을 가짐
 * memberId를 기준으로 동일성 판단
 * 
 * 엔티티 특징:
 * 1. 식별자 기반 동일성: memberId로 구분
 * 2. 가변 상태: 멤버십, 상태 등이 변경 가능
 * 3. 생명주기: 생성→활성→정지→탈퇴의 생명주기
 * 4. 비즈니스 로직 포함: 예약 권한, 상태 전환 등
 */
public class Member {

    // === 불변 필드들 (생성 후 변경 불가) ===
    private final String memberId;        // 회원 고유 식별자 (PK)
    private final String name;            // 회원명 (실명)
    private final String email;           // 이메일 주소 (로그인 ID 겸용)
    private final String phone;           // 전화번호 (연락처)
    private final LocalDateTime createdAt; // 가입 일시 (audit 정보)

    // === 가변 필드들 (비즈니스 로직에 의해 변경됨) ===
    private MemberStatus status;           // 현재 상태 (활성/정지/탈퇴)
    private MembershipPlan currentPlan;    // 현재 멤버십 플랜
    private DateRange membershipPeriod;    // 멤버십 유효 기간
    private LocalDateTime lastStatusChangedAt; // 마지막 상태 변경 일시
    private String suspensionReason;       // 정지 사유 (정지 상태일 때만 사용)

    // === 검증용 정규표현식 (정적 상수) ===
    // Pattern.compile(): 정규표현식을 컴파일하여 재사용 가능한 Pattern 객체 생성
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // 한국 휴대폰 번호 형식: 010-1234-5678
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$");

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * 엔티티도 생성 시 유효성을 보장하기 위해 private 생성자 사용
     * 
     * @param memberId 회원 고유 ID
     * @param name 회원명
     * @param email 이메일 주소
     * @param phone 전화번호
     */
    private Member(String memberId, String name, String email, String phone) {
        // 각 필드 검증 후 할당
        this.memberId = validateNotBlank(memberId, "회원 ID");
        this.name = validateNotBlank(name, "회원명");
        this.email = validateEmail(email);      // 이메일 형식 검증
        this.phone = validatePhone(phone);      // 전화번호 형식 검증
        
        // LocalDateTime.now(): 현재 시스템 시간 (서버 시간대 기준)
        this.createdAt = LocalDateTime.now();
        
        // 기본값 설정: 신규 가입자는 활성 상태로 시작
        this.status = MemberStatus.ACTIVE;
        this.lastStatusChangedAt = this.createdAt; // 생성 시점을 마지막 변경으로 설정
    }

    /**
     * 회원 생성 팩토리 메서드
     * 
     * 정적 팩토리 메서드 패턴:
     * 1. 의미 명확화: create() 메서드명으로 생성 의도 표현
     * 2. 유효성 검사 강제: private 생성자로 모든 생성 경로 통제
     * 3. 확장 가능성: 향후 다양한 생성 방법 추가 가능
     */
    public static Member create(String memberId, String name, String email, String phone) {
        return new Member(memberId, name, email, phone);
    }

    // =========================
    // 멤버십 관리 메서드들
    // =========================

    /**
     * 멤버십 할당 (최초 가입 또는 플랜 변경)
     * 
     * 비즈니스 로직: 멤버십과 기간을 함께 설정
     * 원자성 보장: 둘 다 성공하거나 둘 다 실패
     * 
     * @param plan 할당할 멤버십 플랜
     * @param period 멤버십 유효 기간
     */
    public void assignMembership(MembershipPlan plan, DateRange period) {
        // null 체크: 필수 파라미터 검증
        validateNotNull(plan, "멤버십 플랜");
        validateNotNull(period, "멤버십 기간");
        
        // 비즈니스 규칙 검증
        validateMembershipPeriod(period);  // 기간이 유효한지 확인
        validatePlanIsActive(plan);        // 플랜이 활성 상태인지 확인

        // 검증 완료 후 상태 변경
        this.currentPlan = plan;
        this.membershipPeriod = period;
    }

    /**
     * 멤버십 연장
     * 
     * 기존 멤버십 기간을 연장하는 기능
     * 
     * @param additionalDays 추가할 일수 (양수)
     */
    public void extendMembership(int additionalDays) {
        // 전제조건 확인: 멤버십이 이미 할당되어 있어야 함
        if (currentPlan == null || membershipPeriod == null) {
            throw new IllegalStateException("멤버십이 할당되지 않은 상태에서는 연장할 수 없습니다");
        }

        // 가드 클로즈: 잘못된 입력에 대한 빠른 실패
        if (additionalDays <= 0) {
            throw new IllegalArgumentException("연장 일수는 0보다 커야 합니다. 입력값: " + additionalDays);
        }

        // DateRange.extend(): 불변 객체이므로 새 객체 반환
        this.membershipPeriod = membershipPeriod.extend(additionalDays);
    }

    /**
     * 멤버십 플랜 변경 (기간 유지)
     * 
     * 업그레이드/다운그레이드 시 사용
     * 기존 기간은 유지하고 플랜만 변경
     * 
     * @param newPlan 변경할 새로운 플랜
     */
    public void changePlan(MembershipPlan newPlan) {
        validateNotNull(newPlan, "새로운 멤버십 플랜");
        validatePlanIsActive(newPlan);

        // 기간이 설정되지 않은 상태에서는 플랜 변경 불가
        if (membershipPeriod == null) {
            throw new IllegalStateException("멤버십 기간이 설정되지 않은 상태에서는 플랜을 변경할 수 없습니다");
        }

        this.currentPlan = newPlan;
    }

    /**
     * 멤버십 만료 여부 확인
     * 
     * @return 만료되었으면 true, 아니면 false
     */
    public boolean isMembershipExpired() {
        if (membershipPeriod == null) {
            return true; // 멤버십이 없으면 만료된 것으로 간주
        }
        // DateRange.isPast(): 종료일이 현재 날짜보다 이전인지 확인
        return membershipPeriod.isPast();
    }

    /**
     * 멤버십 활성 여부 확인 (상태 + 기간 모두 고려)
     * 
     * 종합적인 멤버십 유효성 검사:
     * 1. 회원 상태가 활성인가?
     * 2. 플랜이 존재하고 활성인가?
     * 3. 기간이 설정되어 있고 만료되지 않았는가?
     * 
     * @return 모든 조건을 만족하면 true
     */
    public boolean hasMembershipActive() {
        return status == MemberStatus.ACTIVE &&          // 회원 상태 확인
                currentPlan != null &&                    // 플랜 존재 확인
                currentPlan.isActive() &&                 // 플랜 활성 상태 확인
                membershipPeriod != null &&               // 기간 존재 확인
                !isMembershipExpired();                   // 만료되지 않음 확인
    }

    // =========================
    // 예약 권한 확인 메서드들
    // =========================

    /**
     * 예약 가능 여부 확인 (포괄적 검사)
     * 
     * 가장 기본적인 예약 권한 확인
     * 다른 예약 권한 메서드들의 기본 조건
     * 
     * @return 예약이 가능하면 true
     */
    public boolean canMakeReservation() {
        return hasMembershipActive(); // 멤버십 활성 상태가 예약의 전제조건
    }

    /**
     * 특정 리소스 타입 예약 권한 확인
     * 
     * 세부적인 예약 권한 검사:
     * 1. 기본 예약 권한이 있는가?
     * 2. 예약 날짜가 멤버십 기간 내인가?
     * 3. 플랜에서 해당 리소스 이용 권한이 있는가?
     * 
     * @param resourceType 예약하려는 리소스 타입
     * @param reservationDate 예약 날짜
     * @return 예약 권한이 있으면 true
     */
    public boolean canReserve(ResourceType resourceType, LocalDate reservationDate) {
        if (!canMakeReservation()) {
            return false; // 기본 예약 권한 없음
        }

        // 예약 날짜가 멤버십 기간 내인지 확인
        if (!membershipPeriod.contains(reservationDate)) {
            return false; // 멤버십 기간 밖의 날짜
        }

        // 플랜에서 해당 리소스 타입 이용 권한 확인
        return currentPlan.hasPrivilege(resourceType);
    }

    /**
     * 선예약 가능 여부 확인
     * 
     * 미래 날짜에 대한 예약 가능성 검사
     * 플랜별로 다른 선예약 허용 일수 적용
     * 
     * @param reservationDate 예약하려는 날짜
     * @return 선예약이 가능하면 true
     */
    public boolean canReserveInAdvance(LocalDate reservationDate) {
        if (!canMakeReservation()) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (!reservationDate.isAfter(today)) {
            return true; // 오늘이거나 과거 날짜는 선예약 제한 없음
        }

        // Period.between().getDays(): 두 날짜 사이의 일수 계산
        int daysFromToday = (int) today.until(reservationDate).getDays();
        return currentPlan.canReserveInAdvance(daysFromToday);
    }

    /**
     * 동시 예약 가능 여부 확인
     * 
     * 현재 활성 예약 수를 기준으로 추가 예약 가능성 판단
     * 
     * @param currentActiveReservations 현재 활성 예약 수
     * @return 동시 예약이 가능하면 true
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
     * 
     * 상태 전환 + 정지 사유 기록 + 시점 기록
     * 
     * @param reason 정지 사유 (필수)
     */
    public void suspend(String reason) {
        // 탈퇴한 회원은 정지할 수 없음 (비즈니스 규칙)
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴한 회원은 정지할 수 없습니다");
        }

        // enum에 정의된 상태 전환 규칙 확인
        if (!status.canTransitionTo(MemberStatus.SUSPENDED)) {
            throw new IllegalStateException("현재 상태에서 정지 상태로 전환할 수 없습니다");
        }

        // 상태 변경 실행
        this.status = MemberStatus.SUSPENDED;
        this.suspensionReason = validateNotBlank(reason, "정지 사유");
        this.lastStatusChangedAt = LocalDateTime.now(); // 변경 시점 기록
    }

    /**
     * 회원 활성화 (정지 해제)
     * 
     * 정지 상태에서 활성 상태로 복귀
     */
    public void activate() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴한 회원은 활성화할 수 없습니다");
        }

        if (!status.canTransitionTo(MemberStatus.ACTIVE)) {
            throw new IllegalStateException("현재 상태에서 활성 상태로 전환할 수 없습니다");
        }

        this.status = MemberStatus.ACTIVE;
        this.suspensionReason = null; // 정지 사유 초기화 (더 이상 필요 없음)
        this.lastStatusChangedAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴
     * 
     * 최종 상태로의 전환 (복구 불가)
     */
    public void withdraw() {
        if (!status.canTransitionTo(MemberStatus.WITHDRAWN)) {
            throw new IllegalStateException("현재 상태에서 탈퇴할 수 없습니다");
        }

        this.status = MemberStatus.WITHDRAWN;
        this.lastStatusChangedAt = LocalDateTime.now();

        // 탈퇴 시 멤버십 정보는 유지 (이력 관리용)
        // 실제로는 예약 등 다른 비즈니스 로직에서 탈퇴 회원의 행위를 제한
        // 데이터는 남겨두되, 기능상 제한을 가하는 소프트 딜리트 패턴
    }

    // =========================
    // 정보 조회 메서드들
    // =========================

    /**
     * 회원 정보 요약
     * 
     * 디버깅, 로깅, 관리자 화면 등에서 사용할 요약 정보
     * 
     * @return 회원 정보 요약 문자열
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        // String.format(): printf 스타일 문자열 포매팅
        summary.append(String.format("회원 %s (%s) - %s", name, memberId, status.getDisplayName()));

        // 멤버십 정보 추가 (있는 경우)
        if (currentPlan != null && membershipPeriod != null) {
            summary.append(String.format(" | 플랜: %s (%s)",
                    currentPlan.getName(), membershipPeriod));
        }

        // 정지 사유 추가 (정지 상태인 경우)
        if (status == MemberStatus.SUSPENDED && suspensionReason != null) {
            summary.append(String.format(" | 정지사유: %s", suspensionReason));
        }

        return summary.toString();
    }

    /**
     * 멤버십 남은 일수 계산
     * 
     * 사용자에게 보여줄 남은 기간 정보
     * 
     * @return 남은 일수 (0 이상)
     */
    public int getRemainingMembershipDays() {
        if (membershipPeriod == null) {
            return 0; // 멤버십이 없으면 0일
        }

        LocalDate today = LocalDate.now();
        
        // 이미 만료된 경우
        if (membershipPeriod.getEndDate().isBefore(today)) {
            return 0;
        }

        // 아직 시작하지 않은 경우 (미래의 멤버십)
        if (membershipPeriod.getStartDate().isAfter(today)) {
            return (int) membershipPeriod.getDays(); // 전체 기간 반환
        }

        // Period.between().getDays(): 날짜 차이 계산
        // +1: 오늘도 포함해서 계산 (예: 오늘이 마지막 날이면 1일 남음)
        return (int) today.until(membershipPeriod.getEndDate()).getDays() + 1;
    }

    /**
     * 멤버십 만료까지 남은 일수가 경고 수준인지 확인
     * 
     * UI에서 경고 메시지 표시 여부 결정에 활용
     * 
     * @param warningDays 경고할 일수 (예: 7일 전부터 경고)
     * @return 경고 수준이면 true
     */
    public boolean isMembershipExpiryWarning(int warningDays) {
        int remainingDays = getRemainingMembershipDays();
        // 남은 일수가 0보다 크고 경고 일수 이하인 경우
        return remainingDays > 0 && remainingDays <= warningDays;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    /**
     * equals 메서드 오버라이드
     * 
     * 엔티티의 동일성: 식별자(memberId) 기준으로만 판단
     * 다른 속성이 달라도 ID가 같으면 같은 회원으로 취급
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // 같은 참조면 true
        if (obj == null || getClass() != obj.getClass()) return false; // null이거나 다른 클래스면 false

        Member member = (Member) obj;
        // 식별자만으로 동일성 판단 (엔티티의 특징)
        return Objects.equals(memberId, member.memberId);
    }

    /**
     * hashCode 메서드 오버라이드
     * 
     * equals에서 사용한 필드와 동일하게 memberId만 사용
     * HashMap, HashSet에서의 올바른 동작 보장
     */
    @Override
    public int hashCode() {
        return Objects.hash(memberId); // 식별자만으로 해시코드 생성
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 디버깅과 로깅에 유용한 간결한 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("Member{id='%s', name='%s', email='%s', status=%s}",
                memberId, name, email, status);
    }

    // =========================
    // Getter 메서드들
    // =========================

    /**
     * 회원 ID 반환
     * 
     * 엔티티의 식별자 - 외부에서 참조할 때 사용
     */
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

    /**
     * 현재 플랜 반환
     * 
     * null일 수 있음 - 호출자가 null 체크 필요
     */
    public MembershipPlan getCurrentPlan() {
        return currentPlan;
    }

    /**
     * 멤버십 기간 반환
     * 
     * DateRange는 불변 객체이므로 방어적 복사 불필요
     */
    public DateRange getMembershipPeriod() {
        return membershipPeriod;
    }

    public LocalDateTime getLastStatusChangedAt() {
        return lastStatusChangedAt;
    }

    /**
     * 정지 사유 반환
     * 
     * 정지 상태가 아닐 때는 null
     */
    public String getSuspensionReason() {
        return suspensionReason;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    /**
     * 문자열이 null이거나 공백이 아닌지 검증
     * 
     * @param value 검증할 문자열
     * @param fieldName 필드명 (오류 메시지용)
     * @return trim된 유효한 문자열
     */
    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim(); // 앞뒤 공백 제거
    }

    /**
     * 객체가 null이 아닌지 검증
     * 
     * 제네릭 메서드: 어떤 타입이든 사용 가능
     */
    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }

    /**
     * 이메일 형식 검증
     * 
     * 정규표현식을 사용한 이메일 유효성 검사
     * 
     * @param email 검증할 이메일
     * @return 유효한 이메일
     */
    private String validateEmail(String email) {
        String cleanEmail = validateNotBlank(email, "이메일");
        
        // Pattern.matcher().matches(): 정규표현식 전체 매칭 확인
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다: " + cleanEmail);
        }
        return cleanEmail;
    }

    /**
     * 전화번호 형식 검증
     * 
     * 한국 휴대폰 번호 형식 (010-XXXX-XXXX) 확인
     * 
     * @param phone 검증할 전화번호
     * @return 유효한 전화번호
     */
    private String validatePhone(String phone) {
        String cleanPhone = validateNotBlank(phone, "전화번호");
        
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("전화번호는 010-XXXX-XXXX 형식이어야 합니다: " + cleanPhone);
        }
        return cleanPhone;
    }

    /**
     * 멤버십 기간 유효성 검증
     * 
     * 비즈니스 규칙: 멤버십 종료일은 현재 날짜 이후여야 함
     * 
     * @param period 검증할 멤버십 기간
     */
    private void validateMembershipPeriod(DateRange period) {
        LocalDate today = LocalDate.now();
        if (period.getEndDate().isBefore(today)) {
            throw new IllegalArgumentException("멤버십 종료일은 현재 날짜 이후여야 합니다");
        }
    }

    /**
     * 플랜 활성 상태 검증
     * 
     * 비활성화된 플랜은 할당할 수 없음
     * 
     * @param plan 검증할 플랜
     */
    private void validatePlanIsActive(MembershipPlan plan) {
        if (!plan.isActive()) {
            throw new IllegalArgumentException("비활성화된 플랜은 할당할 수 없습니다: " + plan.getName());
        }
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. 엔티티 설계 원칙

#### 식별자 기반 동일성
```java
// 같은 ID면 다른 속성이 달라도 같은 회원
Member member1 = Member.create("M001", "홍길동", "hong@test.com", "010-1234-5678");
Member member2 = Member.create("M001", "홍길동", "newemail@test.com", "010-9999-9999");

boolean same = member1.equals(member2); // true (같은 memberId)
```

#### 상태 변경 메서드
- 상태 전환 규칙을 코드로 명시
- 비즈니스 불변식 보장
- 상태 변경 이력 추적

### 2. 비즈니스 로직 캡슐화

#### 권한 확인 로직
```java
// 복잡한 권한 확인 로직을 메서드로 캡슐화
boolean canReserve = member.canReserve(ResourceType.GYM, LocalDate.now().plusDays(3));

// 내부에서는 여러 조건을 체크
// 1. 멤버십 활성 상태
// 2. 예약 날짜가 멤버십 기간 내
// 3. 플랜에서 해당 리소스 권한 보유
```

### 3. 상태 관리 패턴

#### 상태 전환 + 메타데이터 기록
```java
member.suspend("부적절한 행위로 인한 정지");
// 1. 상태 변경: ACTIVE → SUSPENDED  
// 2. 정지 사유 기록
// 3. 변경 시점 기록
```

### 4. 검증 전략

#### 정규표현식 활용
- 이메일: RFC 5322 기반 간소화된 패턴
- 전화번호: 한국 휴대폰 형식 (010-XXXX-XXXX)

#### 다단계 검증
1. Null/공백 체크
2. 형식 검증 (정규표현식)
3. 비즈니스 규칙 검증

### 5. 가변성 관리

#### 불변 필드 vs 가변 필드 구분
```java
// 불변: 생성 후 절대 변경되지 않음
private final String memberId;
private final String name;

// 가변: 비즈니스 로직에 의해 변경됨  
private MemberStatus status;
private MembershipPlan currentPlan;
```

### 6. 정보 은닉과 캡슐화

#### private 검증 메서드
- 중복 코드 제거
- 검증 로직 일관성 보장
- 내부 구현 세부사항 숨김

#### 계산된 속성
```java
// 저장하지 않고 계산으로 제공
public int getRemainingMembershipDays() {
    // 현재 날짜 기준으로 동적 계산
}
```

이러한 설계로 Member는 단순한 데이터 컨테이너가 아닌, 회원과 관련된 비즈니스 규칙을 캡슐화한 풍부한 도메인 객체가 되었습니다.