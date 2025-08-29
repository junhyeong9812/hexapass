package com.hexapass.adapter.persistence.memory;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.port.outbound.MembershipPlanRepository;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 멤버십 플랜 저장소의 인메모리 구현체
 */
public class InMemoryMembershipPlanRepository implements MembershipPlanRepository {

    private final Map<String, MembershipPlan> plans = new ConcurrentHashMap<>();

    @Override
    public Optional<MembershipPlan> findById(String planId) {
        return Optional.ofNullable(plans.get(planId));
    }

    @Override
    public void save(MembershipPlan membershipPlan) {
        plans.put(membershipPlan.getPlanId(), membershipPlan);
    }

    @Override
    public void delete(String planId) {
        plans.remove(planId);
    }

    @Override
    public boolean existsById(String planId) {
        return plans.containsKey(planId);
    }

    @Override
    public boolean existsByName(String name) {
        return plans.values().stream()
                .anyMatch(plan -> name.equals(plan.getName()));
    }

    @Override
    public List<MembershipPlan> findAll() {
        return new ArrayList<>(plans.values());
    }

    @Override
    public List<MembershipPlan> findActiveProjects() {
        return plans.values().stream()
                .filter(MembershipPlan::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findInactiveProjects() {
        return plans.values().stream()
                .filter(plan -> !plan.isActive())
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findByType(PlanType planType) {
        return plans.values().stream()
                .filter(plan -> planType.equals(plan.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findActiveByType(PlanType planType) {
        return plans.values().stream()
                .filter(plan -> plan.isActive() && planType.equals(plan.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findByPriceRange(Money minPrice, Money maxPrice) {
        return plans.values().stream()
                .filter(plan -> {
                    Money price = plan.getDiscountedPrice();
                    return !price.isLessThan(minPrice) && !price.isGreaterThan(maxPrice);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findByDiscountRateGreaterThan(BigDecimal minDiscountRate) {
        return plans.values().stream()
                .filter(plan -> plan.getDiscountRate().compareTo(minDiscountRate) > 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findByResourceType(ResourceType resourceType) {
        return plans.values().stream()
                .filter(plan -> plan.hasPrivilege(resourceType))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findByResourceTypes(Set<ResourceType> resourceTypes) {
        return plans.values().stream()
                .filter(plan -> plan.hasPrivileges(resourceTypes))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findPlansWithAllResourceTypes() {
        Set<ResourceType> allTypes = Set.of(ResourceType.values());
        return findByResourceTypes(allTypes);
    }

    @Override
    public List<MembershipPlan> findByMinSimultaneousReservations(int minReservations) {
        return plans.values().stream()
                .filter(plan -> plan.getMaxSimultaneousReservations() >= minReservations)
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findByMinAdvanceReservationDays(int minDays) {
        return plans.values().stream()
                .filter(plan -> plan.getMaxAdvanceReservationDays() >= minDays)
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findBySimultaneousReservations(int simultaneousReservations) {
        return plans.values().stream()
                .filter(plan -> plan.getMaxSimultaneousReservations() == simultaneousReservations)
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findAllOrderByPriceAsc() {
        return plans.values().stream()
                .sorted((a, b) -> a.getDiscountedPrice().compareTo(b.getDiscountedPrice()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findAllOrderByPriceDesc() {
        return plans.values().stream()
                .sorted((a, b) -> b.getDiscountedPrice().compareTo(a.getDiscountedPrice()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findAllOrderByDiscountRateDesc() {
        return plans.values().stream()
                .sorted((a, b) -> b.getDiscountRate().compareTo(a.getDiscountRate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findAllOrderByCreatedAtDesc() {
        return plans.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findAffordablePlans(Money maxBudget) {
        return plans.values().stream()
                .filter(plan -> !plan.getDiscountedPrice().isGreaterThan(maxBudget))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findPlansByRequirements(Set<ResourceType> requiredResources,
                                                        int minSimultaneousReservations,
                                                        int minAdvanceReservationDays) {
        return plans.values().stream()
                .filter(plan -> plan.hasPrivileges(requiredResources))
                .filter(plan -> plan.getMaxSimultaneousReservations() >= minSimultaneousReservations)
                .filter(plan -> plan.getMaxAdvanceReservationDays() >= minAdvanceReservationDays)
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findUpgradePlans(String currentPlanId) {
        Optional<MembershipPlan> currentPlan = findById(currentPlanId);
        if (currentPlan.isEmpty()) {
            return new ArrayList<>();
        }

        Money currentPrice = currentPlan.get().getDiscountedPrice();
        return plans.values().stream()
                .filter(plan -> !plan.getPlanId().equals(currentPlanId))
                .filter(plan -> plan.getDiscountedPrice().isGreaterThan(currentPrice))
                .collect(Collectors.toList());
    }

    @Override
    public List<MembershipPlan> findDowngradePlans(String currentPlanId) {
        Optional<MembershipPlan> currentPlan = findById(currentPlanId);
        if (currentPlan.isEmpty()) {
            return new ArrayList<>();
        }

        Money currentPrice = currentPlan.get().getDiscountedPrice();
        return plans.values().stream()
                .filter(plan -> !plan.getPlanId().equals(currentPlanId))
                .filter(plan -> plan.getDiscountedPrice().isLessThan(currentPrice))
                .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return plans.size();
    }

    @Override
    public long countActive() {
        return plans.values().stream()
                .filter(MembershipPlan::isActive)
                .count();
    }

    @Override
    public long countByType(PlanType planType) {
        return plans.values().stream()
                .filter(plan -> planType.equals(plan.getType()))
                .count();
    }

    @Override
    public long countByPriceRange(Money minPrice, Money maxPrice) {
        return plans.values().stream()
                .filter(plan -> {
                    Money price = plan.getDiscountedPrice();
                    return !price.isLessThan(minPrice) && !price.isGreaterThan(maxPrice);
                })
                .count();
    }

    @Override
    public Money getAveragePrice() {
        if (plans.isEmpty()) {
            return Money.zero("KRW");
        }

        BigDecimal sum = plans.values().stream()
                .map(plan -> plan.getDiscountedPrice().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(BigDecimal.valueOf(plans.size()), 2, java.math.RoundingMode.HALF_UP);

        // 첫 번째 플랜의 통화 사용 (모든 플랜이 같은 통화라고 가정)
        String currency = plans.values().iterator().next().getDiscountedPrice().getCurrency();
        return Money.of(average, currency);
    }

    @Override
    public Optional<MembershipPlan> findMostExpensivePlan() {
        return plans.values().stream()
                .max((a, b) -> a.getDiscountedPrice().compareTo(b.getDiscountedPrice()));
    }

    @Override
    public Optional<MembershipPlan> findCheapestPlan() {
        return plans.values().stream()
                .min((a, b) -> a.getDiscountedPrice().compareTo(b.getDiscountedPrice()));
    }

    @Override
    public void saveAll(List<MembershipPlan> planList) {
        for (MembershipPlan plan : planList) {
            save(plan);
        }
    }

    @Override
    public List<MembershipPlan> findAll(int page, int size) {
        List<MembershipPlan> allPlans = findAll();
        int start = page * size;
        int end = Math.min(start + size, allPlans.size());

        if (start >= allPlans.size()) {
            return new ArrayList<>();
        }

        return allPlans.subList(start, end);
    }

    @Override
    public List<MembershipPlan> findActive(int page, int size) {
        List<MembershipPlan> activePlans = findActiveProjects();
        int start = page * size;
        int end = Math.min(start + size, activePlans.size());

        if (start >= activePlans.size()) {
            return new ArrayList<>();
        }

        return activePlans.subList(start, end);
    }

    // 개발/테스트용 유틸리티 메서드들
    public void clear() {
        plans.clear();
    }

    public int size() {
        return plans.size();
    }

    public boolean isEmpty() {
        return plans.isEmpty();
    }
}