package com.hexapass.domain.policy.cancellation;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Member;
import com.hexapass.domain.policy.CancellationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("표준 취소 정책 테스트")
class StandardCancellationPolicyTest {

    private StandardCancellationPolicy policy;
    private Member member;
    private Money originalPrice;

    @BeforeEach
    void setUp() {
        policy = new StandardCancellationPolicy();
        member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
        originalPrice = Money.won(10000);
    }

    @DisplayName("시간대별 취소 수수료 테스트")
    @Nested
    class CancellationFeeByTimeTest {

        @Test
        @DisplayName("24시간 전 취소 시 수수료가 0%이다")
        void freeCancellation24HoursBefore() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(25); // 25시간 후
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(0));
        }

        @Test
        @DisplayName("6-24시간 전 취소 시 수수료가 20%이다")
        void twentyPercentFee6To24HoursBefore() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(12); // 12시간 후
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(2000)); // 20%
        }

        @Test
        @DisplayName("2-6시간 전 취소 시 수수료가 50%이다")
        void fiftyPercentFee2To6HoursBefore() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(4); // 4시간 후
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(5000)); // 50%
        }

        @Test
        @DisplayName("2시간 미만 취소 시 수수료가 80%이다")
        void eightyPercentFeeLessThan2Hours() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusMinutes(90); // 1시간 30분 후
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(8000)); // 80%
        }

        @Test
        @DisplayName("예약 시간 이후 취소 시 수수료가 100%이다")
        void fullFeeAfterReservationTime() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().minusHours(1); // 1시간 전 (이미 지남)
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(10000)); // 100%
        }
    }

    @DisplayName("경계값 테스트")
    @Nested
    class BoundaryValueTest {

        @Test
        @DisplayName("정확히 24시간 전 취소 시 수수료가 0%이다")
        void exactlyTwentyFourHoursBefore() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(24);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(0));
        }

        @Test
        @DisplayName("23시간 59분 전 취소 시 수수료가 20%이다")
        void justUnder24Hours() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(23).plusMinutes(59);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(2000)); // 20%
        }

        @Test
        @DisplayName("정확히 6시간 전 취소 시 수수료가 20%이다")
        void exactlySixHoursBefore() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(6);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(2000)); // 20%
        }

        @Test
        @DisplayName("정확히 2시간 전 취소 시 수수료가 50%이다")
        void exactlyTwoHoursBefore() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(2);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(5000)); // 50%
        }

        @Test
        @DisplayName("정확히 예약 시간에 취소 시 수수료가 100%이다")
        void exactlyAtReservationTime() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now();
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(originalPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(10000)); // 100%
        }
    }

    @DisplayName("취소 가능성 테스트")
    @Nested
    class CancellationAllowanceTest {

        @Test
        @DisplayName("표준 정책에서는 모든 취소가 허용된다")
        void allCancellationsAllowed() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(1);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When & Then
            assertThat(policy.isCancellationAllowed(context)).isTrue();
        }

        @Test
        @DisplayName("예약 시간 이후에도 취소가 허용된다 (단, 100% 수수료)")
        void cancellationAllowedAfterReservationTime() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().minusHours(2);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When & Then
            assertThat(policy.isCancellationAllowed(context)).isTrue();
        }

        @Test
        @DisplayName("취소 거부 사유는 null이다")
        void noCancellationDenialReason() {
            // Given
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(1);
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, originalPrice, member, false);

            // When & Then
            assertThat(policy.getCancellationDenialReason(context)).isNull();
        }
    }

    @DisplayName("정책 설명 테스트")
    @Nested
    class PolicyDescriptionTest {

        @Test
        @DisplayName("정책 설명이 올바르게 반환된다")
        void correctDescription() {
            String description = policy.getDescription();

            assertThat(description).contains("표준 취소 정책");
            assertThat(description).contains("시간대별 차등 수수료");
            assertThat(description).contains("24시간 전 무료");
        }
    }

    @DisplayName("다양한 가격에서의 수수료 계산 테스트")
    @Nested
    class VariousPricesTest {

        @Test
        @DisplayName("작은 금액에서도 정확한 비율로 수수료가 계산된다")
        void correctFeeForSmallAmount() {
            // Given
            Money smallPrice = Money.won(100);
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(1); // 80% 수수료 구간
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, smallPrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(smallPrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(80)); // 80%
        }

        @Test
        @DisplayName("큰 금액에서도 정확한 비율로 수수료가 계산된다")
        void correctFeeForLargeAmount() {
            // Given
            Money largePrice = Money.won(1000000);
            LocalDateTime reservationTime = LocalDateTime.now().plusHours(4); // 50% 수수료 구간
            LocalDateTime cancellationTime = LocalDateTime.now();
            CancellationContext context = CancellationContext.create(
                    reservationTime, cancellationTime, largePrice, member, false);

            // When
            Money fee = policy.calculateCancellationFee(largePrice, context);

            // Then
            assertThat(fee).isEqualTo(Money.won(500000)); // 50%
        }
    }
}