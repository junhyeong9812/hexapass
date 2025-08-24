# HexaPass-09-헥사고날 아키텍처 — 개념정리 (리팩토링 버전)

## 1) 정의

\*\*헥사고날 아키텍처(Hexagonal Architecture, 포트와 어댑터 아키텍처)\*\*는 애플리케이션의 핵심 도메인을 \*\*외부 세계(웹, DB, 메시징 등)\*\*로부터 분리하여, 도메인 로직이 외부 기술에 의존하지 않도록 하는 아키텍처 패턴이다.

* **포트(Port)**: 도메인과 외부 세계를 연결하는 인터페이스.
* **어댑터(Adapter)**: 포트를 실제 구현하는 외부 기술 계층.
* 핵심 도메인은 **외부 환경에 독립적**이며, 테스트와 유지보수가 쉬워진다.

---

## 2) 장점

1. **의존성 역전**: 도메인이 외부 기술에 의존하지 않음.
2. **유연한 교체 가능**: DB, UI, API 클라이언트 등 기술 교체가 용이.
3. **테스트 용이성**: 인메모리/Mock 어댑터로 빠른 단위 테스트 가능.
4. **유지보수성 향상**: 핵심 비즈니스 로직을 기술적 세부사항으로부터 보호.

---

## 3) 단점 / 주의점

* 초기 설계 복잡도 증가 → 작은 앱에는 과설계 위험.
* 포트/어댑터가 많아지면 클래스 관리 비용 상승.
* 잘못 적용 시 도메인과 인프라가 여전히 뒤엉킬 수 있음.

---

## 4) 대안 및 비교

* **Layered Architecture (계층형)**: 전통적 3계층(Controller-Service-Repository). 단순하지만 의존 방향이 인프라 → 도메인으로 향해 결합도↑.
* **Clean Architecture**: 헥사고날의 확장/일반화. 동심원 구조로 도메인 보호.
* **Onion Architecture**: 유사 개념, 의존성 규칙 동일.

➡️ 세 접근 모두 핵심은 **도메인 독립성** 보장.

---

## 5) HexaPass 적용 예시

### 5.1 포트 정의

```java
// 아웃바운드 포트 (DB 저장소)
public interface ReservationRepositoryPort {
    void save(Reservation reservation);
    Optional<Reservation> findById(ReservationId id);
}

// 아웃바운드 포트 (결제)
public interface PaymentPort {
    PaymentResult pay(Order order, Money amount);
}
```

### 5.2 도메인 서비스 (포트 사용)

```java
public class ReservationService {
    private final ReservationRepositoryPort repository;
    private final PaymentPort paymentPort;

    public ReservationService(ReservationRepositoryPort repository, PaymentPort paymentPort) {
        this.repository = repository;
        this.paymentPort = paymentPort;
    }

    public Reservation reserve(Member member, Resource resource, DateRange range) {
        Reservation reservation = Reservation.create(member, resource, range);
        repository.save(reservation);
        paymentPort.pay(reservation.getOrder(), reservation.totalPrice());
        return reservation;
    }
}
```

➡️ 도메인은 `PaymentPort`라는 **추상 인터페이스**만 의존한다.

### 5.3 어댑터 구현체

```java
// DB 어댑터 (JPA)
@Repository
public class JpaReservationRepositoryAdapter implements ReservationRepositoryPort {
    private final SpringDataReservationRepository repo;
    public void save(Reservation reservation) { repo.save(reservation); }
    public Optional<Reservation> findById(ReservationId id) { return repo.findById(id); }
}

// 결제 어댑터 (외부 API)
public class KakaoPaymentAdapter implements PaymentPort {
    @Override
    public PaymentResult pay(Order order, Money amount) {
        // 카카오페이 API 호출
        return new PaymentResult(true, "성공");
    }
}
```

➡️ 기술 변경 시 어댑터만 교체하면 됨.

---

## 6) 체크리스트

✅ 도메인 서비스가 외부 기술에 직접 의존하지 않고, **포트 인터페이스**에만 의존하는가?
✅ 어댑터 교체 시 도메인 로직 수정이 필요 없는가?
✅ 포트 인터페이스가 도메인 언어로 작성되어 있는가? (`ReservationRepositoryPort` vs `JpaRepository`)

---

## 7) 학습 과제

1. `NotificationPort`를 정의하고, 이메일/SMS 알림 어댑터를 구현해보라.
2. 테스트용 `InMemoryReservationRepositoryAdapter`를 작성하여 단위 테스트에서 활용해보라.
3. `PaymentPort`에 여러 구현체를 두고, DI 컨테이너(Spring)로 런타임 교체해보라.
4. 기존 Layered 아키텍처 코드와 비교하여, 헥사고날 구조가 주는 장점을 ADR에 기록하라.

---

📌 이 문서는 헥사고날 아키텍처를 HexaPass의 **예약/결제 모듈**에 적용해, 도메인 독립성과 테스트 용이성을 실습할 수 있도록 구성됨.
