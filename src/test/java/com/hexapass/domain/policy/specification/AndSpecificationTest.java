package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@DisplayName("AND 조건 사양 테스트")
@ExtendWith(MockitoExtension.class)
class AndSpecificationTest {

    @Mock
    private ReservationSpecification leftSpec;

    @Mock
    private ReservationSpecification rightSpec;

    @Mock
    private ReservationContext context;

    private AndSpecification andSpecification;

    @BeforeEach
    void setUp() {
        andSpecification = new AndSpecification(leftSpec, rightSpec);
    }

    @DisplayName("AND 조건 사양 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 두 사양으로 AND 조건 사양을 생성할 수 있다")
        void createValidAndSpecification() {
            // Given & When
            AndSpecification spec = new AndSpecification(leftSpec, rightSpec);

            // Then
            assertThat(spec.getLeft()).isEqualTo(leftSpec);
            assertThat(spec.getRight()).isEqualTo(rightSpec);
        }

        @Test
        @DisplayName("좌측 사양이 null이면 예외가 발생한다")
        void createWithNullLeftSpec() {
            assertThatThrownBy(() -> new AndSpecification(null, rightSpec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("좌측 사양은 null일 수 없습니다");
        }

        @Test
        @DisplayName("우측 사양이 null이면 예외가 발생한다")
        void createWithNullRightSpec() {
            assertThatThrownBy(() -> new AndSpecification(leftSpec, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("우측 사양은 null일 수 없습니다");
        }

        @Test
        @DisplayName("양쪽 사양이 모두 null이면 예외가 발생한다")
        void createWithBothSpecsNull() {
            assertThatThrownBy(() -> new AndSpecification(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("좌측 사양은 null일 수 없습니다");
        }
    }

    @DisplayName("AND 조건 만족도 테스트")
    @Nested
    class SatisfactionTest {

        @Test
        @DisplayName("양쪽 사양이 모두 만족될 때 true를 반환한다")
        void returnTrueWhenBothSpecificationsSatisfied() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(true);
            given(rightSpec.isSatisfiedBy(context)).willReturn(true);

            // When
            boolean result = andSpecification.isSatisfiedBy(context);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("좌측 사양만 만족될 때 false를 반환한다")
        void returnFalseWhenOnlyLeftSpecificationSatisfied() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(true);
            given(rightSpec.isSatisfiedBy(context)).willReturn(false);

            // When
            boolean result = andSpecification.isSatisfiedBy(context);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("우측 사양만 만족될 때 false를 반환한다")
        void returnFalseWhenOnlyRightSpecificationSatisfied() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(false);
            given(rightSpec.isSatisfiedBy(context)).willReturn(true);

            // When
            boolean result = andSpecification.isSatisfiedBy(context);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("양쪽 사양이 모두 만족되지 않을 때 false를 반환한다")
        void returnFalseWhenBothSpecificationsNotSatisfied() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(false);
            given(rightSpec.isSatisfiedBy(context)).willReturn(false);

            // When
            boolean result = andSpecification.isSatisfiedBy(context);

            // Then
            assertThat(result).isFalse();
        }
    }

    @DisplayName("단락 평가 (Short-circuit) 테스트")
    @Nested
    class ShortCircuitEvaluationTest {

        @Test
        @DisplayName("좌측 사양이 false이면 우측 사양을 평가하지 않는다")
        void shortCircuitWhenLeftSpecificationFalse() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(false);

            // When
            boolean result = andSpecification.isSatisfiedBy(context);

            // Then
            assertThat(result).isFalse();
            verify(leftSpec).isSatisfiedBy(context);
            verify(rightSpec, never()).isSatisfiedBy(context); // 우측은 호출되지 않음
        }

        @Test
        @DisplayName("좌측 사양이 true이면 우측 사양도 평가한다")
        void evaluateRightSpecificationWhenLeftSpecificationTrue() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(true);
            given(rightSpec.isSatisfiedBy(context)).willReturn(false);

            // When
            boolean result = andSpecification.isSatisfiedBy(context);

            // Then
            assertThat(result).isFalse();
            verify(leftSpec).isSatisfiedBy(context);
            verify(rightSpec).isSatisfiedBy(context); // 우측도 호출됨
        }
    }

    @DisplayName("설명 테스트")
    @Nested
    class DescriptionTest {

        @Test
        @DisplayName("두 사양의 설명이 AND로 결합된다")
        void combineDescriptionsWithAnd() {
            // Given
            given(leftSpec.getDescription()).willReturn("좌측 조건");
            given(rightSpec.getDescription()).willReturn("우측 조건");

            // When
            String description = andSpecification.getDescription();

            // Then
            assertThat(description).isEqualTo("(좌측 조건) AND (우측 조건)");
        }

        @Test
        @DisplayName("복잡한 설명도 올바르게 결합된다")
        void combineComplexDescriptions() {
            // Given
            given(leftSpec.getDescription()).willReturn("활성 회원이고 멤버십이 유효함");
            given(rightSpec.getDescription()).willReturn("리소스에 여유 공간이 있음");

            // When
            String description = andSpecification.getDescription();

            // Then
            assertThat(description).isEqualTo("(활성 회원이고 멤버십이 유효함) AND (리소스에 여유 공간이 있음)");
        }
    }

    @DisplayName("중첩 AND 조건 테스트")
    @Nested
    class NestedAndTest {

        @Mock
        private ReservationSpecification thirdSpec;

        @Test
        @DisplayName("AND 조건을 중첩하여 사용할 수 있다")
        void nestedAndConditions() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(true);
            given(rightSpec.isSatisfiedBy(context)).willReturn(true);
            given(thirdSpec.isSatisfiedBy(context)).willReturn(true);

            // When
            AndSpecification nestedAnd = new AndSpecification(andSpecification, thirdSpec);
            boolean result = nestedAnd.isSatisfiedBy(context);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("중첩 AND 조건에서 하나라도 false이면 전체가 false가 된다")
        void nestedAndConditionsFalseWhenOneFails() {
            // Given
            given(leftSpec.isSatisfiedBy(context)).willReturn(true);
            given(rightSpec.isSatisfiedBy(context)).willReturn(true);
            given(thirdSpec.isSatisfiedBy(context)).willReturn(false); // 하나만 false

            // When
            AndSpecification nestedAnd = new AndSpecification(andSpecification, thirdSpec);
            boolean result = nestedAnd.isSatisfiedBy(context);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("중첩된 AND 조건의 설명이 올바르게 생성된다")
        void nestedAndConditionDescription() {
            // Given
            given(leftSpec.getDescription()).willReturn("조건1");
            given(rightSpec.getDescription()).willReturn("조건2");
            given(thirdSpec.getDescription()).willReturn("조건3");

            // When
            AndSpecification nestedAnd = new AndSpecification(andSpecification, thirdSpec);
            String description = nestedAnd.getDescription();

            // Then
            assertThat(description).isEqualTo("((조건1) AND (조건2)) AND (조건3)");
        }
    }

    @DisplayName("실제 사양과의 결합 테스트")
    @Nested
    class RealSpecificationCombinationTest {

        @Test
        @DisplayName("실제 사양 객체들과 AND 조건으로 결합할 수 있다")
        void combineWithRealSpecifications() {
            // Given
            ActiveMemberSpecification activeMemberSpec = new ActiveMemberSpecification();
            ResourceCapacitySpecification capacitySpec = new ResourceCapacitySpecification();

            // When
            AndSpecification combined = new AndSpecification(activeMemberSpec, capacitySpec);

            // Then
            assertThat(combined.getLeft()).isEqualTo(activeMemberSpec);
            assertThat(combined.getRight()).isEqualTo(capacitySpec);
            assertThat(combined.getDescription()).contains("활성 회원 여부");
            assertThat(combined.getDescription()).contains("리소스 수용 인원 여유");
        }
    }
}