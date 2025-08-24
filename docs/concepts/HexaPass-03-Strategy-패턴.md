# HexaPass-03-Strategy 패턴 — 개념정리 (리팩토링 버전)

## 1) 정의

**전략(Strategy) 패턴**은 알고리즘 군(여러 방법들)을 정의하고, 각각을 캡슐화하여 런타임에 교체할 수 있도록 하는 패턴이다. 조건 분기문(if/else, switch)을 제거하고, 알고리즘 선택을 객체 위임으로 처리한다.

---

## 2) 핵심 요소

* **Strategy 인터페이스**: 공통 알고리즘 계약 정의.
* **Concrete Strategy**: 실제 알고리즘 구현체.
* **Context**: 전략을 사용하는 클라이언트. 실행 시점에 전략을 주입받아 동작.

---

## 3) 장점

1. 조건 분기 제거 → 코드 가독성/유지보수성 향상.
2. 알고리즘 추가/변경 시 기존 코드 수정 없이 확장 가능 (OCP 충족).
3. 단위 테스트 용이 (전략 개별 테스트).

---

## 4) 단점 / 주의점

* 전략 클래스가 많아져 관리 복잡성 증가.
* 전략 선택 로직은 별도로 필요 (팩토리/DI로 보완).

---

## 5) 대안 및 비교

* **템플릿 메서드 패턴**: 알고리즘 골격을 상위 클래스에 두고 세부 단계만 하위 클래스에서 구현.

    * 장점: 중복 제거 쉬움.
    * 단점: 상속 구조라 런타임 교체가 불가.
* **조건 분기(if/switch)**: 전략이 적으면 단순.

    * 단점: 전략 수 증가 시 유지보수 악화.
* **룰 엔진**: 복잡한 정책을 선언적으로 표현.

    * 장점: 비즈니스 규칙에 적합.
    * 단점: 러닝커브, 성능 오버헤드.

---

## 6) HexaPass 적용 예시

### 할인 정책 전략 인터페이스

```java
public interface DiscountPolicy {
    Money apply(Money price);
}
```

### 정률 할인 구현체

```java
public class RateDiscountPolicy implements DiscountPolicy {
    private final double rate;
    public RateDiscountPolicy(double rate) { this.rate = rate; }

    @Override
    public Money apply(Money price) {
        return price.multiply(1 - rate);
    }
}
```

### 정액 할인 구현체

```java
public class AmountDiscountPolicy implements DiscountPolicy {
    private final Money discount;
    public AmountDiscountPolicy(Money discount) { this.discount = discount; }

    @Override
    public Money apply(Money price) {
        return price.minus(discount);
    }
}
```

### Context 예시 (결제 서비스)

```java
public class PaymentService {
    private final DiscountPolicy discountPolicy;

    public PaymentService(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public Money pay(Money originalPrice) {
        return discountPolicy.apply(originalPrice);
    }
}
```

➡️ 실행 시점에 `RateDiscountPolicy` 또는 `AmountDiscountPolicy`를 주입받아 동작.

---

## 7) 체크리스트

✅ 전략 인터페이스는 하나의 책임만 갖는가?
✅ 새로운 전략 추가 시 기존 Context 수정이 필요 없는가?
✅ 전략 객체는 무상태(stateless)로 설계되어 동시성 문제가 없는가?

---

## 8) 학습 과제

1. `CouponDiscountPolicy`를 추가 구현하고 기존 코드 수정 없이 적용 가능한지 확인하기.
2. 여러 할인 정책을 조합하는 `CompositeDiscountPolicy`를 작성해보기.
3. JUnit으로 각각의 `DiscountPolicy` 전략을 단위 테스트 작성.
4. `PaymentService`를 DI 컨테이너(Spring)에서 전략 주입받도록 리팩토링.

---

📌 이 문서는 전략 패턴을 HexaPass 도메인의 **할인 정책** 시나리오로 구체화하여 학습할 수 있도록 구성됨.
