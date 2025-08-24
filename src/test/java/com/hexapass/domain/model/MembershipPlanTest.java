package com.hexapass.domain.model;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MembershipPlan 엔티티 테스트")
class MembershipPlanTest {

    @DisplayName("MembershipPlan 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 정보로 기본 멤버십 플랜을 생성할 수 있다")
        void createBasicMembershipPlan() {
            // Given
            String planId = "BASIC_MONTHLY";
            String name = "기본 월간권";
            PlanType type = PlanType.MONTHLY;
            Money price = Money.won(50000);
            int durationDays = 30;
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM, ResourceType.STUDY_ROOM);

            // When
            MembershipPlan plan = MembershipPlan.create(planId, name, type, price, durationDays, resourceTypes);

            // Then
            assertThat(plan.getPlanId()).isEqualTo(planId);
            assertThat(plan.getName()).isEqualTo(name);
            assertThat(plan.getType()).isEqualTo(type);
            assertThat(plan.getPrice()).isEqualTo(price);
            assertThat(plan.getDurationDays()).isEqualTo(durationDays);
            assertThat(plan.getAllowedResourceTypes()).containsExactlyInAnyOrderElementsOf(resourceTypes);
            assertThat(plan.getMaxSimultaneousReservations()).isEqualTo(3); // 기본값
            assertThat(plan.getMaxAdvanceReservationDays()).isEqualTo(30); // 기본값
            assertThat(plan.getDiscountRate()).isEqualTo(BigDecimal.ZERO); // 기본값
            assertThat(plan.isActive()).isTrue();
        }

        @Test
        @DisplayName("상세 옵션이 포함된 멘버십 플랜을 생성할 수 있다")
        void createDetailedMembershipPlan() {
            // Given
            String planId = "PREMIUM_YEARLY";
            String name = "프리미엄 연간권";
            PlanType type = PlanType.YEARLY;
            Money price = Money.won(500000);
            int durationDays = 365;
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM, ResourceType.POOL, ResourceType.SAUNA);
            int maxReservations = 5;
            int maxAdvanceDays = 60;
            BigDecimal discountRate = new BigDecimal("0.15");

            // When
            MembershipPlan plan = MembershipPlan.createWithOptions(
                    planId, name, type, price, durationDays, resourceTypes,
                    maxReservations, maxAdvanceDays, discountRate
            );

            // Then
            assertThat(plan.getMaxSimultaneousReservations()).isEqualTo(maxReservations);
            assertThat(plan.getMaxAdvanceReservationDays()).isEqualTo(maxAdvanceDays);
            assertThat(plan.getDiscountRate()).isEqualTo(discountRate);
        }

        @Test
        @DisplayName("기본 플랜들을 팩토리 메서드로 생성할 수 있다")
        void createPresetPlans() {
            // When
            MembershipPlan basic = MembershipPlan.basicMonthly();
            MembershipPlan premium = MembershipPlan.premiumMonthly();
            MembershipPlan vip = MembershipPlan.vipYearly();

            // Then
            assertThat(basic.getPlanId()).isEqualTo("BASIC_MONTHLY");
            assertThat(basic.getType()).isEqualTo(PlanType.MONTHLY);

            assertThat(premium.getPlanId()).isEqualTo("PREMIUM_MONTHLY");
            assertThat(premium.getDiscountRate()).isEqualTo(new BigDecimal("0.1"));

            assertThat(vip.getPlanId()).isEqualTo("VIP_YEARLY");
            assertThat(vip.getType()).isEqualTo(PlanType.YEARLY);
            assertThat(vip.getDiscountRate()).isEqualTo(new BigDecimal("0.2"));
        }

        @ParameterizedTest
        @DisplayName("필수 필드가 null이거나 빈 값이면 예외가 발생한다")
        @MethodSource("provideInvalidStringFields")
        void createWithInvalidStringFields(String planId, String name) {
            // Given
            PlanType type = PlanType.MONTHLY;
            Money price = Money.won(50000);
            int durationDays = 30;
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM);

            // When & Then
            assertThatThrownBy(() -> MembershipPlan.create(planId, name, type, price, durationDays, resourceTypes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null이거나 빈 값일 수 없습니다");
        }

        static Stream<Arguments> provideInvalidStringFields() {
            return Stream.of(
                    Arguments.of(null, "Valid Name"),
                    Arguments.of("", "Valid Name"),
                    Arguments.of("  ", "Valid Name"),
                    Arguments.of("Valid ID", null),
                    Arguments.of("Valid ID", ""),
                    Arguments.of("Valid ID", "  ")
            );
        }

        @Test
        @DisplayName("null 객체 필드로 생성하면 예외가 발생한다")
        void createWithNullObjectFields() {
            // Given
            String planId = "PLAN_001";
            String name = "Test Plan";

            // When & Then
            assertThatThrownBy(() -> MembershipPlan.create(planId, name, null, Money.won(50000), 30, Set.of(ResourceType.GYM)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("플랜 타입은 null일 수 없습니다");

            assertThatThrownBy(() -> MembershipPlan.create(planId, name, PlanType.MONTHLY, null, 30, Set.of(ResourceType.GYM)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가격은 null일 수 없습니다");

            assertThatThrownBy(() -> MembershipPlan.create(planId, name, PlanType.MONTHLY, Money.won(50000), 30, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이용 가능 리소스는 null이거나 빈 집합일 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("0 이하의 값으로 생성하면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -30})
        void createWithInvalidPositiveValues(int invalidValue) {
            // Given
            String planId = "PLAN_001";
            String name = "Test Plan";
            PlanType type = PlanType.MONTHLY;
            Money price = Money.won(50000);
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM);

            // When & Then
            assertThatThrownBy(() -> MembershipPlan.create(planId, name, type, price, invalidValue, resourceTypes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이용 기간은 0보다 커야 합니다");
        }

        @ParameterizedTest
        @DisplayName("유효하지 않은 할인율로 생성하면 예외가 발생한다")
        @ValueSource(strings = {"-0.1", "1.1", "2.0"})
        void createWithInvalidDiscountRate(String invalidRate) {
            // Given
            BigDecimal discountRate = new BigDecimal(invalidRate);

            // When & Then
            assertThatThrownBy(() ->
                    MembershipPlan.createWithOptions(
                            "PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                            Set.of(ResourceType.GYM), 3, 30, discountRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인율은 0.0 이상 1.0 이하여야 합니다");
        }

        @Test
        @DisplayName("플랜 타입과 일치하지 않는 기간으로 생성하면 예외가 발생한다")
        void createWithInconsistentTypeAndDuration() {
            // When & Then
            assertThatThrownBy(() ->
                    MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000),
                            400, Set.of(ResourceType.GYM))) // 월간권인데 400일
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("플랜 타입 MONTHLY에 적합하지 않은 기간입니다");
        }
    }

    @DisplayName("MembershipPlan 권한 확인 테스트")
    @Nested
    class PrivilegeTest {

        private MembershipPlan plan;

        @Test
        @DisplayName("허용된 리소스 타입에 대한 권한이 있다")
        void hasPrivilegeForAllowedResourceType() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM, ResourceType.POOL));

            // When & Then
            assertThat(plan.hasPrivilege(ResourceType.GYM)).isTrue();
            assertThat(plan.hasPrivilege(ResourceType.POOL)).isTrue();
        }

        @Test
        @DisplayName("허용되지 않은 리소스 타입에 대한 권한이 없다")
        void noPrivilegeForDisallowedResourceType() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then
            assertThat(plan.hasPrivilege(ResourceType.POOL)).isFalse();
            assertThat(plan.hasPrivilege(ResourceType.SAUNA)).isFalse();
        }

        @Test
        @DisplayName("여러 리소스 타입에 대한 권한을 동시에 확인할 수 있다")
        void hasPrivilegesForMultipleResourceTypes() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM, ResourceType.POOL, ResourceType.SAUNA));

            // When & Then
            assertThat(plan.hasPrivileges(Set.of(ResourceType.GYM, ResourceType.POOL))).isTrue();
            assertThat(plan.hasPrivileges(Set.of(ResourceType.GYM, ResourceType.STUDY_ROOM))).isFalse(); // STUDY_ROOM 없음
        }

        @Test
        @DisplayName("비활성화된 플랜은 권한이 없다")
        void inactivePlanHasNoPrivilege() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));
            plan.deactivate();

            // When & Then
            assertThat(plan.hasPrivilege(ResourceType.GYM)).isFalse();
        }
    }

    @DisplayName("MembershipPlan 예약 제한 확인 테스트")
    @Nested
    class ReservationLimitTest {

        private MembershipPlan plan;

        @Test
        @DisplayName("현재 예약 수가 최대 허용 수보다 적으면 예약할 수 있다")
        void canReserveWhenUnderLimit() {
            // Given
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 5, 30, BigDecimal.ZERO);

            // When & Then
            assertThat(plan.canReserve(0)).isTrue(); // 0개 예약 중, 최대 5개
            assertThat(plan.canReserve(3)).isTrue(); // 3개 예약 중, 최대 5개
            assertThat(plan.canReserve(4)).isTrue(); // 4개 예약 중, 최대 5개
        }

        @Test
        @DisplayName("현재 예약 수가 최대 허용 수와 같거나 크면 예약할 수 없다")
        void cannotReserveWhenAtOrOverLimit() {
            // Given
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 30, BigDecimal.ZERO);

            // When & Then
            assertThat(plan.canReserve(3)).isFalse(); // 3개 예약 중, 최대 3개
            assertThat(plan.canReserve(5)).isFalse(); // 5개 예약 중, 최대 3개
        }

        @Test
        @DisplayName("선예약 가능 일수 내에서는 예약할 수 있다")
        void canReserveInAdvanceWithinLimit() {
            // Given
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 45, BigDecimal.ZERO);

            // When & Then
            assertThat(plan.canReserveInAdvance(30)).isTrue(); // 30일 후 예약, 최대 45일
            assertThat(plan.canReserveInAdvance(45)).isTrue(); // 45일 후 예약, 최대 45일
        }

        @Test
        @DisplayName("선예약 가능 일수를 초과하면 예약할 수 없다")
        void cannotReserveInAdvanceBeyondLimit() {
            // Given
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 30, BigDecimal.ZERO);

            // When & Then
            assertThat(plan.canReserveInAdvance(45)).isFalse(); // 45일 후 예약, 최대 30일
            assertThat(plan.canReserveInAdvance(60)).isFalse(); // 60일 후 예약, 최대 30일
        }

        @Test
        @DisplayName("비활성화된 플랜은 예약할 수 없다")
        void inactivePlanCannotReserve() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));
            plan.deactivate();

            // When & Then
            assertThat(plan.canReserve(0)).isFalse();
            assertThat(plan.canReserveInAdvance(10)).isFalse();
        }
    }

    @DisplayName("MembershipPlan 가격 계산 테스트")
    @Nested
    class PricingTest {

        private MembershipPlan plan;

        @Test
        @DisplayName("일할 계산된 가격을 올바르게 계산한다")
        void calculateProRatedPriceCorrectly() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(30000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then
            assertThat(plan.calculateProRatedPrice(30)).isEqualTo(Money.won(30000)); // 전체 기간
            assertThat(plan.calculateProRatedPrice(15)).isEqualTo(Money.won(15000)); // 절반 기간
            assertThat(plan.calculateProRatedPrice(10)).isEqualTo(Money.won(10000)); // 1/3 기간
            assertThat(plan.calculateProRatedPrice(0)).isEqualTo(Money.zeroWon()); // 0일
        }

        @Test
        @DisplayName("남은 일수가 전체 기간보다 크면 전체 가격을 반환한다")
        void returnFullPriceWhenRemainingDaysExceedDuration() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(30000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then
            assertThat(plan.calculateProRatedPrice(45)).isEqualTo(Money.won(30000));
            assertThat(plan.calculateProRatedPrice(100)).isEqualTo(Money.won(30000));
        }

        @Test
        @DisplayName("할인이 적용된 가격을 계산한다")
        void calculateDiscountedPrice() {
            // Given - 20% 할인
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 30, new BigDecimal("0.2"));

            // When & Then
            assertThat(plan.getDiscountedPrice()).isEqualTo(Money.won(40000)); // 20% 할인 적용
            assertThat(plan.getDiscountAmount()).isEqualTo(Money.won(10000)); // 할인 금액
        }

        @Test
        @DisplayName("할인율이 0인 경우 원가를 반환한다")
        void returnOriginalPriceWhenNoDiscount() {
            // Given
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then
            assertThat(plan.getDiscountedPrice()).isEqualTo(Money.won(50000));
            assertThat(plan.getDiscountAmount()).isEqualTo(Money.zeroWon());
        }

        @Test
        @DisplayName("업그레이드 비용을 올바르게 계산한다")
        void calculateUpgradeCost() {
            // Given
            MembershipPlan basicPlan = MembershipPlan.create("BASIC", "Basic Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan premiumPlan = MembershipPlan.create("PREMIUM", "Premium Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM, ResourceType.POOL));

            // When
            Money upgradeCost = basicPlan.calculateUpgradeCost(premiumPlan, 15); // 15일 남음

            // Then
            Money basicRefund = Money.won(15000); // 15일 치 환불
            Money premiumCost = Money.won(50000); // 프리미엄 전체 가격
            Money expectedCost = premiumCost.subtract(basicRefund);
            assertThat(upgradeCost).isEqualTo(expectedCost);
        }

        @Test
        @DisplayName("null 대상 플랜으로 업그레이드 비용 계산 시 예외가 발생한다")
        void calculateUpgradeCostWithNullTargetPlan() {
            // Given
            plan = MembershipPlan.basicMonthly();

            // When & Then
            assertThatThrownBy(() -> plan.calculateUpgradeCost(null, 15))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("대상 플랜은 null일 수 없습니다");
        }
    }

    @DisplayName("MembershipPlan 상태 관리 테스트")
    @Nested
    class StatusManagementTest {

        private MembershipPlan plan;

        @Test
        @DisplayName("플랜을 비활성화할 수 있다")
        void deactivatePlan() {
            // Given
            plan = MembershipPlan.basicMonthly();
            assertThat(plan.isActive()).isTrue();

            // When
            plan.deactivate();

            // Then
            assertThat(plan.isActive()).isFalse();
        }

        @Test
        @DisplayName("플랜을 다시 활성화할 수 있다")
        void reactivatePlan() {
            // Given
            plan = MembershipPlan.basicMonthly();
            plan.deactivate();
            assertThat(plan.isActive()).isFalse();

            // When
            plan.activate();

            // Then
            assertThat(plan.isActive()).isTrue();
        }
    }

    @DisplayName("MembershipPlan 비교 테스트")
    @Nested
    class ComparisonTest {

        @Test
        @DisplayName("가격을 기준으로 플랜을 비교할 수 있다")
        void comparePlansByPrice() {
            // Given
            MembershipPlan cheapPlan = MembershipPlan.create("CHEAP", "Cheap Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan expensivePlan = MembershipPlan.create("EXPENSIVE", "Expensive Plan", PlanType.MONTHLY,
                    Money.won(80000), 30, Set.of(ResourceType.GYM, ResourceType.POOL));

            // When & Then
            assertThat(expensivePlan.compareTo(cheapPlan)).isGreaterThan(0);
            assertThat(cheapPlan.compareTo(expensivePlan)).isLessThan(0);
            assertThat(cheapPlan.compareTo(cheapPlan)).isEqualTo(0);
        }

        @Test
        @DisplayName("상위 플랜인지 확인할 수 있다")
        void checkIfHigherTier() {
            // Given
            MembershipPlan basic = MembershipPlan.basicMonthly();
            MembershipPlan premium = MembershipPlan.premiumMonthly();
            MembershipPlan vip = MembershipPlan.vipYearly();

            // When & Then
            assertThat(premium.isHigherTierThan(basic)).isTrue();
            assertThat(vip.isHigherTierThan(premium)).isTrue();
            assertThat(basic.isHigherTierThan(premium)).isFalse();
        }

        @Test
        @DisplayName("null과 비교하면 1을 반환한다")
        void compareWithNull() {
            // Given
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // When & Then
            assertThat(plan.compareTo(null)).isEqualTo(1);
            assertThat(plan.isHigherTierThan(null)).isTrue();
        }
    }

    @DisplayName("MembershipPlan 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 planId를 가진 플랜들은 동등하다")
        void equalityWithSamePlanId() {
            // Given
            MembershipPlan plan1 = MembershipPlan.create("PLAN_001", "Plan A", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan plan2 = MembershipPlan.create("PLAN_001", "Plan B", PlanType.YEARLY,
                    Money.won(50000), 365, Set.of(ResourceType.POOL)); // 다른 속성들

            // When & Then
            assertThat(plan1).isEqualTo(plan2);
            assertThat(plan1.hashCode()).isEqualTo(plan2.hashCode());
        }

        @Test
        @DisplayName("다른 planId를 가진 플랜들은 동등하지 않다")
        void inequalityWithDifferentPlanId() {
            // Given
            MembershipPlan plan1 = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan plan2 = MembershipPlan.create("PLAN_002", "Test Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM)); // planId만 다름

            // When & Then
            assertThat(plan1).isNotEqualTo(plan2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // When & Then
            assertThat(plan).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 객체와는 동등하지 않다")
        void inequalityWithDifferentType() {
            // Given
            MembershipPlan plan = MembershipPlan.basicMonthly();
            String notPlan = "BASIC_MONTHLY";

            // When & Then
            assertThat(plan).isNotEqualTo(notPlan);
        }
    }

    @DisplayName("MembershipPlan toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 정보를 포함한다")
        void toStringContainsCorrectInfo() {
            // Given
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // When
            String result = plan.toString();

            // Then
            assertThat(result).contains("BASIC_MONTHLY");
            assertThat(result).contains("기본 월간권");
            assertThat(result).contains("MONTHLY");
            assertThat(result).contains("50000 KRW");
            assertThat(result).contains("true"); // active status
        }

        @Test
        @DisplayName("getSummary가 플랜 정보를 요약해서 보여준다")
        void summaryContainsPlanInfo() {
            // Given
            MembershipPlan plan = MembershipPlan.premiumMonthly();

            // When
            String summary = plan.getSummary();

            // Then
            assertThat(summary).contains("프리미엄 월간권");
            assertThat(summary).contains("월간권");
            assertThat(summary).contains("30일");
            assertThat(summary).contains("동시예약");
        }
    }

    @DisplayName("MembershipPlan 불변성 테스트")
    @Nested
    class ImmutabilityTest {

        @Test
        @DisplayName("getAllowedResourceTypes는 불변 집합을 반환한다")
        void getAllowedResourceTypesReturnsImmutableSet() {
            // Given
            MembershipPlan plan = MembershipPlan.basicMonthly();
            Set<ResourceType> resourceTypes = plan.getAllowedResourceTypes();

            // When & Then
            assertThatThrownBy(() -> resourceTypes.add(ResourceType.POOL))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("생성 시점 이후로는 변경되지 않는다")
        void planCreationTimeIsImmutable() {
            // Given
            MembershipPlan plan = MembershipPlan.basicMonthly();
            var creationTime = plan.getCreatedAt();

            // When - 시간이 지나도
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // ignore
            }

            // Then - 생성 시간은 변경되지 않음
            assertThat(plan.getCreatedAt()).isEqualTo(creationTime);
        }
    }
}