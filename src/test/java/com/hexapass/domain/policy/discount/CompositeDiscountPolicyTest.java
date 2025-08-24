package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@DisplayName("복합 할인 정책 테스트")
@ExtendWith(MockitoExtension.class)
class CompositeDiscountPolicyTest {

    @Mock
    private DiscountPolicy policy1;

    @Mock
    private DiscountPolicy policy2;

    @Mock
    private DiscountPolicy policy3;

    private Member member;
    private MembershipPlan membershipPlan;
    private DiscountContext context;
    private Money originalPrice;

    @BeforeEach
    void setUp() {
        member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
        membershipPlan = MembershipPlan.create("BASIC", "기본 플랜", PlanType.MONTHLY,
                Money.won(30000), 30, Set.of(ResourceType.GYM));
        context = DiscountContext.of(member, membershipPlan);
        originalPrice = Money.won(10000);
    }

    @DisplayName("복합 할인 정책 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 정책 목록으로 순차적용 복합 정책을 생성할 수 있다")
        void createSequentialCompositePolicy() {
            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);

            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            assertThat(composite.getPolicies()).hasSize(2);
            assertThat(composite.getStrategy()).isEqualTo(CompositeDiscountPolicy.CombinationStrategy.SEQUENTIAL);
            assertThat(composite.getDescription()).contains("순차 할인");
        }

        @Test
        @DisplayName("최고 할인 복합 정책을 생성할 수 있다")
        void createBestDiscountCompositePolicy() {
            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);

            CompositeDiscountPolicy composite = CompositeDiscountPolicy.bestDiscount(policies, "최고 할인");

            assertThat(composite.getStrategy()).isEqualTo(CompositeDiscountPolicy.CombinationStrategy.BEST_DISCOUNT);
            assertThat(composite.getDescription()).contains("최고 할인");
        }

        @Test
        @DisplayName("우선순위 복합 정책을 생성할 수 있다")
        void createPriorityFirstCompositePolicy() {
            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);

            CompositeDiscountPolicy composite = CompositeDiscountPolicy.priorityFirst(policies, "우선순위 할인");

            assertThat(composite.getStrategy()).isEqualTo(CompositeDiscountPolicy.CombinationStrategy.PRIORITY_FIRST);
            assertThat(composite.getDescription()).contains("우선순위 할인");
        }

        @Test
        @DisplayName("빈 정책 목록으로 생성하면 예외가 발생한다")
        void createWithEmptyPolicies() {
            List<DiscountPolicy> emptyPolicies = Arrays.asList();

            assertThatThrownBy(() -> CompositeDiscountPolicy.sequential(emptyPolicies, "빈 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인 정책 목록은 null이거나 빈 목록일 수 없습니다");
        }

        @Test
        @DisplayName("null 정책 목록으로 생성하면 예외가 발생한다")
        void createWithNullPolicies() {
            assertThatThrownBy(() -> CompositeDiscountPolicy.sequential(null, "null 할인"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("할인 정책 목록은 null이거나 빈 목록일 수 없습니다");
        }
    }

    @DisplayName("순차 적용 전략 테스트")
    @Nested
    class SequentialStrategyTest {

        @Test
        @DisplayName("모든 적용 가능한 정책이 순차적으로 적용된다")
        void applyAllApplicablePoliciesSequentially() {
            // Given
            given(policy1.isApplicable(context)).willReturn(true);
            given(policy1.applyDiscount(originalPrice, context)).willReturn(Money.won(9000)); // 1000원 할인

            given(policy2.isApplicable(context)).willReturn(true);
            given(policy2.applyDiscount(Money.won(9000), context)).willReturn(Money.won(8100)); // 추가 900원 할인

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(8100)); // 총 1900원 할인
            verify(policy1).applyDiscount(originalPrice, context);
            verify(policy2).applyDiscount(Money.won(9000), context);
        }

        @Test
        @DisplayName("적용 불가능한 정책은 건너뛰고 적용 가능한 정책만 적용된다")
        void skipNotApplicablePolicies() {
            // Given
            given(policy1.isApplicable(context)).willReturn(true);
            given(policy1.applyDiscount(originalPrice, context)).willReturn(Money.won(9000));

            given(policy2.isApplicable(context)).willReturn(false); // 적용 불가

            given(policy3.isApplicable(context)).willReturn(true);
            given(policy3.applyDiscount(Money.won(9000), context)).willReturn(Money.won(8500));

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2, policy3);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(8500));
            verify(policy1).applyDiscount(originalPrice, context);
            verify(policy2, never()).applyDiscount(any(Money.class), eq(context));
            verify(policy3).applyDiscount(Money.won(9000), context);
        }

        @Test
        @DisplayName("모든 정책이 적용 불가능하면 원래 가격이 반환된다")
        void returnOriginalPriceWhenNoPolicyApplicable() {
            // Given
            given(policy1.isApplicable(context)).willReturn(false);
            given(policy2.isApplicable(context)).willReturn(false);

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(originalPrice);
        }
    }

    @DisplayName("최고 할인 전략 테스트")
    @Nested
    class BestDiscountStrategyTest {

        @Test
        @DisplayName("가장 큰 할인을 제공하는 정책만 적용된다")
        void applyPolicyWithBestDiscount() {
            // Given
            given(policy1.isApplicable(context)).willReturn(true);
            given(policy1.applyDiscount(originalPrice, context)).willReturn(Money.won(9000)); // 1000원 할인

            given(policy2.isApplicable(context)).willReturn(true);
            given(policy2.applyDiscount(originalPrice, context)).willReturn(Money.won(8500)); // 1500원 할인 (더 큰 할인)

            given(policy3.isApplicable(context)).willReturn(true);
            given(policy3.applyDiscount(originalPrice, context)).willReturn(Money.won(9200)); // 800원 할인

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2, policy3);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.bestDiscount(policies, "최고 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(8500)); // 가장 큰 할인 적용
        }

        @Test
        @DisplayName("동일한 할인 금액이면 첫 번째 정책이 선택된다")
        void selectFirstPolicyWhenSameDiscountAmount() {
            // Given
            given(policy1.isApplicable(context)).willReturn(true);
            given(policy1.applyDiscount(originalPrice, context)).willReturn(Money.won(8500)); // 1500원 할인

            given(policy2.isApplicable(context)).willReturn(true);
            given(policy2.applyDiscount(originalPrice, context)).willReturn(Money.won(8500)); // 동일한 1500원 할인

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.bestDiscount(policies, "최고 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(8500));
        }

        @Test
        @DisplayName("적용 불가능한 정책은 최고 할인 선택에서 제외된다")
        void excludeNotApplicablePoliciesFromBestSelection() {
            // Given
            given(policy1.isApplicable(context)).willReturn(false); // 적용 불가

            given(policy2.isApplicable(context)).willReturn(true);
            given(policy2.applyDiscount(originalPrice, context)).willReturn(Money.won(9000));

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.bestDiscount(policies, "최고 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(9000));
            verify(policy1, never()).applyDiscount(any(Money.class), eq(context));
        }
    }

    @DisplayName("우선순위 전략 테스트")
    @Nested
    class PriorityFirstStrategyTest {

        @Test
        @DisplayName("가장 높은 우선순위의 적용 가능한 정책만 적용된다")
        void applyHighestPriorityPolicy() {
            // Given
            given(policy1.isApplicable(context)).willReturn(true);
            given(policy1.getPriority()).willReturn(50);
            given(policy1.applyDiscount(originalPrice, context)).willReturn(Money.won(9000));

            given(policy2.isApplicable(context)).willReturn(true);
            given(policy2.getPriority()).willReturn(10); // 더 높은 우선순위 (낮은 숫자)
            given(policy2.applyDiscount(originalPrice, context)).willReturn(Money.won(8500));

            given(policy3.isApplicable(context)).willReturn(true);
            given(policy3.getPriority()).willReturn(100);
            given(policy3.applyDiscount(originalPrice, context)).willReturn(Money.won(9500));

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2, policy3);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.priorityFirst(policies, "우선순위 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(8500)); // 우선순위 10인 policy2 적용
        }

        @Test
        @DisplayName("적용 불가능한 정책은 우선순위 선택에서 제외된다")
        void excludeNotApplicablePoliciesFromPrioritySelection() {
            // Given
            given(policy1.isApplicable(context)).willReturn(false); // 적용 불가 (우선순위 1)
            given(policy1.getPriority()).willReturn(1);

            given(policy2.isApplicable(context)).willReturn(true);
            given(policy2.getPriority()).willReturn(50);
            given(policy2.applyDiscount(originalPrice, context)).willReturn(Money.won(9000));

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.priorityFirst(policies, "우선순위 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(9000)); // policy2 적용
            verify(policy1, never()).applyDiscount(any(Money.class), eq(context));
        }
    }

    @DisplayName("적용 가능성 테스트")
    @Nested
    class ApplicabilityTest {

        @Test
        @DisplayName("하나라도 적용 가능한 정책이 있으면 복합 정책도 적용 가능하다")
        void applicableWhenAnyPolicyApplicable() {
            // Given
            given(policy1.isApplicable(context)).willReturn(false);
            given(policy2.isApplicable(context)).willReturn(true); // 하나라도 적용 가능

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            // When & Then
            assertThat(composite.isApplicable(context)).isTrue();
        }

        @Test
        @DisplayName("모든 정책이 적용 불가능하면 복합 정책도 적용 불가능하다")
        void notApplicableWhenAllPoliciesNotApplicable() {
            // Given
            given(policy1.isApplicable(context)).willReturn(false);
            given(policy2.isApplicable(context)).willReturn(false);

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            // When & Then
            assertThat(composite.isApplicable(context)).isFalse();
        }
    }

    @DisplayName("우선순위 테스트")
    @Nested
    class PriorityTest {

        @Test
        @DisplayName("복합 정책의 우선순위는 포함된 정책 중 가장 높은 우선순위와 같다")
        void compositePriorityEqualsHighestPriorityOfIncludedPolicies() {
            // Given
            given(policy1.getPriority()).willReturn(50);
            given(policy2.getPriority()).willReturn(10); // 가장 높은 우선순위
            given(policy3.getPriority()).willReturn(100);

            List<DiscountPolicy> policies = Arrays.asList(policy1, policy2, policy3);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "순차 할인");

            // When & Then
            assertThat(composite.getPriority()).isEqualTo(10);
        }

        @Test
        @DisplayName("정책이 없을 때 우선순위는 최대값이다")
        void defaultPriorityWhenNoPolicies() {
            // Given - 빈 목록을 직접 테스트할 수는 없으므로 Mock으로 시뮬레이션
            List<DiscountPolicy> policies = Arrays.asList(policy1);
            given(policy1.getPriority()).willReturn(Integer.MAX_VALUE);

            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "빈 할인");

            // When & Then
            assertThat(composite.getPriority()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @DisplayName("실제 정책들과의 통합 테스트")
    @Nested
    class IntegrationWithRealPoliciesTest {

        @Test
        @DisplayName("실제 할인 정책들을 조합하여 순차 적용할 수 있다")
        void combineRealPoliciesSequentially() {
            // Given
            RateDiscountPolicy ratePolicy = RateDiscountPolicy.create(
                    new BigDecimal("0.10"), "10% 할인");
            AmountDiscountPolicy amountPolicy = AmountDiscountPolicy.create(
                    Money.won(1000), "1000원 추가 할인");

            List<DiscountPolicy> policies = Arrays.asList(ratePolicy, amountPolicy);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.sequential(policies, "10% + 1000원 할인");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            // 10000 * 0.9 = 9000, 9000 - 1000 = 8000
            assertThat(result).isEqualTo(Money.won(8000));
        }

        @Test
        @DisplayName("실제 할인 정책들 중 최고 할인을 선택할 수 있다")
        void selectBestDiscountFromRealPolicies() {
            // Given
            RateDiscountPolicy ratePolicy = RateDiscountPolicy.create(
                    new BigDecimal("0.15"), "15% 할인"); // 1500원 할인
            AmountDiscountPolicy amountPolicy = AmountDiscountPolicy.create(
                    Money.won(2000), "2000원 할인"); // 2000원 할인 (더 큰 할인)

            List<DiscountPolicy> policies = Arrays.asList(ratePolicy, amountPolicy);
            CompositeDiscountPolicy composite = CompositeDiscountPolicy.bestDiscount(policies, "최고 할인 선택");

            // When
            Money result = composite.applyDiscount(originalPrice, context);

            // Then
            assertThat(result).isEqualTo(Money.won(8000)); // 2000원 할인 적용
        }
    }
}