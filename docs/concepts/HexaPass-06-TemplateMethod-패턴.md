# HexaPass-06-Template Method 패턴 — 개념정리 (리팩토링 버전)

## 1) 정의

**템플릿 메서드(Template Method) 패턴**은 알고리즘의 공통 골격을 상위 클래스에 정의하고, 일부 세부 단계(훅 메서드)를 하위 클래스에서 재정의할 수 있도록 하는 패턴이다. 즉, **변하지 않는 절차는 상위 클래스에 두고, 변할 수 있는 부분은 하위 클래스에서 구현**한다.

---

## 2) 핵심 요소

* **추상 클래스(Abstract Class)**: 템플릿 메서드와 공통 로직을 정의.
* **템플릿 메서드(Template Method)**: 알고리즘의 골격을 정의하는 메서드.
* **훅 메서드(Hook Method)**: 하위 클래스가 선택적으로 구현할 수 있는 메서드.
* **구체 하위 클래스(Concrete Class)**: 템플릿 메서드의 일부 단계를 재정의.

---

## 3) 장점

1. 알고리즘의 공통 부분을 상위 클래스에 모아 중복 제거.
2. 절차의 일관성 보장.
3. 변하는 부분만 하위 클래스에 두어 확장 용이.

---

## 4) 단점 / 주의점

* 상속 기반이므로 런타임에 행위를 교체하기 어렵다.
* 하위 클래스 수가 많아질 수 있다.
* 리스코프 치환 원칙 위반 위험(부적절한 재정의 시).

---

## 5) 대안 및 비교

* **전략 패턴**: 합성 기반, 런타임 교체 가능.
* **일반 상속**: 중복 제거 가능하지만 변하지 않는 절차를 강제하기 어려움.

---

## 6) HexaPass 적용 예시

### 결제 처리 템플릿

```java
public abstract class PaymentFlowTemplate {
    // 템플릿 메서드
    public final void processPayment(Order order, Money amount) {
        authorize(order, amount);
        capture(order, amount);
        sendReceipt(order);
    }

    protected abstract void authorize(Order order, Money amount);
    protected abstract void capture(Order order, Money amount);

    // 공통 단계 (재정의 불가)
    private void sendReceipt(Order order) {
        System.out.println("영수증 발송: " + order.getId());
    }
}
```

### 카드 결제 구현체

```java
public class CardPaymentFlow extends PaymentFlowTemplate {
    @Override
    protected void authorize(Order order, Money amount) {
        System.out.println("카드 승인 완료");
    }

    @Override
    protected void capture(Order order, Money amount) {
        System.out.println("카드 결제 완료");
    }
}
```

### 포인트 결제 구현체

```java
public class PointPaymentFlow extends PaymentFlowTemplate {
    @Override
    protected void authorize(Order order, Money amount) {
        System.out.println("포인트 차감 가능 확인");
    }

    @Override
    protected void capture(Order order, Money amount) {
        System.out.println("포인트 차감 완료");
    }
}
```

### 실행 예시

```java
PaymentFlowTemplate payment = new CardPaymentFlow();
payment.processPayment(order, new Money(50000));
```

➡️ `processPayment` 골격은 동일, 결제 방식만 달라진다.

---

## 7) 체크리스트

✅ 알고리즘의 공통 골격을 상위 클래스에 두었는가?
✅ 변하는 부분만 하위 클래스에서 정의했는가?
✅ 하위 클래스가 공통 절차를 깨지 않도록 강제했는가?

---

## 8) 학습 과제

1. `RefundFlowTemplate`을 작성하고 카드/포인트 환불 구현체 만들기.
2. `ReservationFlowTemplate` 작성 → 공통 절차(검증, 저장, 알림)와 훅 메서드(리소스 타입별 검증)를 나누기.
3. 전략 패턴으로 동일 기능을 구현해보고 차이를 비교하기.
4. 상속 구조가 깊어졌을 때의 단점(확장 어려움)을 직접 경험하기.

---

📌 이 문서는 템플릿 메서드 패턴을 HexaPass의 **결제 흐름/예약 절차**에 적용해 학습할 수 있도록 구성됨.
