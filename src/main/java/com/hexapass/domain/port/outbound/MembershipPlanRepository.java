package com.hexapass.domain.port.outbound;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 멤버십 플랜 저장소 포트 (Outbound Port)
 * 멤버십 플랜 관리와 조회를 위한 추상화
 */
public interface MembershipPlanRepository {

    // =========================
    // 기본 CRUD 연산
    // =========================

    /**
     * 플랜 ID로 멤버십 플랜 조회
     */
    Optional<MembershipPlan> findById(String planId);

    /**
     * 멤버십 플랜 저장 (생성/수정)
     */
    void save(MembershipPlan membershipPlan);

    /**
     * 멤버십 플랜 삭제
     */
    void delete(String planId);

    /**
     * 멤버십 플랜 존재 여부 확인
     */
    boolean existsById(String planId);

    /**
     * 플랜명으로 중복 확인
     */
    boolean existsByName(String name);

    // =========================
    // 조건별 조회
    // =========================

    /**
     * 모든 멤버십 플랜 조회
     */
    List<MembershipPlan> findAll();

    /**
     * 활성화된 멤버십 플랜 조회
     */
    List<MembershipPlan> findActiveProjects();

    /**
     * 비활성화된 멤버십 플랜 조회
     */
    List<MembershipPlan> findInactiveProjects();

    /**
     * 플랜 타입별 조회
     */
    List<MembershipPlan> findByType(PlanType planType);

    /**
     * 활성 플랜 중 타입별 조회
     */
    List<MembershipPlan> findActiveByType(PlanType planType);

    /**
     * 가격 범위로 조회
     */
    List<MembershipPlan> findByPriceRange(Money minPrice, Money maxPrice);

    /**
     * 특정 할인율 이상인 플랜 조회
     */
    List<MembershipPlan> findByDiscountRateGreaterThan(BigDecimal minDiscountRate);

    // =========================
    // 리소스 권한 기반 조회
    // =========================

    /**
     * 특정 리소스 타입 이용 가능한 플랜 조회
     */
    List<MembershipPlan> findByResourceType(ResourceType resourceType);

    /**
     * 특정 리소스 타입들을 모두 포함하는 플랜 조회
     */
    List<MembershipPlan> findByResourceTypes(Set<ResourceType> resourceTypes);

    /**
     * 모든 리소스 타입을 이용할 수 있는 플랜 조회 (VIP/프리미엄)
     */
    List<MembershipPlan> findPlansWithAllResourceTypes();

    // =========================
    // 예약 조건 기반 조회
    // =========================

    /**
     * 최소 동시 예약 수 이상인 플랜 조회
     */
    List<MembershipPlan> findByMinSimultaneousReservations(int minReservations);

    /**
     * 최소 선예약 일수 이상인 플랜 조회
     */
    List<MembershipPlan> findByMinAdvanceReservationDays(int minDays);

    /**
     * 특정 동시 예약 수를 허용하는 플랜 조회
     */
    List<MembershipPlan> findBySimultaneousReservations(int simultaneousReservations);

    // =========================
    // 정렬된 조회
    // =========================

    /**
     * 가격순 정렬 조회 (낮은 가격부터)
     */
    List<MembershipPlan> findAllOrderByPriceAsc();

    /**
     * 가격순 정렬 조회 (높은 가격부터)
     */
    List<MembershipPlan> findAllOrderByPriceDesc();

    /**
     * 할인율순 정렬 조회 (높은 할인율부터)
     */
    List<MembershipPlan> findAllOrderByDiscountRateDesc();

    /**
     * 생성일순 정렬 조회 (최신순)
     */
    List<MembershipPlan> findAllOrderByCreatedAtDesc();

    // =========================
    // 추천 및 필터링
    // =========================

    /**
     * 예산에 맞는 플랜 조회
     */
    List<MembershipPlan> findAffordablePlans(Money maxBudget);

    /**
     * 특정 요구사항에 맞는 플랜 조회
     */
    List<MembershipPlan> findPlansByRequirements(
            Set<ResourceType> requiredResources,
            int minSimultaneousReservations,
            int minAdvanceReservationDays
    );

    /**
     * 업그레이드 가능한 플랜 조회 (현재 플랜보다 상위)
     */
    List<MembershipPlan> findUpgradePlans(String currentPlanId);

    /**
     * 다운그레이드 가능한 플랜 조회 (현재 플랜보다 하위)
     */
    List<MembershipPlan> findDowngradePlans(String currentPlanId);

    // =========================
    // 통계 및 집계
    // =========================

    /**
     * 전체 플랜 수
     */
    long countAll();

    /**
     * 활성 플랜 수
     */
    long countActive();

    /**
     * 타입별 플랜 수
     */
    long countByType(PlanType planType);

    /**
     * 가격대별 플랜 수
     */
    long countByPriceRange(Money minPrice, Money maxPrice);

    /**
     * 평균 플랜 가격
     */
    Money getAveragePrice();

    /**
     * 최고/최저 가격 플랜 조회
     */
    Optional<MembershipPlan> findMostExpensivePlan();
    Optional<MembershipPlan> findCheapestPlan();

    // =========================
    // 배치 처리용 메서드들
    // =========================

    /**
     * 여러 플랜 일괄 저장
     */
    void saveAll(List<MembershipPlan> plans);

    /**
     * 페이징 처리된 플랜 목록 조회
     */
    List<MembershipPlan> findAll(int page, int size);

    /**
     * 활성 플랜 페이징 조회
     */
    List<MembershipPlan> findActive(int page, int size);
}