# HexaPass-02-SOLID-개념정리 (리팩토링 버전)

## 1) 정의

**SOLID 원칙**은 객체지향 설계에서 코드 품질을 높이고 유지보수성을 확보하기 위한 다섯 가지 핵심 원칙이다.

* **SRP (단일 책임 원칙)**: 한 클래스는 오직 하나의 책임만 가져야 한다.
* **OCP (개방-폐쇄 원칙)**: 확장에는 열려 있고, 수정에는 닫혀 있어야 한다.
* **LSP (리스코프 치환 원칙)**: 하위 타입은 상위 타입의 계약을 위배하지 않고 대체 가능해야 한다.
* **ISP (인터페이스 분리 원칙)**: 클라이언트는 자신이 사용하지 않는 메서드에 의존하면 안 된다.
* **DIP (의존성 역전 원칙)**: 고수준 모듈은 저수준 모듈에 의존하지 않고, 추상에 의존해야 한다.

---

## 2) 장점

1. 변경에 강한 설계 → 유지보수성 향상.
2. 결합도 감소, 응집도 증가.
3. 테스트 용이성 향상 (mock/stub 대체 가능).
4. 재사용성 증가.

---

## 3) 단점 / 주의점

* 과도한 추상화는 설계를 복잡하게 만들어 학습 비용과 초기 구현 시간이 증가한다.
* 단순 CRUD 프로젝트나 작은 규모 시스템에서는 과설계가 될 수 있다.
* SRP를 지나치게 적용하면 너무 많은 클래스로 쪼개져 관리가 어려워질 수 있다.

---

## 4) 대안 및 보완 원칙

* **KISS (Keep It Simple, Stupid)**: 단순함 유지, 불필요한 복잡성 배제.
* **YAGNI (You Aren’t Gonna Need It)**: 필요할 때만 기능 추가.
* **LoD (Law of Demeter)**: 최소 지식의 원칙, 객체는 꼭 필요한 대상과만 소통.
* **성능 최적화는 사후에**: 미리 추상화보다 실제 성능 병목 파악 후 대응.

---

## 5) HexaPass 적용 예시

### SRP 적용 — `ReservationPolicy` 분리

```java
public class ReservationPolicy {
    public boolean canReserve(Member member, Resource resource, DateRange dateRange) {
        // 예약 가능 여부 로직 (동시 예약 제한, 멤버십 조건 검증 등)
        return member.hasActiveMembership() && resource.isAvailable(dateRange);
    }
}
```

➡️ `Reservation` 엔티티가 모든 검증 책임을 가지지 않고 정책 객체로 분리.

### OCP 적용 — 할인 정책 확장

```java
public interface DiscountPolicy {
    Money apply(Money price);
}

public class RateDiscountPolicy implements DiscountPolicy {
    private final double rate;
    public RateDiscountPolicy(double rate) { this.rate = rate; }
    public Money apply(Money price) { return price.multiply(1 - rate); }
}

public class AmountDiscountPolicy implements DiscountPolicy {
    private final Money discount;
    public AmountDiscountPolicy(Money discount) { this.discount = discount; }
    public Money apply(Money price) { return price.minus(discount); }
}
```

➡️ 새로운 할인 정책 추가 시 기존 코드 수정 없이 확장 가능.

### DIP 적용 — 결제 포트

```java
public interface PaymentPort {
    PaymentResult pay(Order order, Money amount);
}

public class KakaoPayAdapter implements PaymentPort {
    public PaymentResult pay(Order order, Money amount) {
        // 카카오페이 API 연동
    }
}

public class PaymentService {
    private final PaymentPort paymentPort;
    public PaymentService(PaymentPort paymentPort) { this.paymentPort = paymentPort; }

    public PaymentResult process(Order order) {
        return paymentPort.pay(order, order.totalPrice());
    }
}
```

➡️ 서비스는 포트(추상)에만 의존, 구체 구현은 어댑터에서.

---

## 6) 체크리스트

✅ 클래스가 하나의 책임만 가지는가? (SRP)
✅ 새로운 요구사항이 추가될 때 기존 코드를 수정하지 않고 확장 가능한가? (OCP)
✅ 하위 타입이 상위 타입의 계약을 위배하지 않는가? (LSP)
✅ 인터페이스가 클라이언트 맞춤형으로 분리되어 있는가? (ISP)
✅ 서비스가 구체 구현 대신 추상에 의존하고 있는가? (DIP)

---

## 7) 학습 과제

1. `DiscountPolicy`에 신규 `CouponDiscountPolicy`를 추가해보기. (OCP 검증)
2. `PaymentPort` 인터페이스를 `authorize`, `capture`, `refund` 등 기능별로 분리해보기. (ISP 검증)
3. `ReservationPolicy` 책임을 추가/분리하면서 SRP가 깨지는 순간을 찾아보고 개선하기.
4. Mock 객체를 활용해 `PaymentService` 단위 테스트 작성하기 (DIP 활용).

---

📌 이 문서는 SOLID 원칙을 실제 HexaPass 도메인 예시와 코드로 학습할 수 있도록 구성됨.
