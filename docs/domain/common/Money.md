# Money.java - 상세 주석 및 설명

## 클래스 개요
`Money`는 금액을 나타내는 **값 객체(Value Object)**로, 불변 객체로 설계되었습니다.
통화(Currency)와 함께 관리하여 서로 다른 통화끼리의 잘못된 연산을 방지하고, `Comparable<Money>` 인터페이스를 구현하여 정렬과 비교가 가능합니다.

## 왜 이런 클래스가 필요한가?
1. **타입 안전성**: `BigDecimal amount`와 `String currency` 대신 강타입 사용
2. **통화 혼용 방지**: USD와 KRW를 실수로 더하는 등의 오류 방지
3. **도메인 개념 표현**: "돈"이라는 비즈니스 개념을 명확하게 코드로 표현
4. **부동소수점 오류 방지**: BigDecimal 사용으로 정확한 금액 계산

## Comparable<Money>가 필요한 이유
- **자연스러운 정렬**: Collections.sort(), TreeSet 등에서 자동 정렬
- **비교 연산**: compareTo()를 통한 표준화된 비교 방법 제공
- **범위 검색**: 최소/최대 금액 찾기, 금액 범위 검색 등

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.common;

import java.math.BigDecimal; // 정확한 십진수 계산을 위한 클래스 (부동소수점 오류 방지)
import java.math.RoundingMode; // 반올림 방식을 정의하는 열거형
import java.util.Currency; // ISO 4217 통화 코드를 나타내는 Java 표준 클래스
import java.util.Objects; // equals, hashCode 등 유틸리티 메서드 제공

/**
 * 금액을 나타내는 값 객체
 * 불변 객체로 설계되어 생성 후 상태 변경 불가
 * 통화와 함께 관리하여 서로 다른 통화끼리의 잘못된 연산을 방지
 * 
 * Comparable<Money> 구현 이유:
 * 1. 자연스러운 정렬: Collections.sort(moneyList) 가능
 * 2. TreeSet, TreeMap에서 자동 정렬
 * 3. 범위 검색: 최소/최대값 찾기 용이
 * 4. 비교 로직 표준화: <, >, == 연산을 compareTo()로 통일
 */
public final class Money implements Comparable<Money> { // final: 상속 불가

    // BigDecimal 사용 이유:
    // 1. float/double: 부동소수점 오차 발생 (0.1 + 0.2 ≠ 0.3)
    // 2. BigDecimal: 정확한 십진수 연산 보장
    private final BigDecimal amount;   // 금액 (불변)
    private final Currency currency;   // 통화 (불변)

    // 자주 사용되는 통화들을 정적 상수로 정의 (성능 최적화 + 가독성)
    private static final Currency KRW = Currency.getInstance("KRW"); // 원화
    private static final Currency USD = Currency.getInstance("USD"); // 달러
    private static final Currency EUR = Currency.getInstance("EUR"); // 유로

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * private 생성자 사용 이유:
     * 1. 객체 생성 방법 제어
     * 2. 모든 생성 경로에서 유효성 검사 강제
     * 3. 의도 명확화: won(), usd() 등으로 생성 의도 표현
     */
    private Money(BigDecimal amount, Currency currency) {
        this.amount = validateAmount(amount);     // 금액 검증 후 저장
        this.currency = validateCurrency(currency); // 통화 검증 후 저장
    }

    /**
     * 금액과 통화를 지정하여 Money 객체 생성
     * 
     * 정적 팩토리 메서드 패턴:
     * 1. 생성자보다 명확한 의미
     * 2. 매개변수에 따른 다양한 생성 방법
     * 3. 인스턴스 제어 가능 (캐싱, 싱글톤 등)
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * long 타입 금액으로 Money 객체 생성
     * 
     * 편의 메서드: 정수 금액을 쉽게 입력할 수 있도록 함
     * BigDecimal.valueOf(): long을 BigDecimal로 안전하게 변환
     */
    public static Money of(long amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * 원화(KRW) Money 객체 생성 - 편의 메서드
     * 
     * 한국 개발자들이 자주 사용할 원화 생성을 단순화
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
     * 
     * 특별한 의미를 가진 값 (영점) 생성용
     * 초기화, 합계 계산 시작값 등에 활용
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency); // BigDecimal.ZERO: 상수 0
    }

    public static Money zeroWon() {
        return zero(KRW); // 원화 0원
    }

    // =========================
    // 산술 연산 메서드들
    // =========================

    /**
     * 더하기 - 새로운 Money 객체 반환 (불변성 유지)
     * 
     * 불변 객체의 특징:
     * 1. 기존 객체를 변경하지 않음
     * 2. 항상 새로운 객체를 반환
     * 3. 스레드 안전성 보장
     */
    public Money add(Money other) {
        validateSameCurrency(other, "더하기"); // 같은 통화인지 검사
        // BigDecimal.add(): 새로운 BigDecimal 반환 (불변)
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 빼기
     * 
     * subtract() 결과가 음수가 될 수 있음 (현재는 허용)
     * 비즈니스 규칙에 따라 음수 금액 허용 여부 결정
     */
    public Money subtract(Money other) {
        validateSameCurrency(other, "빼기");
        BigDecimal result = this.amount.subtract(other.amount);
        return new Money(result, this.currency);
    }

    /**
     * 곱하기 (배수)
     * 
     * 할인율, 세율 적용 등에 사용
     * 예: money.multiply(0.9) // 10% 할인
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

    /**
     * double을 받는 곱하기 오버로드
     * 
     * 편의성 제공: multiply(1.5) 형태로 사용 가능
     * BigDecimal.valueOf(): double을 정확한 BigDecimal로 변환
     */
    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * 나누기
     * 
     * 나눗셈에서 중요한 것:
     * 1. 0으로 나누기 방지
     * 2. 소수점 처리 (RoundingMode 필요)
     * 3. scale(소수점 자릿수) 지정
     */
    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException("나눌 값은 null일 수 없습니다");
        }
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("나눌 값은 0보다 커야 합니다");
        }

        // divide(divisor, scale, roundingMode)
        // scale=2: 소수점 둘째자리까지
        // RoundingMode.HALF_UP: 반올림 (0.5 이상이면 올림)
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
     * 
     * compareTo() 반환값:
     * - 양수: this > other
     * - 0: this == other  
     * - 음수: this < other
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
     * 
     * BigDecimal과 0 비교 시 compareTo() 사용
     * equals()는 scale까지 비교하므로 부적절
     * 예: new BigDecimal("0.00").equals(BigDecimal.ZERO) = false
     *     new BigDecimal("0.00").compareTo(BigDecimal.ZERO) = 0
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

    /**
     * equals 메서드 오버라이드
     * 
     * Money는 값 객체이므로 내용이 같으면 같은 객체
     * amount와 currency 모두 일치해야 함
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // 동일 참조
        if (obj == null || getClass() != obj.getClass()) return false;

        Money money = (Money) obj;
        // Objects.equals(): null-safe 비교
        return Objects.equals(amount, money.amount) &&
                Objects.equals(currency, money.currency);
    }

    /**
     * hashCode 메서드 오버라이드
     * 
     * equals/hashCode 계약 준수:
     * equals가 true면 hashCode도 같아야 함
     */
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 사용자 친화적인 문자열 표현
     * 예: "10000 KRW", "29.99 USD"
     */
    @Override
    public String toString() {
        return String.format("%s %s", amount, currency.getCurrencyCode());
    }

    /**
     * Comparable<Money> 인터페이스 구현
     * 
     * compareTo() 메서드:
     * 1. Collections.sort()에서 자동 정렬
     * 2. TreeSet, TreeMap에서 순서 기준
     * 3. 이진 검색 등에서 활용
     * 
     * 반환값:
     * - 음수: this < other
     * - 0: this == other
     * - 양수: this > other
     */
    @Override
    public int compareTo(Money other) {
        validateSameCurrency(other, "비교"); // 같은 통화만 비교 가능
        return this.amount.compareTo(other.amount); // BigDecimal의 compareTo 사용
    }

    // =========================
    // Getter 메서드들
    // =========================

    /**
     * 금액 반환
     * 
     * BigDecimal은 불변 객체이므로 방어적 복사 불필요
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 통화 반환
     * 
     * Currency도 불변 객체
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * 통화 코드 반환 (문자열)
     * 
     * 편의 메서드: "KRW", "USD" 등의 문자열 반환
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    /**
     * 금액 유효성 검사
     * 
     * 현재 정책: 음수 금액 불허
     * 비즈니스 요구사항에 따라 변경 가능
     */
    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다. 입력된 금액: " + amount);
        }
        return amount; // 체이닝을 위한 반환
    }

    /**
     * 통화 유효성 검사
     */
    private Currency validateCurrency(Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("통화는 null일 수 없습니다");
        }
        return currency;
    }

    /**
     * 같은 통화인지 검증
     * 
     * 다른 통화끼리 연산 방지 (USD + KRW = ? 의미 없음)
     * 환율 계산은 별도의 서비스에서 처리해야 함
     */
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
```

## 주요 설계 원칙 및 패턴

### 1. 왜 BigDecimal을 사용하는가?

#### float/double의 문제점
```java
double result = 0.1 + 0.2;
System.out.println(result); // 0.30000000000000004 (예상: 0.3)
```

#### BigDecimal의 장점
- 정확한 십진수 연산
- 임의 정밀도 지원
- 다양한 반올림 모드 제공

### 2. Currency 클래스 활용
- ISO 4217 표준 통화 코드 사용
- 통화별 기본 소수점 자릿수 정보 제공
- 국제화 지원

### 3. Comparable<Money> 구현의 이점

```java
List<Money> prices = Arrays.asList(
    Money.won(10000),
    Money.won(5000), 
    Money.won(15000)
);

Collections.sort(prices); // 자동 정렬 가능
// 결과: [5000 KRW, 10000 KRW, 15000 KRW]

Money max = Collections.max(prices); // 최대값 찾기
Money min = Collections.min(prices); // 최소값 찾기
```

### 4. 불변 객체의 장점
- **스레드 안전성**: 여러 스레드에서 동시 접근 가능
- **예측 가능성**: 상태 변경 없으므로 부작용 없음
- **해시 코드 안정성**: HashMap, HashSet에서 안전하게 사용

### 5. 팩토리 메서드 패턴의 이점
- **의도 명확화**: `Money.won(1000)` vs `new Money(...)`
- **유효성 검사 집중화**: 모든 생성 경로에서 검증
- **확장 가능성**: 향후 캐싱, 싱글톤 등 추가 가능

### 6. 예외 처리 전략
- **실패 빠르게(Fail-Fast)**: 잘못된 입력 즉시 예외
- **의미있는 메시지**: 디버깅을 위한 구체적 정보
- **일관성**: 모든 메서드에서 동일한 예외 처리 패턴

이러한 설계로 Money 클래스는 안전하고 표현력 있는 금융 도메인 객체가 되었습니다.