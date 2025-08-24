# MembershipPlanTest.md

## 클래스 개요
`MembershipPlanTest`는 멤버십 플랜을 다루는 `MembershipPlan` 도메인 객체의 기능을 검증하는 테스트 클래스입니다. 이 클래스는 멤버십 플랜의 생성, 권한 확인, 예약 제한, 가격 계산, 상태 관리 등의 핵심 비즈니스 로직을 테스트합니다.

## 왜 MembershipPlan 객체가 필요한가?
- **멤버십 체계 관리**: 기본, 프리미엄, VIP 등 다양한 멤버십 등급 체계 구현
- **권한 제어**: 플랜별로 이용 가능한 리소스와 서비스 제한
- **비즈니스 규칙 캡슐화**: 할인율, 예약 제한, 선예약 기간 등의 복잡한 정책 관리
- **가격 정책 통합**: 일할 계산, 업그레이드 비용 등 다양한 가격 계산 로직

```java
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

// MembershipPlan 도메인 엔티티의 모든 기능을 검증하는 테스트 클래스
@DisplayName("MembershipPlan 엔티티 테스트")
class MembershipPlanTest {

    @DisplayName("MembershipPlan 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 정보로 기본 멤버십 플랜을 생성할 수 있다")
        void createBasicMembershipPlan() {
            // Given: 멤버십 플랜 생성에 필요한 기본 정보들
            String planId = "BASIC_MONTHLY";
            String name = "기본 월간권";
            // PlanType enum: 플랜의 유형을 나타내는 열거형 (MONTHLY, YEARLY 등)
            PlanType type = PlanType.MONTHLY;
            // Money 도메인 객체: 정확한 금액 표현
            Money price = Money.won(50000);
            int durationDays = 30;
            // Set.of(): Java 9+의 불변 Set 생성 메서드
            // ResourceType enum: 이용 가능한 리소스 타입들 (GYM, STUDY_ROOM 등)
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM, ResourceType.STUDY_ROOM);

            // When: 기본 정보로 멤버십 플랜 생성
            // create(): 기본 설정값을 사용하는 정적 팩토리 메서드
            MembershipPlan plan = MembershipPlan.create(planId, name, type, price, durationDays, resourceTypes);

            // Then: 생성된 플랜의 속성들 검증
            assertThat(plan.getPlanId()).isEqualTo(planId);
            assertThat(plan.getName()).isEqualTo(name);
            assertThat(plan.getType()).isEqualTo(type);
            assertThat(plan.getPrice()).isEqualTo(price);
            assertThat(plan.getDurationDays()).isEqualTo(durationDays);
            // containsExactlyInAnyOrderElementsOf(): Set의 모든 요소를 순서 무관하게 검증
            assertThat(plan.getAllowedResourceTypes()).containsExactlyInAnyOrderElementsOf(resourceTypes);
            // 기본값들 확인 - 명시적으로 설정하지 않은 속성들은 기본값 사용
            assertThat(plan.getMaxSimultaneousReservations()).isEqualTo(3); // 기본값
            assertThat(plan.getMaxAdvanceReservationDays()).isEqualTo(30); // 기본값
            assertThat(plan.getDiscountRate()).isEqualTo(BigDecimal.ZERO); // 기본값
            assertThat(plan.isActive()).isTrue(); // 생성 시 활성 상태
        }

        @Test
        @DisplayName("상세 옵션이 포함된 멤버십 플랜을 생성할 수 있다")
        void createDetailedMembershipPlan() {
            // Given: 모든 옵션을 포함한 상세 정보
            String planId = "PREMIUM_YEARLY";
            String name = "프리미엄 연간권";
            PlanType type = PlanType.YEARLY;
            Money price = Money.won(500000);
            int durationDays = 365;
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM, ResourceType.POOL, ResourceType.SAUNA);
            int maxReservations = 5; // 동시 예약 최대 개수
            int maxAdvanceDays = 60; // 선예약 가능 일수
            // BigDecimal: 정확한 소수 계산을 위한 클래스 (15% 할인)
            BigDecimal discountRate = new BigDecimal("0.15");

            // When: 상세 옵션을 포함한 멤버십 플랜 생성
            // createWithOptions(): 모든 옵션을 설정할 수 있는 정적 팩토리 메서드
            MembershipPlan plan = MembershipPlan.createWithOptions(
                    planId, name, type, price, durationDays, resourceTypes,
                    maxReservations, maxAdvanceDays, discountRate
            );

            // Then: 상세 옵션들이 올바르게 설정되었는지 확인
            assertThat(plan.getMaxSimultaneousReservations()).isEqualTo(maxReservations);
            assertThat(plan.getMaxAdvanceReservationDays()).isEqualTo(maxAdvanceDays);
            assertThat(plan.getDiscountRate()).isEqualTo(discountRate);
        }

        @Test
        @DisplayName("기본 플랜들을 팩토리 메서드로 생성할 수 있다")
        void createPresetPlans() {
            // When: 미리 정의된 플랜들 생성
            // 도메인 특화 편의 메서드들 - 일반적인 플랜들을 쉽게 생성
            MembershipPlan basic = MembershipPlan.basicMonthly();
            MembershipPlan premium = MembershipPlan.premiumMonthly();
            MembershipPlan vip = MembershipPlan.vipYearly();

            // Then: 미리 정의된 플랜들의 특성 확인
            assertThat(basic.getPlanId()).isEqualTo("BASIC_MONTHLY");
            assertThat(basic.getType()).isEqualTo(PlanType.MONTHLY);

            assertThat(premium.getPlanId()).isEqualTo("PREMIUM_MONTHLY");
            // 프리미엄 플랜은 기본적으로 10% 할인 제공
            assertThat(premium.getDiscountRate()).isEqualTo(new BigDecimal("0.1"));

            assertThat(vip.getPlanId()).isEqualTo("VIP_YEARLY");
            assertThat(vip.getType()).isEqualTo(PlanType.YEARLY);
            // VIP 플랜은 20% 할인 제공
            assertThat(vip.getDiscountRate()).isEqualTo(new BigDecimal("0.2"));
        }

        @ParameterizedTest
        @DisplayName("필수 필드가 null이거나 빈 값이면 예외가 발생한다")
        @MethodSource("provideInvalidStringFields")
        void createWithInvalidStringFields(String planId, String name) {
            // Given: 유효한 다른 필드들
            PlanType type = PlanType.MONTHLY;
            Money price = Money.won(50000);
            int durationDays = 30;
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM);

            // When & Then: null이나 빈 문자열로 생성 시도 시 예외 발생
            // 비즈니스 규칙: 필수 문자열 필드들은 null이거나 공백일 수 없음
            assertThatThrownBy(() -> MembershipPlan.create(planId, name, type, price, durationDays, resourceTypes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null이거나 빈 값일 수 없습니다");
        }

        // 유효하지 않은 문자열 필드 조합을 제공하는 데이터 소스
        static Stream<Arguments> provideInvalidStringFields() {
            return Stream.of(
                    Arguments.of(null, "Valid Name"),        // planId가 null
                    Arguments.of("", "Valid Name"),          // planId가 빈 문자열
                    Arguments.of("  ", "Valid Name"),        // planId가 공백만 포함
                    Arguments.of("Valid ID", null),          // name이 null
                    Arguments.of("Valid ID", ""),            // name이 빈 문자열
                    Arguments.of("Valid ID", "  ")           // name이 공백만 포함
            );
        }

        @Test
        @DisplayName("null 객체 필드로 생성하면 예외가 발생한다")
        void createWithNullObjectFields() {
            // Given: 유효한 문자열 필드들
            String planId = "PLAN_001";
            String name = "Test Plan";

            // When & Then: 필수 객체 필드들이 null일 때 예외 발생
            // 각각의 null 케이스에 대한 명확한 에러 메시지 확인
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
            // Given: 유효한 다른 필드들
            String planId = "PLAN_001";
            String name = "Test Plan";
            PlanType type = PlanType.MONTHLY;
            Money price = Money.won(50000);
            Set<ResourceType> resourceTypes = Set.of(ResourceType.GYM);

            // When & Then: 0 이하의 기간으로 생성 시도 시 예외 발생
            // 비즈니스 규칙: 이용 기간은 최소 1일 이상이어야 함
            assertThatThrownBy(() -> MembershipPlan.create(planId, name, type, price, invalidValue, resourceTypes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이용 기간은 0보다 커야 합니다");
        }

        @ParameterizedTest
        @DisplayName("유효하지 않은 할인율로 생성하면 예외가 발생한다")
        @ValueSource(strings = {"-0.1", "1.1", "2.0"})
        void createWithInvalidDiscountRate(String invalidRate) {
            // Given: 유효하지 않은 할인율
            BigDecimal discountRate = new BigDecimal(invalidRate);

            // When & Then: 잘못된 범위의 할인율로 생성 시도
            // 비즈니스 규칙: 할인율은 0.0~1.0 사이의 값이어야 함 (0%~100%)
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
            // When & Then: 플랜 타입과 맞지 않는 기간으로 생성 시도
            // 비즈니스 규칙: MONTHLY는 1~60일, YEARLY는 300~400일 정도의 기간이 적합
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
            // Given: GYM과 POOL을 허용하는 플랜
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM, ResourceType.POOL));

            // When & Then: 허용된 리소스에 대한 권한 확인
            // hasPrivilege(): 특정 리소스 타입에 대한 이용 권한 확인
            assertThat(plan.hasPrivilege(ResourceType.GYM)).isTrue();
            assertThat(plan.hasPrivilege(ResourceType.POOL)).isTrue();
        }

        @Test
        @DisplayName("허용되지 않은 리소스 타입에 대한 권한이 없다")
        void noPrivilegeForDisallowedResourceType() {
            // Given: GYM만 허용하는 기본 플랜
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then: 허용되지 않은 리소스에 대한 권한 없음 확인
            assertThat(plan.hasPrivilege(ResourceType.POOL)).isFalse();
            assertThat(plan.hasPrivilege(ResourceType.SAUNA)).isFalse();
        }

        @Test
        @DisplayName("여러 리소스 타입에 대한 권한을 동시에 확인할 수 있다")
        void hasPrivilegesForMultipleResourceTypes() {
            // Given: 다양한 리소스를 허용하는 플랜
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM, ResourceType.POOL, ResourceType.SAUNA));

            // When & Then: 여러 리소스에 대한 권한을 한 번에 확인
            // hasPrivileges(): Set으로 여러 리소스 타입을 한 번에 확인
            assertThat(plan.hasPrivileges(Set.of(ResourceType.GYM, ResourceType.POOL))).isTrue();
            assertThat(plan.hasPrivileges(Set.of(ResourceType.GYM, ResourceType.STUDY_ROOM))).isFalse(); // STUDY_ROOM 없음
        }

        @Test
        @DisplayName("비활성화된 플랜은 권한이 없다")
        void inactivePlanHasNoPrivilege() {
            // Given: 활성화된 플랜을 비활성화
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));
            // deactivate(): 플랜을 비활성화하는 메서드
            plan.deactivate();

            // When & Then: 비활성화된 플랜은 어떤 리소스도 이용 불가
            // 비즈니스 규칙: 비활성화된 플랜은 모든 권한 박탈
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
            // Given: 최대 5개 동시 예약이 가능한 플랜
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 5, 30, BigDecimal.ZERO);

            // When & Then: 제한 내에서의 예약 가능성 확인
            // canReserve(): 현재 예약 수를 기준으로 추가 예약 가능 여부 확인
            assertThat(plan.canReserve(0)).isTrue(); // 0개 예약 중, 최대 5개
            assertThat(plan.canReserve(3)).isTrue(); // 3개 예약 중, 최대 5개
            assertThat(plan.canReserve(4)).isTrue(); // 4개 예약 중, 최대 5개
        }

        @Test
        @DisplayName("현재 예약 수가 최대 허용 수와 같거나 크면 예약할 수 없다")
        void cannotReserveWhenAtOrOverLimit() {
            // Given: 최대 3개 동시 예약이 가능한 플랜
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 30, BigDecimal.ZERO);

            // When & Then: 제한에 도달하거나 초과한 경우 예약 불가
            assertThat(plan.canReserve(3)).isFalse(); // 3개 예약 중, 최대 3개
            assertThat(plan.canReserve(5)).isFalse(); // 5개 예약 중, 최대 3개
        }

        @Test
        @DisplayName("선예약 가능 일수 내에서는 예약할 수 있다")
        void canReserveInAdvanceWithinLimit() {
            // Given: 45일 선예약이 가능한 플랜
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 45, BigDecimal.ZERO);

            // When & Then: 선예약 제한 내에서의 예약 가능성 확인
            // canReserveInAdvance(): 몇 일 후까지 선예약이 가능한지 확인
            assertThat(plan.canReserveInAdvance(30)).isTrue(); // 30일 후 예약, 최대 45일
            assertThat(plan.canReserveInAdvance(45)).isTrue(); // 45일 후 예약, 최대 45일
        }

        @Test
        @DisplayName("선예약 가능 일수를 초과하면 예약할 수 없다")
        void cannotReserveInAdvanceBeyondLimit() {
            // Given: 30일 선예약이 가능한 플랜
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 30, BigDecimal.ZERO);

            // When & Then: 선예약 제한을 초과한 경우 예약 불가
            assertThat(plan.canReserveInAdvance(45)).isFalse(); // 45일 후 예약, 최대 30일
            assertThat(plan.canReserveInAdvance(60)).isFalse(); // 60일 후 예약, 최대 30일
        }

        @Test
        @DisplayName("비활성화된 플랜은 예약할 수 없다")
        void inactivePlanCannotReserve() {
            // Given: 비활성화된 플랜
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));
            plan.deactivate();

            // When & Then: 비활성화된 플랜은 모든 예약 불가
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
            // Given: 30일 30,000원 플랜 (1일당 1,000원)
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(30000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then: 다양한 잔여 일수에 대한 일할 계산 확인
            // calculateProRatedPrice(): 남은 일수에 비례하여 가격 계산
            assertThat(plan.calculateProRatedPrice(30)).isEqualTo(Money.won(30000)); // 전체 기간
            assertThat(plan.calculateProRatedPrice(15)).isEqualTo(Money.won(15000)); // 절반 기간
            assertThat(plan.calculateProRatedPrice(10)).isEqualTo(Money.won(10000)); // 1/3 기간
            assertThat(plan.calculateProRatedPrice(0)).isEqualTo(Money.zeroWon()); // 0일
        }

        @Test
        @DisplayName("남은 일수가 전체 기간보다 크면 전체 가격을 반환한다")
        void returnFullPriceWhenRemainingDaysExceedDuration() {
            // Given: 30일 30,000원 플랜
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(30000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then: 잔여 일수가 전체 기간을 초과하는 경우 전체 가격 반환
            // 비즈니스 규칙: 남은 기간이 플랜 기간보다 길어도 전체 가격만 청구
            assertThat(plan.calculateProRatedPrice(45)).isEqualTo(Money.won(30000));
            assertThat(plan.calculateProRatedPrice(100)).isEqualTo(Money.won(30000));
        }

        @Test
        @DisplayName("할인이 적용된 가격을 계산한다")
        void calculateDiscountedPrice() {
            // Given: 20% 할인이 적용된 플랜
            plan = MembershipPlan.createWithOptions("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM), 3, 30, new BigDecimal("0.2"));

            // When & Then: 할인 적용된 가격 계산 확인
            // getDiscountedPrice(): 할인이 적용된 최종 가격
            assertThat(plan.getDiscountedPrice()).isEqualTo(Money.won(40000)); // 20% 할인 적용
            // getDiscountAmount(): 할인 금액
            assertThat(plan.getDiscountAmount()).isEqualTo(Money.won(10000)); // 할인 금액
        }

        @Test
        @DisplayName("할인율이 0인 경우 원가를 반환한다")
        void returnOriginalPriceWhenNoDiscount() {
            // Given: 할인이 없는 플랜
            plan = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY, Money.won(50000), 30,
                    Set.of(ResourceType.GYM));

            // When & Then: 할인이 없는 경우 원가 그대로 반환
            assertThat(plan.getDiscountedPrice()).isEqualTo(Money.won(50000));
            assertThat(plan.getDiscountAmount()).isEqualTo(Money.zeroWon());
        }

        @Test
        @DisplayName("업그레이드 비용을 올바르게 계산한다")
        void calculateUpgradeCost() {
            // Given: 기본 플랜과 프리미엄 플랜
            MembershipPlan basicPlan = MembershipPlan.create("BASIC", "Basic Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan premiumPlan = MembershipPlan.create("PREMIUM", "Premium Plan", PlanType.MONTHLY,
                    Money.won(50000), 30, Set.of(ResourceType.GYM, ResourceType.POOL));

            // When: 15일 남은 시점에서 업그레이드 비용 계산
            // calculateUpgradeCost(): 현재 플랜을 다른 플랜으로 업그레이드할 때의 비용
            Money upgradeCost = basicPlan.calculateUpgradeCost(premiumPlan, 15); // 15일 남음

            // Then: 업그레이드 비용 계산 로직 확인
            Money basicRefund = Money.won(15000); // 15일 치 환불 (30000 * 15/30)
            Money premiumCost = Money.won(50000); // 프리미엄 전체 가격
            Money expectedCost = premiumCost.subtract(basicRefund); // 프리미엄 - 기본플랜환불
            assertThat(upgradeCost).isEqualTo(expectedCost);
        }

        @Test
        @DisplayName("null 대상 플랜으로 업그레이드 비용 계산 시 예외가 발생한다")
        void calculateUpgradeCostWithNullTargetPlan() {
            // Given: 기본 플랜
            plan = MembershipPlan.basicMonthly();

            // When & Then: null 대상 플랜으로 업그레이드 시도
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
            // Given: 활성화된 플랜
            plan = MembershipPlan.basicMonthly();
            assertThat(plan.isActive()).isTrue();

            // When: 플랜 비활성화
            plan.deactivate();

            // Then: 비활성화 상태 확인
            assertThat(plan.isActive()).isFalse();
        }

        @Test
        @DisplayName("플랜을 다시 활성화할 수 있다")
        void reactivatePlan() {
            // Given: 비활성화된 플랜
            plan = MembershipPlan.basicMonthly();
            plan.deactivate();
            assertThat(plan.isActive()).isFalse();

            // When: 플랜 재활성화
            // activate(): 비활성화된 플랜을 다시 활성화
            plan.activate();

            // Then: 활성화 상태 확인
            assertThat(plan.isActive()).isTrue();
        }
    }

    @DisplayName("MembershipPlan 비교 테스트")
    @Nested
    class ComparisonTest {

        @Test
        @DisplayName("가격을 기준으로 플랜을 비교할 수 있다")
        void comparePlansByPrice() {
            // Given: 서로 다른 가격의 플랜들
            MembershipPlan cheapPlan = MembershipPlan.create("CHEAP", "Cheap Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan expensivePlan = MembershipPlan.create("EXPENSIVE", "Expensive Plan", PlanType.MONTHLY,
                    Money.won(80000), 30, Set.of(ResourceType.GYM, ResourceType.POOL));

            // When & Then: 가격 기준 비교 (Comparable 인터페이스 구현)
            // compareTo(): Comparable 인터페이스의 메서드로 가격 기준 비교
            assertThat(expensivePlan.compareTo(cheapPlan)).isGreaterThan(0); // 비싼 플랜이 큰 값
            assertThat(cheapPlan.compareTo(expensivePlan)).isLessThan(0); // 싼 플랜이 작은 값
            assertThat(cheapPlan.compareTo(cheapPlan)).isEqualTo(0); // 같은 플랜은 0
        }

        @Test
        @DisplayName("상위 플랜인지 확인할 수 있다")
        void checkIfHigherTier() {
            // Given: 서로 다른 등급의 플랜들
            MembershipPlan basic = MembershipPlan.basicMonthly();
            MembershipPlan premium = MembershipPlan.premiumMonthly();
            MembershipPlan vip = MembershipPlan.vipYearly();

            // When & Then: 플랜 등급 비교
            // isHigherTierThan(): 다른 플랜보다 상위 등급인지 확인 (가격 기준)
            assertThat(premium.isHigherTierThan(basic)).isTrue();
            assertThat(vip.isHigherTierThan(premium)).isTrue();
            assertThat(basic.isHigherTierThan(premium)).isFalse();
        }

        @Test
        @DisplayName("null과 비교하면 1을 반환한다")
        void compareWithNull() {
            // Given: 플랜 객체
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // When & Then: null과 비교 (null 안전성)
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
            // Given: 같은 planId를 가진 서로 다른 속성의 플랜들
            MembershipPlan plan1 = MembershipPlan.create("PLAN_001", "Plan A", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan plan2 = MembershipPlan.create("PLAN_001", "Plan B", PlanType.YEARLY,
                    Money.won(50000), 365, Set.of(ResourceType.POOL)); // 다른 속성들

            // When & Then: planId만 같으면 동등 (엔티티의 특성)
            // equals(): planId를 기준으로 한 동등성 비교
            assertThat(plan1).isEqualTo(plan2);
            assertThat(plan1.hashCode()).isEqualTo(plan2.hashCode());
        }

        @Test
        @DisplayName("다른 planId를 가진 플랜들은 동등하지 않다")
        void inequalityWithDifferentPlanId() {
            // Given: 다른 planId를 가진 플랜들
            MembershipPlan plan1 = MembershipPlan.create("PLAN_001", "Test Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM));
            MembershipPlan plan2 = MembershipPlan.create("PLAN_002", "Test Plan", PlanType.MONTHLY,
                    Money.won(30000), 30, Set.of(ResourceType.GYM)); // planId만 다름

            // When & Then: planId가 다르면 비동등
            assertThat(plan1).isNotEqualTo(plan2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given: 플랜 객체
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // When & Then: null과 비교
            assertThat(plan).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 객체와는 동등하지 않다")
        void inequalityWithDifferentType() {
            // Given: MembershipPlan 객체와 문자열
            MembershipPlan plan = MembershipPlan.basicMonthly();
            String notPlan = "BASIC_MONTHLY";

            // When & Then: 타입이 다르면 비동등
            assertThat(plan).isNotEqualTo(notPlan);
        }
    }

    @DisplayName("MembershipPlan toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 정보를 포함한다")
        void toStringContainsCorrectInfo() {
            // Given: 기본 월간 플랜
            MembershipPlan plan = MembershipPlan.basicMonthly();

            // When: 문자열 변환
            String result = plan.toString();

            // Then: 주요 정보들이 포함되어 있는지 확인
            // toString(): 디버깅과 로깅을 위한 문자열 표현
            assertThat(result).contains("BASIC_MONTHLY");
            assertThat(result).contains("기본 월간권");
            assertThat(result).contains("MONTHLY");
            assertThat(result).contains("50000 KRW");
            assertThat(result).contains("true"); // active status
        }

        @Test
        @DisplayName("getSummary가 플랜 정보를 요약해서 보여준다")
        void summaryContainsPlanInfo() {
            // Given: 프리미엄 월간 플랜
            MembershipPlan plan = MembershipPlan.premiumMonthly();

            // When: 요약 정보 조회
            // getSummary(): 사용자에게 표시할 플랜 요약 정보
            String summary = plan.getSummary();

            // Then: 요약에 주요 정보들이 포함되어 있는지 확인
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
            // Given: 플랜 생성
            MembershipPlan plan = MembershipPlan.basicMonthly();
            Set<ResourceType> resourceTypes = plan.getAllowedResourceTypes();

            // When & Then: 반환된 Set이 수정 불가능한지 확인
            // 불변성 보장: 외부에서 내부 상태를 변경할 수 없도록 방어
            assertThatThrownBy(() -> resourceTypes.add(ResourceType.POOL))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("생성 시점 이후로는 변경되지 않는다")
        void planCreationTimeIsImmutable() {
            // Given: 플랜 생성
            MembershipPlan plan = MembershipPlan.basicMonthly();
            // getCreatedAt(): 플랜 생성 시간 반환
            var creationTime = plan.getCreatedAt();

            // When: 시간이 지나도
            try {
                Thread.sleep(10); // 10ms 대기 (실제 테스트에서는 Thread.sleep 지양)
            } catch (InterruptedException e) {
                // 예외 무시
            }

            // Then: 생성 시간은 변경되지 않음 (불변성)
            assertThat(plan.getCreatedAt()).isEqualTo(creationTime);
        }
    }
}
```

## 주요 설계 원칙 및 특징

### 1. **도메인 엔티티 (Domain Entity)**
- `planId`를 식별자로 하는 엔티티 객체
- 비즈니스 로직과 상태를 함께 캡슐화

### 2. **복잡한 비즈니스 규칙 관리**
- 리소스별 접근 권한 제어
- 예약 제한 정책 (동시 예약 수, 선예약 기간)
- 할인 정책과 가격 계산 로직

### 3. **상태 패턴 적용**
- 활성/비활성 상태에 따른 동작 변경
- 비활성화된 플랜은 모든 권한과 예약 기능 차단

### 4. **정적 팩토리 메서드**
- `basicMonthly()`, `premiumMonthly()` 등의 도메인 특화 편의 메서드
- 복잡한 생성 로직을 의미있는 메서드명으로 추상화

### 5. **불변성과 방어적 복사**
- 외부로 반환하는 컬렉션은 불변 컬렉션으로 방어
- 내부 상태 변경을 외부에서 차단

### 사용되는 주요 Java 클래스와 기능

#### **BigDecimal**
```java
// 정확한 소수점 계산이 필요한 할인율 처리
BigDecimal discountRate = new BigDecimal("0.15"); // 15% 할인
```

#### **Set.of() (Java 9+)**
```java
// 불변 Set 생성으로 컬렉션 안전성 보장
Set<ResourceType> resources = Set.of(ResourceType.GYM, ResourceType.POOL);
```

#### **Enum 활용**
```java
// PlanType, ResourceType 등 타입 안전성과 확장성 제공
PlanType type = PlanType.MONTHLY;
ResourceType resource = ResourceType.GYM;
```

#### **Comparable 인터페이스**
```java
// 플랜 간 비교와 정렬을 위한 자연 순서 정의
public int compareTo(MembershipPlan other) {
    return this.price.compareTo(other.price);
}
```

### 왜 이런 구조로 설계했는가?

1. **복잡한 멤버십 체계**: 다양한 플랜과 권한을 체계적으로 관리
2. **비즈니스 규칙 중앙화**: 플랜 관련 모든 정책을 한 곳에서 관리
3. **확장성**: 새로운 플랜 타입이나 권한을 쉽게 추가 가능
4. **일관성**: 모든 플랜이 동일한 인터페이스와 정책을 따름
5. **안전성**: 불변성과 검증을 통한 데이터 무결성 보장

### 테스트에서 확인하는 핵심 비즈니스 로직

1. **권한 관리**: 플랜별 리소스 접근 권한 제어
2. **예약 제한**: 동시 예약 수와 선예약 기간 제한
3. **가격 정책**: 할인, 일할 계산, 업그레이드 비용 계산
4. **상태 관리**: 활성/비활성 상태에 따른 기능 제어
5. **데이터 검증**: 생성 시점의 다양한 유효성 검사