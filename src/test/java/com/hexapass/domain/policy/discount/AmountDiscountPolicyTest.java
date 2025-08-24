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

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("정액 할인 정책 테스트")
class AmountDiscountPolicyTest {

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

    @DisplayName("정액 할인 정책 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 할인 금액으로 정책을 생성할 수 있다")
        void createValidAmountDiscountPolicy() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(1000), "1000원 할인");

            assertThat(policy.getDiscountAmount()).isEqualTo(Money.won(1000));
            assertThat(policy.getDescription()).isEqualTo("1000원 할인");
        }

        @Test
        @DisplayName("할인 금액이 null이면 예외가 발생한다")
        void createWithNullAmount() {
            assertThatThrownBy(() ->
                    AmountDiscountPolicy.create(null, "null 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인 금액은 null일 수 없습니다");
        }

        @Test
        @DisplayName("할인 금액이 0원 이하이면 예외가 발생한다")
        void createWithZeroOrNegativeAmount() {
            assertThatThrownBy(() ->
                    AmountDiscountPolicy.create(Money.won(0), "0원 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인 금액은 0보다 커야 합니다");

            assertThatThrownBy(() ->
                    AmountDiscountPolicy.create(Money.won(-1000), "음수 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("설명이 빈 문자열이면 예외가 발생한다")
        void createWithEmptyDescription() {
            assertThatThrownBy(() ->
                    AmountDiscountPolicy.create(Money.won(1000), ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("설명은 null이거나 빈 값일 수 없습니다");
        }
    }

    @DisplayName("할인 적용 테스트")
    @Nested
    class DiscountApplicationTest {

        @Test
        @DisplayName("1000원 할인이 정확히 적용된다")
        void apply1000WonDiscount() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(1000), "1000원 할인");
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(9000));
        }

        @Test
        @DisplayName("5000원 할인이 정확히 적용된다")
        void apply5000WonDiscount() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(5000), "5000원 할인");
            Money originalPrice = Money.won(20000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(15000));
        }

        @Test
        @DisplayName("할인 금액이 원래 가격보다 클 때 0원이 된다")
        void discountExceedsOriginalPrice() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(15000), "15000원 할인");
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(0));
        }

        @Test
        @DisplayName("할인 금액과 원래 가격이 같을 때 0원이 된다")
        void discountEqualsOriginalPrice() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(10000), "10000원 할인");
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(0));
        }
    }

    @DisplayName("최소 금액 제한 테스트")
    @Nested
    class MinimumAmountTest {

        @Test
        @DisplayName("최소 금액 이상일 때 할인이 적용된다")
        void applyDiscountWhenAboveMinimum() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.withMinimum(
                    Money.won(2000), "2000원 할인", Money.won(5000));
            Money originalPrice = Money.won(10000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(8000));
        }

        @Test
        @DisplayName("최소 금액 미만일 때 할인이 적용되지 않는다")
        void noDiscountWhenBelowMinimum() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.withMinimum(
                    Money.won(2000), "2000원 할인", Money.won(5000));
            Money originalPrice = Money.won(3000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(3000));
        }

        @Test
        @DisplayName("최소 금액과 정확히 같을 때 할인이 적용된다")
        void applyDiscountWhenExactMinimum() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.withMinimum(
                    Money.won(2000), "2000원 할인", Money.won(5000));
            Money originalPrice = Money.won(5000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(3000));
        }
    }

    @DisplayName("우선순위 테스트")
    @Nested
    class PriorityTest {

        @Test
        @DisplayName("기본 우선순위는 100이다")
        void defaultPriority() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(1000), "1000원 할인");

            assertThat(policy.getPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("사용자 지정 우선순위가 설정된다")
        void customPriority() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.withPriority(
                    Money.won(1000), "1000원 할인", 50);

            assertThat(policy.getPriority()).isEqualTo(50);
        }
    }

    @DisplayName("모든 옵션이 있는 정책 테스트")
    @Nested
    class CompleteOptionsTest {

        @Test
        @DisplayName("최소 금액과 우선순위가 모두 적용된다")
        void applyAllOptions() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.withOptions(
                    Money.won(3000), "3000원 할인", Money.won(10000), 30);

            // 최소 금액 미만 - 할인 없음
            Money price1 = Money.won(5000);
            Money result1 = policy.applyDiscount(price1, context);
            assertThat(result1).isEqualTo(Money.won(5000));

            // 최소 금액 이상 - 할인 적용
            Money price2 = Money.won(15000);
            Money result2 = policy.applyDiscount(price2, context);
            assertThat(result2).isEqualTo(Money.won(12000));

            // 우선순위 확인
            assertThat(policy.getPriority()).isEqualTo(30);
        }
    }

    @DisplayName("적용 가능성 테스트")
    @Nested
    class ApplicabilityTest {

        @Test
        @DisplayName("정액 할인 정책은 항상 적용 가능하다")
        void alwaysApplicable() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(1000), "1000원 할인");

            assertThat(policy.isApplicable(context)).isTrue();
        }
    }

    @DisplayName("경계값 테스트")
    @Nested
    class BoundaryValueTest {

        @Test
        @DisplayName("할인 후 최소값은 0원이다")
        void minimumDiscountedPriceIsZero() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(1000000), "100만원 할인");
            Money originalPrice = Money.won(1);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(0));
        }

        @Test
        @DisplayName("매우 큰 할인 금액도 처리된다")
        void handleLargeDiscountAmount() {
            AmountDiscountPolicy policy = AmountDiscountPolicy.create(
                    Money.won(1000000), "100만원 할인");
            Money originalPrice = Money.won(2000000);

            Money discountedPrice = policy.applyDiscount(originalPrice, context);

            assertThat(discountedPrice).isEqualTo(Money.won(1000000));
        }
    }
}