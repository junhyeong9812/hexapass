package com.hexapass.domain.policy;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.type.ResourceType;

import java.time.LocalDate;
import java.util.Set;

/**
 * 할인 적용에 필요한 컨텍스트 정보
 */
public class DiscountContext {

    private final Member member;
    private final MembershipPlan membershipPlan;
    private final LocalDate purchaseDate;
    private final String couponCode;
    private final Set<ResourceType> resourceTypes;

    private DiscountContext(Member member, MembershipPlan membershipPlan,
                            LocalDate purchaseDate, String couponCode,
                            Set<ResourceType> resourceTypes) {
        this.member = member;
        this.membershipPlan = membershipPlan;
        this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDate.now();
        this.couponCode = couponCode;
        this.resourceTypes = resourceTypes != null ? Set.copyOf(resourceTypes) : Set.of();
    }

    public static DiscountContext of(Member member, MembershipPlan membershipPlan) {
        return new DiscountContext(member, membershipPlan, LocalDate.now(), null, null);
    }

    public static DiscountContext withCoupon(Member member, MembershipPlan membershipPlan,
                                             String couponCode) {
        return new DiscountContext(member, membershipPlan, LocalDate.now(), couponCode, null);
    }

    public static DiscountContext complete(Member member, MembershipPlan membershipPlan,
                                           LocalDate purchaseDate, String couponCode,
                                           Set<ResourceType> resourceTypes) {
        return new DiscountContext(member, membershipPlan, purchaseDate, couponCode, resourceTypes);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public Member getMember() {
        return member;
    }

    public MembershipPlan getMembershipPlan() {
        return membershipPlan;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public Set<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public boolean hasCoupon() {
        return couponCode != null && !couponCode.trim().isEmpty();
    }
}