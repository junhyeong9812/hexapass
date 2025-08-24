package com.hexapass.domain.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 금액을 나타내는 값 객체
 * 불변 객체로 설계되어 생성 후 상태 변경 불가
 * 통화와 함께 관리하여 서로 다른 통화끼리의 잘못된 연산을 방지
 */
public final class Money implements Comparable<Money> {

    private final BigDecimal amount;
    private final Currency currency;

    // 기본 통화들
    private static final Currency KRW = Currency.getInstance("KRW");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private Money(BigDecimal amount, Currency currency) {
        this.amount = validateAmount(amount);
        this.currency = validateCurrency(currency);
    }

    /**
     * 금액과 통화를 지정하여 Money 객체 생성
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * long 타입 금액으로 Money 객체 생성
     */
    public static Money of(long amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * 원화(KRW) Money 객체 생성 - 편의 메서드
     */
    public static Money won(long amount) {
        return new Money(BigDecimal.valueOf(amount), KRW);
    }

    public static Money won(BigDecimal amount) {
        return new Money(amount, KRW);
    }

    /**
     * 달러(USD) Money 객체 생성 - 편의 메서드
     */
    public static Money usd(long amount) {
        return new Money(BigDecimal.valueOf(amount), USD);
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, USD);
    }

    /**
     * 유로(EUR) Money 객체 생성 - 편의 메서드
     */
    public static Money eur(long amount) {
        return new Money(BigDecimal.valueOf(amount), EUR);
    }

    /**
     * 0원 객체 생성
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zeroWon() {
        return zero(KRW);
    }

    // =========================
    // 산술 연산 메서드들
    // =========================

    /**
     * 더하기 - 새로운 Money 객체 반환 (불변성 유지)
     */
    public Money add(Money other) {
        validateSameCurrency(other, "더하기");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 빼기
     */
    public Money subtract(Money other) {
        validateSameCurrency(other, "빼기");
        BigDecimal result = this.amount.subtract(other.amount);
        return new Money(result, this.currency);
    }

    /**
     * 곱하기 (배수)
     */
    public Money multiply(BigDecimal multiplier) {
        if (multiplier == null) {
            throw new IllegalArgumentException("곱할 값은 null일 수 없습니다");
        }
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("곱할 값은 0 이상이어야 합니다");
        }

        BigDecimal result = this.amount.multiply(multiplier);
        return new Money(result, this.currency);
    }

    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * 나누기
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException("나눌 값은 null일 수 없습니다");
        }
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("나눌 값은 0보다 커야 합니다");
        }

        BigDecimal result = this.amount.divide(divisor, 2, RoundingMode.HALF_UP);
        return new Money(result, this.currency);
    }

    public Money divide(double divisor) {
        return divide(BigDecimal.valueOf(divisor));
    }

    // =========================
    // 비교 메서드들
    // =========================

    /**
     * 더 큰지 비교
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other, "크기 비교");
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 더 작은지 비교
     */
    public boolean isLessThan(Money other) {
        validateSameCurrency(other, "크기 비교");
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * 0인지 확인
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 양수인지 확인
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Money money = (Money) obj;
        return Objects.equals(amount, money.amount) &&
                Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format("%s %s", amount, currency.getCurrencyCode());
    }

    @Override
    public int compareTo(Money other) {
        validateSameCurrency(other, "비교");
        return this.amount.compareTo(other.amount);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다. 입력된 금액: " + amount);
        }
        return amount;
    }

    private Currency validateCurrency(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("통화는 null일 수 없습니다");
        }
        return currency;
    }

    private void validateSameCurrency(Money other, String operation) {
        if (other == null) {
            throw new IllegalArgumentException("비교할 Money 객체는 null일 수 없습니다");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("다른 통화끼리 %s 연산할 수 없습니다. (%s vs %s)",
                            operation, this.currency.getCurrencyCode(), other.currency.getCurrencyCode()));
        }
    }
}