# HexaPass-05-State 패턴 — 개념정리 (리팩토링 버전)

## 1) 정의

**상태(State) 패턴**은 객체의 내부 상태를 객체로 분리하여, 상태에 따라 다른 동작을 수행하도록 하는 패턴이다. 즉, 조건 분기문으로 상태를 구분하지 않고, 상태 객체 자체가 행위를 정의한다.

---

## 2) 핵심 요소

* **State 인터페이스**: 상태에서 수행할 공통 동작 정의.
* **Concrete State**: 실제 상태별 동작 구현.
* **Context**: 현재 상태를 가지고 있으며, 상태 전이를 관리.

---

## 3) 장점

1. 조건문 제거 → 가독성 및 유지보수성 향상.
2. 새로운 상태 추가가 용이(OCP 충족).
3. 상태 전이 규칙이 명시적으로 드러남.

---

## 4) 단점 / 주의점

* 상태 클래스 수 증가 → 관리 복잡성 증가.
* 상태 전이 다이어그램 설계가 필요.
* 단순 상태에서는 오히려 if-else가 더 간단할 수 있음.

---

## 5) 대안 및 비교

* **전략 패턴**: 교체 가능한 알고리즘에 초점, 상태 패턴은 시간 경과/조건 변화에 따른 상태 전이에 초점.
* **조건 분기(if/else)**: 상태가 적으면 단순.
* **상태 머신/스테이트차트**: 복잡 전이에 강력하지만 러닝커브가 있음.

---

## 6) HexaPass 적용 예시

### 상태 인터페이스

```java
public interface ReservationState {
    void cancel(Reservation reservation);
    void checkIn(Reservation reservation);
}
```

### 상태 구현체들

```java
public class RequestedState implements ReservationState {
    @Override
    public void cancel(Reservation reservation) {
        reservation.setState(new CanceledState());
    }

    @Override
    public void checkIn(Reservation reservation) {
        reservation.setState(new ConfirmedState());
    }
}

public class ConfirmedState implements ReservationState {
    @Override
    public void cancel(Reservation reservation) {
        throw new IllegalStateException("이미 확정된 예약은 취소 불가");
    }

    @Override
    public void checkIn(Reservation reservation) {
        reservation.setState(new CheckedInState());
    }
}

public class CanceledState implements ReservationState {
    @Override
    public void cancel(Reservation reservation) {
        throw new IllegalStateException("이미 취소된 예약입니다");
    }

    @Override
    public void checkIn(Reservation reservation) {
        throw new IllegalStateException("취소된 예약은 체크인 불가");
    }
}
```

### Context (Reservation)

```java
public class Reservation {
    private ReservationState state;

    public Reservation() {
        this.state = new RequestedState();
    }

    public void cancel() {
        state.cancel(this);
    }

    public void checkIn() {
        state.checkIn(this);
    }

    public void setState(ReservationState newState) {
        this.state = newState;
    }
}
```

➡️ 상태에 따라 `cancel()`이나 `checkIn()` 동작이 달라진다.

---

## 7) 체크리스트

✅ 상태 전이는 명확히 정의되어 있는가?
✅ 상태 객체는 불필요한 책임을 가지지 않는가?
✅ 잘못된 상태 전이 시 명시적으로 예외를 발생시키는가?

---

## 8) 학습 과제

1. `CompletedState`를 추가하고, 완료된 예약은 취소 불가능하도록 구현하기.
2. `CanceledState`에서 재예약 플로우를 허용하도록 새로운 전이 규칙 추가하기.
3. 상태 전이 다이어그램을 직접 그려보고, 코드와 일치하는지 검증하기.
4. JUnit으로 각 상태에서 가능한/불가능한 행위를 테스트하기.

---

📌 이 문서는 상태 패턴을 HexaPass의 **예약 상태 관리** 시나리오로 구체화하여 학습할 수 있도록 구성됨.
