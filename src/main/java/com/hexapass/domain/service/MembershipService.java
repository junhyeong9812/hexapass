package com.hexapass.domain.service;

import com.hexapass.domain.common.DateRange;
import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

import java.time.LocalDate;

/**
 * 멤버십 도메인 서비스
 * 멤버십 관련 복잡한 비즈니스 로직 처리
 */
public class MembershipService {

    /**
     * 멤버십 플랜 변경
     */
    public MembershipChangeResult changePlan(
            Member member,
            MembershipPlan newPlan,
            DiscountPolicy discountPolicy) {

        // 1. 변경 가능성 검증
        if (member.getStatus() != com.hexapass.domain.type.MemberStatus.ACTIVE) {
            return MembershipChangeResult.failed("활성 상태의 회원만 플랜을 변경할 수 있습니다");
        }

        MembershipPlan currentPlan = member.getCurrentPlan();
        if (currentPlan != null && currentPlan.equals(newPlan)) {
            return MembershipChangeResult.failed("동일한 플랜으로는 변경할 수 없습니다");
        }

        if (!newPlan.isActive()) {
            return MembershipChangeResult.failed("비활성화된 플랜으로는 변경할 수 없습니다");
        }

        // 2. 요금 계산
        Money planDifference = calculatePlanDifference(currentPlan, newPlan, member, discountPolicy);

        // 3. 멤버 플랜 변경
        try {
            // 새로운 멤버십 기간 설정 (기존 기간 유지하거나 새로 설정)
            DateRange newPeriod = calculateNewMembershipPeriod(member, newPlan);
            member.changePlan(newPlan);
            if (newPeriod != null) {
                member.assignMembership(newPlan, newPeriod);
            }

            return MembershipChangeResult.success(member, planDifference);
        } catch (Exception e) {
            return MembershipChangeResult.failed("플랜 변경 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 멤버십 갱신
     */
    public MembershipRenewalResult renewMembership(
            Member member,
            MembershipPlan plan,
            DiscountPolicy discountPolicy) {

        if (member.getStatus() != com.hexapass.domain.type.MemberStatus.ACTIVE) {
            return MembershipRenewalResult.failed("활성 상태의 회원만 멤버십을 갱신할 수 있습니다");
        }

        if (!plan.isActive()) {
            return MembershipRenewalResult.failed("비활성화된 플랜으로는 갱신할 수 없습니다");
        }

        // 갱신 요금 계산 (할인 적용)
        DiscountContext discountContext = DiscountContext.builder()
                .member(member)
                .plan(plan)
                .baseDate(LocalDate.now())
                .build();

        Money renewalPrice = discountPolicy.applyDiscount(plan.getPrice(), discountContext);

        try {
            // 멤버십 기간 연장
            DateRange currentPeriod = member.getMembershipPeriod();
            DateRange newPeriod;

            if (currentPeriod != null && !member.isMembershipExpired()) {
                // 현재 멤버십이 유효한 경우 연장
                LocalDate extendFrom = currentPeriod.getEndDate().plusDays(1);
                newPeriod = DateRange.of(extendFrom, extendFrom.plusDays(plan.getDurationDays() - 1));
            } else {
                // 만료된 경우 새로 시작
                LocalDate today = LocalDate.now();
                newPeriod = DateRange.of(today, today.plusDays(plan.getDurationDays() - 1));
            }

            member.assignMembership(plan, newPeriod);

            return MembershipRenewalResult.success(member, renewalPrice);
        } catch (Exception e) {
            return MembershipRenewalResult.failed("멤버십 갱신 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 멤버십 일시 정지
     */
    public MembershipSuspensionResult suspendMembership(
            Member member,
            int suspensionDays,
            String reason) {

        if (member.getStatus() != com.hexapass.domain.type.MemberStatus.ACTIVE) {
            return MembershipSuspensionResult.failed("활성 상태의 회원만 일시정지할 수 있습니다");
        }

        if (suspensionDays <= 0) {
            return MembershipSuspensionResult.failed("일시정지 일수는 1일 이상이어야 합니다");
        }

        if (reason == null || reason.trim().isEmpty()) {
            return MembershipSuspensionResult.failed("일시정지 사유를 입력해주세요");
        }

        try {
            member.suspend(reason);

            // 멤버십 기간 연장 (일시정지 기간만큼)
            if (member.getMembershipPeriod() != null) {
                member.extendMembership(suspensionDays);
            }

            return MembershipSuspensionResult.success(member, suspensionDays);
        } catch (Exception e) {
            return MembershipSuspensionResult.failed("멤버십 일시정지 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 멤버십 재개
     */
    public MembershipReactivationResult reactivateMembership(Member member) {
        if (member.getStatus() != com.hexapass.domain.type.MemberStatus.SUSPENDED) {
            return MembershipReactivationResult.failed("일시정지 상태의 회원만 재개할 수 있습니다");
        }

        try {
            member.activate();
            return MembershipReactivationResult.success(member);
        } catch (Exception e) {
            return MembershipReactivationResult.failed("멤버십 재개 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 멤버십 만료 경고 확인
     */
    public boolean checkExpiryWarning(Member member, int warningDays) {
        return member.isMembershipExpiryWarning(warningDays);
    }

    /**
     * 멤버십 자동 갱신 처리
     */
    public MembershipRenewalResult autoRenewMembership(
            Member member,
            DiscountPolicy discountPolicy) {

        if (!member.hasMembershipActive() || !member.isMembershipExpired()) {
            return MembershipRenewalResult.failed("자동 갱신 조건을 만족하지 않습니다");
        }

        MembershipPlan currentPlan = member.getCurrentPlan();
        if (currentPlan == null) {
            return MembershipRenewalResult.failed("현재 플랜이 없어 자동 갱신할 수 없습니다");
        }

        return renewMembership(member, currentPlan, discountPolicy);
    }

    private Money calculatePlanDifference(MembershipPlan current, MembershipPlan newPlan,
                                          Member member, DiscountPolicy discountPolicy) {
        DiscountContext context = DiscountContext.builder()
                .member(member)
                .plan(newPlan)
                .baseDate(LocalDate.now())
                .build();

        Money newPlanPrice = discountPolicy.applyDiscount(newPlan.getPrice(), context);

        if (current == null) {
            return newPlanPrice;
        }

        // 현재 플랜의 남은 기간에 대한 일할 계산
        int remainingDays = member.getRemainingMembershipDays();
        Money currentPlanRefund = current.calculateProRatedPrice(remainingDays);

        return newPlanPrice.subtract(currentPlanRefund);
    }

    private DateRange calculateNewMembershipPeriod(Member member, MembershipPlan newPlan) {
        DateRange currentPeriod = member.getMembershipPeriod();

        if (currentPeriod == null || member.isMembershipExpired()) {
            // 새로 시작
            LocalDate today = LocalDate.now();
            return DateRange.of(today, today.plusDays(newPlan.getDurationDays() - 1));
        } else {
            // 현재 기간 유지하면서 플랜만 변경
            return currentPeriod;
        }
    }

    /**
     * 플랜 변경 결과
     */
    public static class MembershipChangeResult {
        private final boolean success;
        private final Member updatedMember;
        private final Money priceDifference;
        private final String errorMessage;

        private MembershipChangeResult(boolean success, Member updatedMember,
                                       Money priceDifference, String errorMessage) {
            this.success = success;
            this.updatedMember = updatedMember;
            this.priceDifference = priceDifference;
            this.errorMessage = errorMessage;
        }

        public static MembershipChangeResult success(Member updatedMember, Money priceDifference) {
            return new MembershipChangeResult(true, updatedMember, priceDifference, null);
        }

        public static MembershipChangeResult failed(String errorMessage) {
            return new MembershipChangeResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Member getUpdatedMember() { return updatedMember; }
        public Money getPriceDifference() { return priceDifference; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 멤버십 갱신 결과
     */
    public static class MembershipRenewalResult {
        private final boolean success;
        private final Member renewedMember;
        private final Money renewalPrice;
        private final String errorMessage;

        private MembershipRenewalResult(boolean success, Member renewedMember,
                                        Money renewalPrice, String errorMessage) {
            this.success = success;
            this.renewedMember = renewedMember;
            this.renewalPrice = renewalPrice;
            this.errorMessage = errorMessage;
        }

        public static MembershipRenewalResult success(Member renewedMember, Money renewalPrice) {
            return new MembershipRenewalResult(true, renewedMember, renewalPrice, null);
        }

        public static MembershipRenewalResult failed(String errorMessage) {
            return new MembershipRenewalResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Member getRenewedMember() { return renewedMember; }
        public Money getRenewalPrice() { return renewalPrice; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 멤버십 일시정지 결과
     */
    public static class MembershipSuspensionResult {
        private final boolean success;
        private final Member suspendedMember;
        private final int suspensionDays;
        private final String errorMessage;

        private MembershipSuspensionResult(boolean success, Member suspendedMember,
                                           int suspensionDays, String errorMessage) {
            this.success = success;
            this.suspendedMember = suspendedMember;
            this.suspensionDays = suspensionDays;
            this.errorMessage = errorMessage;
        }

        public static MembershipSuspensionResult success(Member suspendedMember, int suspensionDays) {
            return new MembershipSuspensionResult(true, suspendedMember, suspensionDays, null);
        }

        public static MembershipSuspensionResult failed(String errorMessage) {
            return new MembershipSuspensionResult(false, null, 0, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Member getSuspendedMember() { return suspendedMember; }
        public int getSuspensionDays() { return suspensionDays; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 멤버십 재개 결과
     */
    public static class MembershipReactivationResult {
        private final boolean success;
        private final Member reactivatedMember;
        private final String errorMessage;

        private MembershipReactivationResult(boolean success, Member reactivatedMember, String errorMessage) {
            this.success = success;
            this.reactivatedMember = reactivatedMember;
            this.errorMessage = errorMessage;
        }

        public static MembershipReactivationResult success(Member reactivatedMember) {
            return new MembershipReactivationResult(true, reactivatedMember, null);
        }

        public static MembershipReactivationResult failed(String errorMessage) {
            return new MembershipReactivationResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Member getReactivatedMember() { return reactivatedMember; }
        public String getErrorMessage() { return errorMessage; }
    }
}