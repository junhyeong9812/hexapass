package com.hexapass.domain.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money 클래스 테스트")
class MoneyTest {

    @DisplayName("Money 객체 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 금액과 통화로 Money 객체를 생성할 수 있다")
        void createValidMoney() {
            // Given
            BigDecimal amount = BigDecimal.valueOf(10000);
            Currency currency = Currency.getInstance("KRW");

            // When
            Money money = Money.of(amount, currency);

            // Then
            assertThat(money).isNotNull();
            assertThat(money.getAmount()).isEqualTo(amount);
            assertThat(money.getCurrency()).isEqualTo(currency);
            assertThat(money.getCurrencyCode()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("long 타입 금액으로 Money 객체를 생성할 수 있다")
        void createMoneyWithLongAmount() {
            // Given
            long amount = 10000L;
            Currency currency = Currency.getInstance("KRW");

            // When
            Money money = Money.of(amount, currency);

            // Then
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(amount));
            assertThat(money.getCurrency()).isEqualTo(currency);
        }

        @Test
        @DisplayName("원화 편의 메서드로 Money 객체를 생성할 수 있다")
        void createKoreanWonMoney() {
            // When
            Money money = Money.won(50000);

            // Then
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(50000));
            assertThat(money.getCurrencyCode()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("달러 편의 메서드로 Money 객체를 생성할 수 있다")
        void createUsDollarMoney() {
            // When
            Money money = Money.usd(100);

            // Then
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(100));
            assertThat(money.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("0원 Money 객체를 생성할 수 있다")
        void createZeroMoney() {
            // When
            Money zeroKrw = Money.zeroWon();
            Money zeroUsd = Money.zero(Currency.getInstance("USD"));

            // Then
            assertThat(zeroKrw.getAmount()).isEqualTo(BigDecimal.ZERO);
            assertThat(zeroKrw.isZero()).isTrue();
            assertThat(zeroUsd.getAmount()).isEqualTo(BigDecimal.ZERO);
            assertThat(zeroUsd.isZero()).isTrue();
        }

        @ParameterizedTest
        @DisplayName("음수 금액으로 생성하면 예외가 발생한다")
        @ValueSource(longs = {-1, -100, -50000})
        void createWithNegativeAmount(long negativeAmount) {
            // When & Then
            assertThatThrownBy(() -> Money.won(negativeAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("금액은 0 이상이어야 합니다");
        }

        @Test
        @DisplayName("null 금액으로 생성하면 예외가 발생한다")
        void createWithNullAmount() {
            // Given
            BigDecimal nullAmount = null;
            Currency currency = Currency.getInstance("KRW");

            // When & Then
            assertThatThrownBy(() -> Money.of(nullAmount, currency))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("금액은 null일 수 없습니다");
        }

        @Test
        @DisplayName("null 통화로 생성하면 예외가 발생한다")
        void createWithNullCurrency() {
            // Given
            BigDecimal amount = BigDecimal.valueOf(10000);
            Currency nullCurrency = null;

            // When & Then
            assertThatThrownBy(() -> Money.of(amount, nullCurrency))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("통화는 null일 수 없습니다");
        }
    }

    @DisplayName("Money 산술 연산 테스트")
    @Nested
    class ArithmeticOperationTest {

        @Test
        @DisplayName("같은 통화끼리 더할 수 있다")
        void addSameCurrency() {
            // Given
            Money money1 = Money.won(1000);
            Money money2 = Money.won(500);

            // When
            Money result = money1.add(money2);

            // Then
            assertThat(result).isEqualTo(Money.won(1500));
            // 원본 객체는 변경되지 않음 (불변성 확인)
            assertThat(money1).isEqualTo(Money.won(1000));
            assertThat(money2).isEqualTo(Money.won(500));
        }

        @Test
        @DisplayName("같은 통화끼리 뺄 수 있다")
        void subtractSameCurrency() {
            // Given
            Money money1 = Money.won(1000);
            Money money2 = Money.won(300);

            // When
            Money result = money1.subtract(money2);

            // Then
            assertThat(result).isEqualTo(Money.won(700));
        }

        @Test
        @DisplayName("음수 결과가 나오는 빼기도 가능하다")
        void subtractResultingNegative() {
            // Given
            Money money1 = Money.won(500);
            Money money2 = Money.won(1000);

            // When
            Money result = money1.subtract(money2);

            // Then
            assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(-500));
        }

        @Test
        @DisplayName("양수로 곱할 수 있다")
        void multiplyByPositiveNumber() {
            // Given
            Money money = Money.won(1000);

            // When
            Money result1 = money.multiply(BigDecimal.valueOf(2));
            Money result2 = money.multiply(2.5);

            // Then
            assertThat(result1).isEqualTo(Money.won(2000));
            assertThat(result2.getAmount()).isEqualTo(BigDecimal.valueOf(2500));
        }

        @Test
        @DisplayName("0으로 곱하면 0이 된다")
        void multiplyByZero() {
            // Given
            Money money = Money.won(1000);

            // When
            Money result = money.multiply(BigDecimal.ZERO);

            // Then
            assertThat(result.isZero()).isTrue();
        }

        @Test
        @DisplayName("음수로 곱하면 예외가 발생한다")
        void multiplyByNegativeNumber() {
            // Given
            Money money = Money.won(1000);

            // When & Then
            assertThatThrownBy(() -> money.multiply(BigDecimal.valueOf(-1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("곱할 값은 0 이상이어야 합니다");
        }

        @Test
        @DisplayName("양수로 나눌 수 있다")
        void divideByPositiveNumber() {
            // Given
            Money money = Money.won(1000);

            // When
            Money result1 = money.divide(BigDecimal.valueOf(2));
            Money result2 = money.divide(3.0);

            // Then
            assertThat(result1).isEqualTo(Money.won(500));
            // 나누기 3의 경우 반올림 적용 확인 (1000 ÷ 3 = 333.33...)
            assertThat(result2.getAmount()).isEqualTo(new BigDecimal("333.33"));
        }

        @Test
        @DisplayName("0으로 나누면 예외가 발생한다")
        void divideByZero() {
            // Given
            Money money = Money.won(1000);

            // When & Then
            assertThatThrownBy(() -> money.divide(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("나눌 값은 0보다 커야 합니다");
        }

        @ParameterizedTest
        @DisplayName("다른 통화끼리 연산하면 예외가 발생한다")
        @MethodSource("provideDifferentCurrencyPairs")
        void operateWithDifferentCurrencies(Money money1, Money money2) {
            // When & Then
            assertThatThrownBy(() -> money1.add(money2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("다른 통화끼리");

            assertThatThrownBy(() -> money1.subtract(money2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("다른 통화끼리");
        }

        static Stream<Arguments> provideDifferentCurrencyPairs() {
            return Stream.of(
                    Arguments.of(Money.won(1000), Money.usd(10)),
                    Arguments.of(Money.usd(50), Money.eur(30)),
                    Arguments.of(Money.eur(100), Money.won(50000))
            );
        }
    }

    @DisplayName("Money 비교 연산 테스트")
    @Nested
    class ComparisonTest {

        @Test
        @DisplayName("같은 통화끼리 크기를 비교할 수 있다")
        void compareSameCurrency() {
            // Given
            Money money1 = Money.won(1000);
            Money money2 = Money.won(500);
            Money money3 = Money.won(1000);

            // When & Then
            assertThat(money1.isGreaterThan(money2)).isTrue();
            assertThat(money2.isLessThan(money1)).isTrue();
            assertThat(money1.isGreaterThan(money3)).isFalse();
            assertThat(money1.isLessThan(money3)).isFalse();
        }

        @Test
        @DisplayName("0인지 확인할 수 있다")
        void checkIfZero() {
            // Given
            Money zeroMoney = Money.zeroWon();
            Money nonZeroMoney = Money.won(100);

            // When & Then
            assertThat(zeroMoney.isZero()).isTrue();
            assertThat(nonZeroMoney.isZero()).isFalse();
        }

        @Test
        @DisplayName("양수인지 확인할 수 있다")
        void checkIfPositive() {
            // Given
            Money positiveMoney = Money.won(100);
            Money zeroMoney = Money.zeroWon();
            Money negativeMoney = Money.won(100).subtract(Money.won(200)); // -100

            // When & Then
            assertThat(positiveMoney.isPositive()).isTrue();
            assertThat(zeroMoney.isPositive()).isFalse();
            assertThat(negativeMoney.isPositive()).isFalse();
        }

        @Test
        @DisplayName("다른 통화끼리 비교하면 예외가 발생한다")
        void compareWithDifferentCurrencies() {
            // Given
            Money won = Money.won(1000);
            Money usd = Money.usd(10);

            // When & Then
            assertThatThrownBy(() -> won.isGreaterThan(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("다른 통화끼리");
        }
    }

    @DisplayName("Money 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 금액과 통화는 동등하다")
        void equalityWithSameAmountAndCurrency() {
            // Given
            Money money1 = Money.won(1000);
            Money money2 = Money.won(1000);
            Money money3 = Money.of(BigDecimal.valueOf(1000), Currency.getInstance("KRW"));

            // When & Then
            assertThat(money1).isEqualTo(money2);
            assertThat(money1).isEqualTo(money3);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
            assertThat(money1.hashCode()).isEqualTo(money3.hashCode());
        }

        @Test
        @DisplayName("다른 금액은 동등하지 않다")
        void inequalityWithDifferentAmount() {
            // Given
            Money money1 = Money.won(1000);
            Money money2 = Money.won(2000);

            // When & Then
            assertThat(money1).isNotEqualTo(money2);
            assertThat(money1.hashCode()).isNotEqualTo(money2.hashCode());
        }

        @Test
        @DisplayName("다른 통화는 동등하지 않다")
        void inequalityWithDifferentCurrency() {
            // Given
            Money won = Money.won(1000);
            Money usd = Money.of(BigDecimal.valueOf(1000), Currency.getInstance("USD"));

            // When & Then
            assertThat(won).isNotEqualTo(usd);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            Money money = Money.won(1000);

            // When & Then
            assertThat(money).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 객체와는 동등하지 않다")
        void inequalityWithDifferentType() {
            // Given
            Money money = Money.won(1000);
            String notMoney = "1000 KRW";

            // When & Then
            assertThat(money).isNotEqualTo(notMoney);
        }
    }

    @DisplayName("Money Comparable 테스트")
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ComparableTest {

        @Test
        @DisplayName("같은 통화끼리 정렬할 수 있다")
        void comparableWithSameCurrency() {
            // Given
            Money money1 = Money.won(3000);
            Money money2 = Money.won(1000);
            Money money3 = Money.won(2000);

            // When
            var sortedList = Stream.of(money1, money2, money3)
                    .sorted()
                    .toList();

            // Then
            assertThat(sortedList).containsExactly(
                    Money.won(1000),
                    Money.won(2000),
                    Money.won(3000)
            );
        }

        @Test
        @DisplayName("같은 금액은 compareTo에서 0을 반환한다")
        void compareToWithEqualAmount() {
            // Given
            Money money1 = Money.won(1000);
            Money money2 = Money.won(1000);

            // When & Then
            assertThat(money1.compareTo(money2)).isZero();
        }

        @Test
        @DisplayName("다른 통화끼리 compareTo 하면 예외가 발생한다")
        void compareToWithDifferentCurrency() {
            // Given
            Money won = Money.won(1000);
            Money usd = Money.usd(10);

            // When & Then
            assertThatThrownBy(() -> won.compareTo(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("다른 통화끼리");
        }
    }

    @DisplayName("Money toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 형식으로 출력된다")
        void toStringFormat() {
            // Given
            Money wonMoney = Money.won(10000);
            Money usdMoney = Money.usd(100);

            // When
            String wonString = wonMoney.toString();
            String usdString = usdMoney.toString();

            // Then
            assertThat(wonString).isEqualTo("10000 KRW");
            assertThat(usdString).isEqualTo("100 USD");
        }
    }

    @DisplayName("Money 엣지 케이스 테스트")
    @Nested
    class EdgeCaseTest {

        @Test
        @DisplayName("매우 큰 금액도 처리할 수 있다")
        void handleVeryLargeAmount() {
            // Given
            BigDecimal largeAmount = new BigDecimal("999999999999999999");

            // When
            Money largeMoney = Money.of(largeAmount, Currency.getInstance("KRW"));

            // Then
            assertThat(largeMoney.getAmount()).isEqualTo(largeAmount);
        }

        @Test
        @DisplayName("소수점이 있는 금액도 처리할 수 있다")
        void handleDecimalAmount() {
            // Given
            BigDecimal decimalAmount = new BigDecimal("1000.99");

            // When
            Money decimalMoney = Money.of(decimalAmount, Currency.getInstance("USD"));

            // Then
            assertThat(decimalMoney.getAmount()).isEqualTo(decimalAmount);
        }

        @Test
        @DisplayName("연산 체이닝이 가능하다")
        void operationChaining() {
            // Given
            Money baseMoney = Money.won(1000);

            // When
            Money result = baseMoney
                    .add(Money.won(500))        // 1500
                    .multiply(BigDecimal.valueOf(2))  // 3000
                    .subtract(Money.won(1000))  // 2000
                    .divide(BigDecimal.valueOf(4));   // 500

            // Then
            assertThat(result).isEqualTo(Money.won(500));
        }
    }
}