package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("정률 할인 정책 테스트")
class RateDiscountPolicyTest {

    private Member member;
    private MembershipPlan membershipPlan;
    private DiscountContext context;

    @BeforeEach
    void setUp() {
        member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
        membershipPlan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                Money.won(30000), 30, Set.of(ResourceType.GYM));
        context = DiscountContext.of(member, membershipPlan);
    }

    @DisplayName("정률 할인 정책 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 할인율로 정책을 생성할 수 있다")
        void createValidRateDiscountPolicy() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    new BigDecimal("0.10"), "10% 할인");

            assertThat(policy.getDiscountRate()).isEqualTo(new BigDecimal("0.10"));
            assertThat(policy.getDescription()).isEqualTo("10% 할인");
        }

        @Test
        @DisplayName("할인율이 음수이면 예외가 발생한다")
        void createWithNegativeRate() {
            assertThatThrownBy(() ->
                    RateDiscountPolicy.create(new BigDecimal("-0.10"), "음수 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인율은 0.0 이상 1.0 이하여야 합니다");
        }

        @Test
        @DisplayName("할인율이 100%를 초과하면 예외가 발생한다")
        void createWithOverRate() {
            assertThatThrownBy(() ->
                    RateDiscountPolicy.create(new BigDecimal("1.5"), "150% 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인율은 0.0 이상 1.0 이하여야 합니다");
        }

        @Test
        @DisplayName("설명이 빈 문자열이면 예외가 발생한다")
        void createWithEmptyDescription() {
            assertThatThrownBy(() ->
                    RateDiscountPolicy.create(new BigDecimal("0.10"), ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("설명은 null이거나 빈 값일 수 없습니다");
        }
    }

    @DisplayName("할인 적용 테스트")
    @Nested
    class DiscountApplicationTest {

        @Test
        @DisplayName("10% 할인이 정확히 적용된다")
        void apply10PercentDiscount() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    new BigDecimal("0.10"), "10% 할인");
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(9000));
        }

        @Test
        @DisplayName("20% 할인이 정확히 적용된다")
        void apply20PercentDiscount() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    new BigDecimal("0.20"), "20% 할인");
            Money originalPrice = Money.won(50000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(40000));
        }

        @Test
        @DisplayName("100% 할인 시 0원이 된다")
        void apply100PercentDiscount() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    new BigDecimal("1.0"), "100% 할인");
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(0));
        }

        @Test
        @DisplayName("0% 할인 시 원래 가격이 유지된다")
        void applyZeroPercentDiscount() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    BigDecimal.ZERO, "0% 할인");
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(10000));
        }
    }

    @DisplayName("최소 금액 제한 테스트")
    @Nested
    class MinimumAmountTest {

        @Test
        @DisplayName("최소 금액 이상일 때 할인이 적용된다")
        void applyDiscountWhenAboveMinimum() {
            RateDiscountPolicy policy = RateDiscountPolicy.withMinimum(
                    new BigDecimal("0.10"), "10% 할인", Money.won(5000));
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(9000));
        }

        @Test
        @DisplayName("최소 금액 미만일 때 할인이 적용되지 않는다")
        void noDiscountWhenBelowMinimum() {
            RateDiscountPolicy policy = RateDiscountPolicy.withMinimum(
                    new BigDecimal("0.10"), "10% 할인", Money.won(5000));
            Money originalPrice = Money.won(3000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(3000));
        }

        @Test
        @DisplayName("최소 금액과 정확히 같을 때 할인이 적용된다")
        void applyDiscountWhenExactMinimum() {
            RateDiscountPolicy policy = RateDiscountPolicy.withMinimum(
                    new BigDecimal("0.10"), "10% 할인", Money.won(5000));
            Money originalPrice = Money.won(5000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(4500));
        }
    }

    @DisplayName("최대 할인 한도 테스트")
    @Nested
    class MaximumDiscountTest {

        @Test
        @DisplayName("계산된 할인이 한도 이내일 때 정상 적용된다")
        void applyDiscountWithinCap() {
            RateDiscountPolicy policy = RateDiscountPolicy.withCap(
                    new BigDecimal("0.20"), "20% 할인", Money.won(5000));
            Money originalPrice = Money.won(20000); // 20% = 4000원 할인 (한도 이내)

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(16000));
        }

        @Test
        @DisplayName("계산된 할인이 한도를 초과할 때 한도만큼만 할인된다")
        void applyDiscountWithCap() {
            RateDiscountPolicy policy = RateDiscountPolicy.withCap(
                    new BigDecimal("0.20"), "20% 할인", Money.won(5000));
            Money originalPrice = Money.won(50000); // 20% = 10000원 할인 (한도 초과)

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(45000)); // 5000원만 할인
        }
    }

    @DisplayName("모든 제한이 있는 정책 테스트")
    @Nested
    class CompleteConstraintsTest {

        @Test
        @DisplayName("최소 금액과 최대 할인 한도가 모두 적용된다")
        void applyAllConstraints() {
            RateDiscountPolicy policy = RateDiscountPolicy.withLimits(
                    new BigDecimal("0.30"), "30% 할인",
                    Money.won(10000), Money.won(8000), 50);

            // 최소 금액 미만 - 할인 없음
            Money price1 = Money.won(5000);
            Money result1 = policy.applyDiscount(price1, context);
            assertThat(result1).isEqualTo(Money.won(5000));

            // 최소 금액 이상, 한도 이내 - 정상 할인
            Money price2 = Money.won(20000); // 30% = 6000원 할인
            Money result2 = policy.applyDiscount(price2, context);
            assertThat(result2).isEqualTo(Money.won(14000));

            // 최소 금액 이상, 한도 초과 - 한도만큼만 할인
            Money price3 = Money.won(50000); // 30% = 15000원 -> 8000원으로 제한
            Money result3 = policy.applyDiscount(price3, context);
            assertThat(result3).isEqualTo(Money.won(42000));
        }
    }

    @DisplayName("적용 가능성 테스트")
    @Nested
    class ApplicabilityTest {

        @Test
        @DisplayName("기본 정률 할인 정책은 항상 적용 가능하다")
        void alwaysApplicable() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    new BigDecimal("0.10"), "10% 할인");

            assertThat(policy.isApplicable(context)).isTrue();
        }
    }

    @DisplayName("우선순위 테스트")
    @Nested
    class PriorityTest {

        @Test
        @DisplayName("기본 우선순위는 100이다")
        void defaultPriority() {
            RateDiscountPolicy policy = RateDiscountPolicy.create(
                    new BigDecimal("0.10"), "10% 할인");

            assertThat(policy.getPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("사용자 지정 우선순위가 설정된다")
        void customPriority() {
            RateDiscountPolicy policy = RateDiscountPolicy.withLimits(
                    new BigDecimal("0.10"), "10% 할인",
                    null, null, 50);

            assertThat(policy.getPriority()).isEqualTo(50);
        }
    }
}