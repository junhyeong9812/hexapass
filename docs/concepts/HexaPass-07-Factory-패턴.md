# HexaPass-07-Factory 패턴 — 개념정리 (리팩토링 버전)

## 1) 정의

**팩토리(Factory) 패턴**은 객체 생성 로직을 캡슐화하여, 클라이언트가 구체 클래스를 알지 못해도 일관된 방식으로 객체를 만들 수 있게 한다. 생성 책임을 한곳에 모아 **의존성 역전(DIP)** 과 **개방-폐쇄(OCP)** 를 돕는다.

팩토리 계열 분류:

* **Simple Factory(정적 팩토리)**: 분기 기반으로 타입을 선택해 생성.
* **Factory Method**: 하위 클래스가 생성 책임을 결정(상속 기반 훅).
* **Abstract Factory**: 관련된 **객체군**을 일관성 있게 생성.
* **Builder**: 복잡한 생성 과정을 단계적으로 분리.

---

## 2) 핵심 요소

* **Product(생성 대상)**: 만들어질 객체 인터페이스/추상 타입.
* **Creator/Factory**: 생성 책임을 가진 개체(클래스/메서드/객체).
* **Configuration/Context**: 어떤 제품을 만들지 결정하는 입력(설정/환경/도메인 규칙).

---

## 3) 장점

1. 생성 로직 캡슐화 → 클라이언트 단순화, 결합도↓.
2. 새로운 제품 추가 시 기존 코드 수정 최소화(OCP).
3. 테스트 더블(Stub/Mock) 주입이 쉬워 테스트 가능성↑.

---

## 4) 단점 / 주의점

* 추상화/클래스 수 증가로 초기 복잡도↑.
* Factory Method는 상속 트리가 깊어질 수 있음.
* Abstract Factory는 조합 수가 많아지면 팩토리 수도 증가.

---

## 5) 대안 및 비교

* **DI 컨테이너**: 생성/주입/수명주기 자동화. 런타임 바인딩 용이.
* **서비스 로더(플러그인)**: 런타임 확장 포인트 제공(가시성↓, 디버깅 난이도↑).
* **new 직접 생성**: 간단하지만 결합도↑, 테스트 어려움.

---

## 6) HexaPass 적용 예시

### 6.1 Simple Factory — 할인 정책 선택

```java
public interface DiscountPolicy { Money apply(Money price); }

public final class DiscountPolicies {
    private DiscountPolicies() {}
    public static DiscountPolicy of(String code) {
        return switch (code) {
            case "RATE10" -> new RateDiscountPolicy(0.10);
            case "AMOUNT5000" -> new AmountDiscountPolicy(new Money(5000));
            default -> new NoDiscountPolicy();
        };
    }
}
```

* **장점**: 간단, 호출부는 `DiscountPolicies.of(code)`만 알면 됨.
* **주의**: 분기 증가 시 팩토리 수정 필요(OCP 부분 위배).

### 6.2 Factory Method — 리소스 타입별 검증기

```java
public interface ReservationValidator { void validate(Member m, Resource r, DateRange d); }

public abstract class ValidatorFactory {
    public final ReservationValidator create(Resource r) {
        ReservationValidator v = doCreate(r);
        return new LoggingValidator(v); // 공통 데코레이션
    }
    protected abstract ReservationValidator doCreate(Resource r);
}

public class DefaultValidatorFactory extends ValidatorFactory {
    @Override
    protected ReservationValidator doCreate(Resource r) {
        if (r.type() == ResourceType.ROOM) return new RoomValidator();
        if (r.type() == ResourceType.EQUIPMENT) return new EquipmentValidator();
        return new BasicValidator();
    }
}
```

* **장점**: 공통 전처리/후처리를 상위에서 강제, 생성 훅만 하위에서 구현.
* **주의**: 타입 추가 시 하위 팩토리 수정 필요.

### 6.3 Abstract Factory — 결제 어댑터 패밀리

```java
public interface PaymentPort { PaymentResult pay(Order order, Money amount); }
public interface RefundPort { RefundResult refund(Order order, Money amount); }

public interface PaymentAdapterFactory {
    PaymentPort payment();
    RefundPort refund();
}

public class KakaoPayAdapterFactory implements PaymentAdapterFactory {
    public PaymentPort payment() { return new KakaoPayPort(); }
    public RefundPort refund() { return new KakaoRefundPort(); }
}

public class TossPayAdapterFactory implements PaymentAdapterFactory {
    public PaymentPort payment() { return new TossPayPort(); }
    public RefundPort refund() { return new TossRefundPort(); }
}
```

* **장점**: 동일 벤더의 **일관된 객체군**을 세트로 교체 가능.
* **주의**: 벤더 수가 늘면 팩토리 클래스도 늘어남.

### 6.4 Builder — 예약 생성 과정 단계화

```java
public class ReservationBuilder {
    private Member member; private Resource resource; private DateRange dateRange; private DiscountPolicy discount;

    public ReservationBuilder member(Member m) { this.member = m; return this; }
    public ReservationBuilder resource(Resource r) { this.resource = r; return this; }
    public ReservationBuilder dateRange(DateRange d) { this.dateRange = d; return this; }
    public ReservationBuilder discount(DiscountPolicy p) { this.discount = p; return this; }

    public Reservation build() {
        Objects.requireNonNull(member); Objects.requireNonNull(resource); Objects.requireNonNull(dateRange);
        if (!resource.isAvailable(dateRange)) throw new IllegalArgumentException("불가 시간대");
        return new Reservation(ReservationId.generate(), member, resource, dateRange, Optional.ofNullable(discount));
    }
}
```

* **장점**: 필수/선택 파라미터 구분, 가독성↑, 불변 생성 보조.
* **주의**: 간단한 객체엔 과도한 도입.

---

## 7) 체크리스트

✅ 생성 이유(변경 이유)가 여러 곳에 흩어져 있지 않은가?
✅ 팩토리 없이 `new`가 남발되어 결합도가 높지 않은가?
✅ 테스트에서 대역(Stub/Mock)을 쉽게 주입할 수 있는가?
✅ 객체군 교체가 일관성 있게 이뤄지는가(Abstract Factory 고려)?

---

## 8) 학습 과제

1. `PricingPolicyFactory`를 작성해 멤버십 등급, 쿠폰, 기간에 따라 **복합 전략**을 반환하게 하라(전략+데코레이터 조합).
2. `ReservationValidatorFactory`에 새 리소스 타입을 추가하고, 상위 팩토리 수정 없이 확장하는 방법을 탐색해보라(등록형 팩토리/DI 활용).
3. `PaymentAdapterFactory`를 인메모리/샌드박스/실결제 3종으로 구현하고 계약 테스트를 작성하라.
4. 복잡한 예약 생성 시나리오를 **Builder**로 리팩토링하고, 불변식 검증을 빌더 내부로 이동시켜라.

---

📌 이 문서는 팩토리 계열(정적/메서드/추상/빌더)을 HexaPass 도메인에 대입해, 생성 책임 분리와 확장 가능 설계를 실습할 수 있도록 구성함.
