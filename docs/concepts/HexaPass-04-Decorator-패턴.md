# HexaPass-04-Decorator 패턴 — 개념정리 (리팩토링 버전)

## 1) 정의

**데코레이터(Decorator) 패턴**은 기존 객체에 새로운 책임(기능)을 동적으로 추가할 수 있도록, 동일한 인터페이스를 따르는 래퍼(wrapper) 객체를 사용하는 패턴이다. 상속 대신 합성을 사용하여 기능 확장을 유연하게 만든다.

---

## 2) 핵심 요소

* **Component 인터페이스**: 기본 기능 계약 정의.
* **Concrete Component**: 실제 핵심 기능을 수행하는 객체.
* **Decorator 추상 클래스**: Component를 구현하며, 내부에 Component를 합성.
* **Concrete Decorator**: 실제로 기능을 확장/추가하는 객체.

---

## 3) 장점

1. 런타임에 기능 추가/제거 가능 → 유연성 극대화.
2. 다중 조합 가능 → 기능 조합 폭발을 if-else 대신 구성(Composition)으로 해결.
3. OCP(개방-폐쇄 원칙) 충족 → 기존 코드 수정 없이 기능 확장 가능.

---

## 4) 단점 / 주의점

* 데코레이터 체인이 깊어질수록 디버깅이 어려워짐.
* 객체 구조가 복잡해져 추적성이 낮아질 수 있음.
* 순서에 따라 결과가 달라질 수 있으므로 조합 순서를 주의해야 함.

---

## 5) 대안 및 비교

* **상속**: 간단하지만 조합 수가 폭발하고, 다중상속 문제 발생.
* **전략 패턴**: 알고리즘 교체에 초점, 데코레이터는 기능 추가에 초점.
* **AOP (관점 지향 프로그래밍)**: 횡단 관심사(로깅, 트랜잭션)에 효과적이나 런타임 가시성이 낮음.

---

## 6) HexaPass 적용 예시

### 할인 정책 데코레이터

```java
public interface DiscountPolicy {
    Money apply(Money price);
}

public class BasePricePolicy implements DiscountPolicy {
    @Override
    public Money apply(Money price) {
        return price; // 기본값 반환
    }
}

public abstract class DiscountDecorator implements DiscountPolicy {
    protected final DiscountPolicy next;

    protected DiscountDecorator(DiscountPolicy next) {
        this.next = next;
    }

    @Override
    public Money apply(Money price) {
        return next.apply(price);
    }
}

public class LoyaltyDiscount extends DiscountDecorator {
    public LoyaltyDiscount(DiscountPolicy next) { super(next); }

    @Override
    public Money apply(Money price) {
        Money discounted = super.apply(price);
        return discounted.multiply(0.9); // 10% 할인
    }
}

public class CouponDiscount extends DiscountDecorator {
    public CouponDiscount(DiscountPolicy next) { super(next); }

    @Override
    public Money apply(Money price) {
        Money discounted = super.apply(price);
        return discounted.minus(new Money(5000)); // 5000원 할인
    }
}
```

### 실행 예시

```java
DiscountPolicy policy = new LoyaltyDiscount(new CouponDiscount(new BasePricePolicy()));
Money finalPrice = policy.apply(new Money(30000));
System.out.println(finalPrice); // 쿠폰 + 로열티 할인 적용
```

➡️ 데코 순서 변경 시 결과가 달라질 수 있음.

---

## 7) 체크리스트

✅ 기능 확장이 상속 대신 합성으로 되어 있는가?
✅ 데코레이터 순서에 따른 결과가 예상 가능한가?
✅ 체인 깊이가 너무 복잡하지 않은가?

---

## 8) 학습 과제

1. `WeekendDiscount` 데코레이터를 추가해 주말에만 추가 할인되도록 구현하기.
2. 할인 데코레이터 적용 순서를 바꿔서 결과 차이를 테스트하기.
3. 예약 검증 로직에 `OverlapCheck`, `PlanLimitCheck` 데코레이터를 적용해보기.
4. 디버깅 편의를 위해 데코레이터 체인의 동작 로그를 출력하는 `LoggingDecorator` 구현하기.

---

📌 이 문서는 데코레이터 패턴을 HexaPass의 **할인 정책 & 예약 검증 로직**에 적용해 학습할 수 있도록 구성됨.
