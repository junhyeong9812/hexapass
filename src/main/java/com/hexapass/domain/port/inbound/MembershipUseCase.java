package com.hexapass.domain.port.inbound;

import com.hexapass.domain.common.DateRange;
import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.type.MemberStatus;
import com.hexapass.domain.type.PlanType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 멤버십 관련 Use Case 인터페이스 (Inbound Port)
 * 회원 관리, 멤버십 플랜 관리, 갱신 등의 비즈니스 로직을 정의
 */
public interface MembershipUseCase {

    /**
     * 회원 가입
     */
    MembershipResult registerMember(RegisterMemberCommand command);

    /**
     * 멤버십 플랜 가입
     */
    MembershipResult subscribeToPlan(SubscribeToPlanCommand command);

    /**
     * 멤버십 플랜 변경
     */
    MembershipResult changePlan(ChangePlanCommand command);

    /**
     * 멤버십 갱신
     */
    MembershipResult renewMembership(RenewMembershipCommand command);

    /**
     * 멤버십 일시 정지
     */
    MembershipResult suspendMembership(SuspendMembershipCommand command);

    /**
     * 멤버십 재개
     */
    MembershipResult resumeMembership(ResumeMembershipCommand command);

    /**
     * 회원 탈퇴
     */
    MembershipResult withdrawMember(WithdrawMemberCommand command);

    /**
     * 회원 정보 조회
     */
    MemberDetails getMemberDetails(String memberId);

    /**
     * 회원 정보 수정
     */
    MembershipResult updateMemberInfo(UpdateMemberInfoCommand command);

    /**
     * 멤버십 플랜 목록 조회
     */
    List<PlanSummary> getAvailablePlans();

    /**
     * 특정 회원에게 추천하는 플랜 조회
     */
    List<PlanRecommendation> getRecommendedPlans(String memberId);

    /**
     * 플랜 업그레이드/다운그레이드 시뮬레이션
     */
    PlanChangeSimulation simulatePlanChange(String memberId, String targetPlanId);

    /**
     * 멤버십 만료 예정 회원 조회
     */
    List<ExpiringMembership> getExpiringMemberships(int withinDays);

    /**
     * 자동 갱신 처리
     */
    AutoRenewalResult processAutoRenewal(String memberId);

    /**
     * 회원 통계 조회
     */
    MembershipStatistics getMembershipStatistics(
            LocalDate from,
            LocalDate to,
            String planId
    );

    /**
     * 멤버십 사용 이력 조회
     */
    List<MembershipUsageHistory> getMembershipUsageHistory(
            String memberId,
            LocalDate from,
            LocalDate to
    );

    // =========================
    // Command Objects
    // =========================

    /**
     * 회원 가입 명령
     */
    class RegisterMemberCommand {
        private final String name;
        private final String email;
        private final String phone;
        private final String planId;
        private final String couponCode;
        private final String referralCode;

        public RegisterMemberCommand(String name, String email, String phone,
                                     String planId, String couponCode, String referralCode) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.planId = planId;
            this.couponCode = couponCode;
            this.referralCode = referralCode;
        }

        // Getters
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getPlanId() { return planId; }
        public String getCouponCode() { return couponCode; }
        public String getReferralCode() { return referralCode; }
    }

    /**
     * 멤버십 플랜 가입 명령
     */
    class SubscribeToPlanCommand {
        private final String memberId;
        private final String planId;
        private final String couponCode;
        private final String paymentMethodId;
        private final boolean autoRenewal;

        public SubscribeToPlanCommand(String memberId, String planId, String couponCode,
                                      String paymentMethodId, boolean autoRenewal) {
            this.memberId = memberId;
            this.planId = planId;
            this.couponCode = couponCode;
            this.paymentMethodId = paymentMethodId;
            this.autoRenewal = autoRenewal;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getPlanId() { return planId; }
        public String getCouponCode() { return couponCode; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public boolean isAutoRenewal() { return autoRenewal; }
    }

    /**
     * 플랜 변경 명령
     */
    class ChangePlanCommand {
        private final String memberId;
        private final String newPlanId;
        private final String reason;
        private final boolean prorated;
        private final String couponCode;

        public ChangePlanCommand(String memberId, String newPlanId, String reason,
                                 boolean prorated, String couponCode) {
            this.memberId = memberId;
            this.newPlanId = newPlanId;
            this.reason = reason;
            this.prorated = prorated;
            this.couponCode = couponCode;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getNewPlanId() { return newPlanId; }
        public String getReason() { return reason; }
        public boolean isProrated() { return prorated; }
        public String getCouponCode() { return couponCode; }
    }

    /**
     * 멤버십 갱신 명령
     */
    class RenewMembershipCommand {
        private final String memberId;
        private final String planId;
        private final String couponCode;
        private final String paymentMethodId;
        private final boolean autoRenewal;

        public RenewMembershipCommand(String memberId, String planId, String couponCode,
                                      String paymentMethodId, boolean autoRenewal) {
            this.memberId = memberId;
            this.planId = planId;
            this.couponCode = couponCode;
            this.paymentMethodId = paymentMethodId;
            this.autoRenewal = autoRenewal;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getPlanId() { return planId; }
        public String getCouponCode() { return couponCode; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public boolean isAutoRenewal() { return autoRenewal; }
    }

    /**
     * 멤버십 일시정지 명령
     */
    class SuspendMembershipCommand {
        private final String memberId;
        private final String reason;
        private final int suspensionDays;
        private final String suspendedBy;

        public SuspendMembershipCommand(String memberId, String reason,
                                        int suspensionDays, String suspendedBy) {
            this.memberId = memberId;
            this.reason = reason;
            this.suspensionDays = suspensionDays;
            this.suspendedBy = suspendedBy;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getReason() { return reason; }
        public int getSuspensionDays() { return suspensionDays; }
        public String getSuspendedBy() { return suspendedBy; }
    }

    /**
     * 멤버십 재개 명령
     */
    class ResumeMembershipCommand {
        private final String memberId;
        private final String resumedBy;
        private final String notes;

        public ResumeMembershipCommand(String memberId, String resumedBy, String notes) {
            this.memberId = memberId;
            this.resumedBy = resumedBy;
            this.notes = notes;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getResumedBy() { return resumedBy; }
        public String getNotes() { return notes; }
    }

    /**
     * 회원 탈퇴 명령
     */
    class WithdrawMemberCommand {
        private final String memberId;
        private final String reason;
        private final boolean refundRequested;
        private final String withdrawnBy;

        public WithdrawMemberCommand(String memberId, String reason,
                                     boolean refundRequested, String withdrawnBy) {
            this.memberId = memberId;
            this.reason = reason;
            this.refundRequested = refundRequested;
            this.withdrawnBy = withdrawnBy;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getReason() { return reason; }
        public boolean isRefundRequested() { return refundRequested; }
        public String getWithdrawnBy() { return withdrawnBy; }
    }

    /**
     * 회원 정보 수정 명령
     */
    class UpdateMemberInfoCommand {
        private final String memberId;
        private final String newName;
        private final String newEmail;
        private final String newPhone;
        private final String updatedBy;

        public UpdateMemberInfoCommand(String memberId, String newName, String newEmail,
                                       String newPhone, String updatedBy) {
            this.memberId = memberId;
            this.newName = newName;
            this.newEmail = newEmail;
            this.newPhone = newPhone;
            this.updatedBy = updatedBy;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getNewName() { return newName; }
        public String getNewEmail() { return newEmail; }
        public String getNewPhone() { return newPhone; }
        public String getUpdatedBy() { return updatedBy; }
    }

    // =========================
    // Result Objects
    // =========================

    /**
     * 멤버십 결과
     */
    class MembershipResult {
        private final boolean success;
        private final Member member;
        private final Money totalCost;
        private final Money discountAmount;
        private final String paymentId;
        private final String errorMessage;
        private final List<String> warnings;

        public MembershipResult(boolean success, Member member, Money totalCost,
                                Money discountAmount, String paymentId,
                                String errorMessage, List<String> warnings) {
            this.success = success;
            this.member = member;
            this.totalCost = totalCost;
            this.discountAmount = discountAmount;
            this.paymentId = paymentId;
            this.errorMessage = errorMessage;
            this.warnings = warnings != null ? warnings : List.of();
        }

        public static MembershipResult success(Member member, Money totalCost,
                                               Money discountAmount, String paymentId) {
            return new MembershipResult(true, member, totalCost, discountAmount,
                    paymentId, null, null);
        }

        public static MembershipResult failure(String errorMessage) {
            return new MembershipResult(false, null, null, null, null, errorMessage, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Member getMember() { return member; }
        public Money getTotalCost() { return totalCost; }
        public Money getDiscountAmount() { return discountAmount; }
        public String getPaymentId() { return paymentId; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getWarnings() { return warnings; }
    }

    /**
     * 자동 갱신 결과
     */
    class AutoRenewalResult {
        private final boolean processed;
        private final boolean success;
        private final Member member;
        private final String paymentId;
        private final LocalDate nextExpiryDate;
        private final String errorMessage;

        public AutoRenewalResult(boolean processed, boolean success, Member member,
                                 String paymentId, LocalDate nextExpiryDate, String errorMessage) {
            this.processed = processed;
            this.success = success;
            this.member = member;
            this.paymentId = paymentId;
            this.nextExpiryDate = nextExpiryDate;
            this.errorMessage = errorMessage;
        }

        public static AutoRenewalResult notProcessed(String reason) {
            return new AutoRenewalResult(false, false, null, null, null, reason);
        }

        public static AutoRenewalResult success(Member member, String paymentId,
                                                LocalDate nextExpiryDate) {
            return new AutoRenewalResult(true, true, member, paymentId, nextExpiryDate, null);
        }

        public static AutoRenewalResult failure(String errorMessage) {
            return new AutoRenewalResult(true, false, null, null, null, errorMessage);
        }

        // Getters
        public boolean isProcessed() { return processed; }
        public boolean isSuccess() { return success; }
        public Member getMember() { return member; }
        public String getPaymentId() { return paymentId; }
        public LocalDate getNextExpiryDate() { return nextExpiryDate; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 회원 상세 정보
     */
    class MemberDetails {
        private final Member member;
        private final MembershipPlan currentPlan;
        private final DateRange membershipPeriod;
        private final int remainingDays;
        private final Money totalSpent;
        private final int totalReservations;
        private final LocalDateTime lastActivity;
        private final List<String> activeWarnings;

        public MemberDetails(Member member, MembershipPlan currentPlan, DateRange membershipPeriod,
                             int remainingDays, Money totalSpent, int totalReservations,
                             LocalDateTime lastActivity, List<String> activeWarnings) {
            this.member = member;
            this.currentPlan = currentPlan;
            this.membershipPeriod = membershipPeriod;
            this.remainingDays = remainingDays;
            this.totalSpent = totalSpent;
            this.totalReservations = totalReservations;
            this.lastActivity = lastActivity;
            this.activeWarnings = activeWarnings != null ? activeWarnings : List.of();
        }

        // Getters
        public Member getMember() { return member; }
        public MembershipPlan getCurrentPlan() { return currentPlan; }
        public DateRange getMembershipPeriod() { return membershipPeriod; }
        public int getRemainingDays() { return remainingDays; }
        public Money getTotalSpent() { return totalSpent; }
        public int getTotalReservations() { return totalReservations; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public List<String> getActiveWarnings() { return activeWarnings; }
    }

    /**
     * 플랜 요약 정보
     */
    class PlanSummary {
        private final MembershipPlan plan;
        private final Money discountedPrice;
        private final boolean isPopular;
        private final boolean isRecommended;
        private final String description;

        public PlanSummary(MembershipPlan plan, Money discountedPrice,
                           boolean isPopular, boolean isRecommended, String description) {
            this.plan = plan;
            this.discountedPrice = discountedPrice;
            this.isPopular = isPopular;
            this.isRecommended = isRecommended;
            this.description = description;
        }

        // Getters
        public MembershipPlan getPlan() { return plan; }
        public Money getDiscountedPrice() { return discountedPrice; }
        public boolean isPopular() { return isPopular; }
        public boolean isRecommended() { return isRecommended; }
        public String getDescription() { return description; }
    }

    /**
     * 플랜 추천 정보
     */
    class PlanRecommendation {
        private final MembershipPlan plan;
        private final String reasonForRecommendation;
        private final Money potentialSavings;
        private final List<String> benefits;
        private final int priorityScore;

        public PlanRecommendation(MembershipPlan plan, String reasonForRecommendation,
                                  Money potentialSavings, List<String> benefits, int priorityScore) {
            this.plan = plan;
            this.reasonForRecommendation = reasonForRecommendation;
            this.potentialSavings = potentialSavings;
            this.benefits = benefits != null ? benefits : List.of();
            this.priorityScore = priorityScore;
        }

        // Getters
        public MembershipPlan getPlan() { return plan; }
        public String getReasonForRecommendation() { return reasonForRecommendation; }
        public Money getPotentialSavings() { return potentialSavings; }
        public List<String> getBenefits() { return benefits; }
        public int getPriorityScore() { return priorityScore; }
    }

    /**
     * 플랜 변경 시뮬레이션
     */
    class PlanChangeSimulation {
        private final MembershipPlan currentPlan;
        private final MembershipPlan targetPlan;
        private final Money costDifference;
        private final Money refundAmount;
        private final Money additionalCost;
        private final DateRange newMembershipPeriod;
        private final List<String> benefitChanges;
        private final boolean recommended;

        public PlanChangeSimulation(MembershipPlan currentPlan, MembershipPlan targetPlan,
                                    Money costDifference, Money refundAmount, Money additionalCost,
                                    DateRange newMembershipPeriod, List<String> benefitChanges,
                                    boolean recommended) {
            this.currentPlan = currentPlan;
            this.targetPlan = targetPlan;
            this.costDifference = costDifference;
            this.refundAmount = refundAmount;
            this.additionalCost = additionalCost;
            this.newMembershipPeriod = newMembershipPeriod;
            this.benefitChanges = benefitChanges != null ? benefitChanges : List.of();
            this.recommended = recommended;
        }

        // Getters
        public MembershipPlan getCurrentPlan() { return currentPlan; }
        public MembershipPlan getTargetPlan() { return targetPlan; }
        public Money getCostDifference() { return costDifference; }
        public Money getRefundAmount() { return refundAmount; }
        public Money getAdditionalCost() { return additionalCost; }
        public DateRange getNewMembershipPeriod() { return newMembershipPeriod; }
        public List<String> getBenefitChanges() { return benefitChanges; }
        public boolean isRecommended() { return recommended; }
    }

    /**
     * 만료 예정 멤버십
     */
    class ExpiringMembership {
        private final String memberId;
        private final String memberName;
        private final String planName;
        private final LocalDate expiryDate;
        private final int daysRemaining;
        private final boolean autoRenewalEnabled;
        private final String contactEmail;

        public ExpiringMembership(String memberId, String memberName, String planName,
                                  LocalDate expiryDate, int daysRemaining, boolean autoRenewalEnabled,
                                  String contactEmail) {
            this.memberId = memberId;
            this.memberName = memberName;
            this.planName = planName;
            this.expiryDate = expiryDate;
            this.daysRemaining = daysRemaining;
            this.autoRenewalEnabled = autoRenewalEnabled;
            this.contactEmail = contactEmail;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getMemberName() { return memberName; }
        public String getPlanName() { return planName; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public int getDaysRemaining() { return daysRemaining; }
        public boolean isAutoRenewalEnabled() { return autoRenewalEnabled; }
        public String getContactEmail() { return contactEmail; }
    }

    /**
     * 멤버십 통계
     */
    class MembershipStatistics {
        private final int totalMembers;
        private final int activeMembers;
        private final int suspendedMembers;
        private final int newMembers;
        private final int renewedMembers;
        private final int withdrawnMembers;
        private final Money totalRevenue;
        private final Money averageRevenuePerMember;
        private final double renewalRate;
        private final double churnRate;

        public MembershipStatistics(int totalMembers, int activeMembers, int suspendedMembers,
                                    int newMembers, int renewedMembers, int withdrawnMembers,
                                    Money totalRevenue, Money averageRevenuePerMember) {
            this.totalMembers = totalMembers;
            this.activeMembers = activeMembers;
            this.suspendedMembers = suspendedMembers;
            this.newMembers = newMembers;
            this.renewedMembers = renewedMembers;
            this.withdrawnMembers = withdrawnMembers;
            this.totalRevenue = totalRevenue;
            this.averageRevenuePerMember = averageRevenuePerMember;

            // 갱신율과 이탈율 계산
            int eligibleForRenewal = totalMembers - newMembers;
            this.renewalRate = eligibleForRenewal > 0 ? (double) renewedMembers / eligibleForRenewal : 0.0;
            this.churnRate = totalMembers > 0 ? (double) withdrawnMembers / totalMembers : 0.0;
        }

        // Getters
        public int getTotalMembers() { return totalMembers; }
        public int getActiveMembers() { return activeMembers; }
        public int getSuspendedMembers() { return suspendedMembers; }
        public int getNewMembers() { return newMembers; }
        public int getRenewedMembers() { return renewedMembers; }
        public int getWithdrawnMembers() { return withdrawnMembers; }
        public Money getTotalRevenue() { return totalRevenue; }
        public Money getAverageRevenuePerMember() { return averageRevenuePerMember; }
        public double getRenewalRate() { return renewalRate; }
        public double getChurnRate() { return churnRate; }
    }

    /**
     * 멤버십 사용 이력
     */
    class MembershipUsageHistory {
        private final LocalDateTime timestamp;
        private final String action;
        private final String planName;
        private final Money amount;
        private final String description;
        private final String performedBy;

        public MembershipUsageHistory(LocalDateTime timestamp, String action, String planName,
                                      Money amount, String description, String performedBy) {
            this.timestamp = timestamp;
            this.action = action;
            this.planName = planName;
            this.amount = amount;
            this.description = description;
            this.performedBy = performedBy;
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getPlanName() { return planName; }
        public Money getAmount() { return amount; }
        public String getDescription() { return description; }
        public String getPerformedBy() { return performedBy; }
    }
}