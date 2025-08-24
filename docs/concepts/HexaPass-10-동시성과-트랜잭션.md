# HexaPass-10-동시성과 트랜잭션 — 개념정리 (리팩토링 버전)

## 1) 정의 & 문제 배경

동시성은 여러 실행 흐름(스레드/프로세스/노드)이 **같은 자원**을 동시에 다루는 상황을 말한다. 이때 **경쟁 조건(Race Condition)**, **잃어버린 업데이트(Lost Update)**, **더티/논리적 읽기**, **팬텀 리드(Phantom Read)** 등이 발생할 수 있다. 트랜잭션은 이러한 상황에서 **원자성/일관성/격리성/지속성(ACID)** 을 보장하기 위한 단위 작업이며, 격리 수준에 따라 동시성과 무결성의 균형을 조절한다.

* **임계 구역(Critical Section)**: 동시에 접근 시 순서를 직렬화해야 하는 코드/데이터 영역
* **락킹(Locking)**: 비관적(선점) 락 vs 낙관적(버전)
* **격리 수준**: Read Uncommitted/Committed, Repeatable Read, Serializable
* **멱등성(Idempotency)**: 같은 요청이 여러 번 와도 결과가 변하지 않도록 보장(재시도/중복 방지)
* **일관성 경계**: DDD에서 보통 **애그리게잇 단위**로 강한 일관성을 유지

---

## 2) 왜 중요한가 (장점)

* **데이터 무결성**과 **도메인 불변식** 보장
* 결제/예약 등 **비즈니스 크리티컬** 경로에서 신뢰성 확보
* 장애/네트워크 이슈에도 **안전한 재시도** 가능 (멱등 키)

---

## 3) 비용/주의점 (단점)

* 락 경합으로 **대기/지연/스루풋 저하**
* **교착 상태(Deadlock)** 위험, 타임아웃/재시도 필요
* 분산 트랜잭션(2PC) 등은 **운영 복잡도↑**, 대안(SAGA) 고려

---

## 4) 주요 전략 & 비교

### 4.1 비관적 락(Pessimistic Lock)

* **개념**: 갱신 전 잠금 획득(예: `SELECT ... FOR UPDATE`), 다른 트랜잭션은 대기
* **장점**: 충돌이 잦을 때 실패/재시도 비용↓, 직관적
* **단점**: 락 경합/교착 가능, 지연↑

### 4.2 낙관적 락(Optimistic Lock)

* **개념**: 버전 필드로 충돌 감지, 커밋 시 버전 비교 후 실패 시 재시도
* **장점**: 충돌이 드문 시스템에서 고성능, 락 경합 없음
* **단점**: 충돌 시 재시도 비용, 재시도 정책 필요

### 4.3 고유 제약(Unique Constraint)에 의한 가드

* **개념**: DB 유니크 인덱스로 **논리적 중복**을 하드가드
* **장점**: 단순/강력, 애플리케이션 버그 방지망
* **단점**: 예외 기반 흐름 처리 필요(롤백/재시도 설계)

### 4.4 큐 기반 직렬화(Per-Key Serialization)

* **개념**: `resourceId + timeSlot` 단위 큐/싱글 워커로 순차 처리
* **장점**: 구현 단순, 경합 제거, 예측 가능한 순서
* **단점**: 큐 지연, 파티셔닝/스케일링 설계 필요

### 4.5 분산 일관성 패턴

* **SAGA(보상 트랜잭션)**: 장기간/분산 워크플로를 **보상 단계**로 롤백
* **Outbox/Inbox 패턴**: 로컬 트랜잭션 + 메시지 **원자적 발행** 보장
* **이벤트 소싱**: 상태 대신 이벤트 스트림 저장(복잡도↑)

### 4.6 CQRS/읽기-쓰기 분리

* **개념**: 쓰기 모델은 강한 일관성, 읽기 모델은 조회 최적화/캐시/최종 일관성
* **장점**: 경합 완화, 확장성↑
* **단점**: 동기화/복잡성↑

---

## 5) HexaPass 시나리오 설계

### 5.1 동일 리소스+시간대 예약 경쟁

* **불변식**: `(resourceId, timeRange)`는 **겹치지 않게** 하나의 예약만 허용
* **전략 A (비관적)**: 스케줄/재고 레코드를 `FOR UPDATE`로 잠그고 가용성 감소 후 커밋
* **전략 B (낙관적)**: `Availability` 애그리게잇에 `version` 필드를 두고 저장 시 버전 체크
* **전략 C (유니크 가드)**: DB에 `(resource_id, start, end)` 유니크 인덱스 → 충돌 시 예외 후 재시도/오류
* **전략 D (큐 직렬화)**: `partitionKey = resourceId`로 단일 워커가 순차 예약 처리

### 5.2 결제 멱등성

* **원칙**: `idempotencyKey = orderId`로 동일 요청 중복 처리 방지
* **재시도 정책**: 네트워크 오류 시 지수 백오프 + 멱등키로 재호출 안전화

### 5.3 취소 수수료 & 상태 전이

* **상태 패턴**과 결합: `REQUESTED → CONFIRMED → CHECKED_IN → COMPLETED/CANCELED`
* 상태 전이 + 금액 정산(결제/환불) **원자적** 반영 필요(동일 트랜잭션 또는 SAGA)

---

## 6) 코드 예시

### 6.1 JPA 낙관적 락(@Version)

```java
@Entity
public class Availability {
    @Id Long id;
    @Version Long version; // 낙관적 락 버전

    @Embedded DateRange slot;
    private int capacity;

    public void reserve(int qty) {
        if (capacity < qty) throw new IllegalStateException("재고 부족");
        capacity -= qty;
    }
}
```

### 6.2 비관적 락 Repository

```java
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Availability a where a.id = :id")
    Availability lockById(@Param("id") Long id);
}
```

### 6.3 유니크 인덱스 가드 (DDL)

```sql
CREATE UNIQUE INDEX ux_reservation_unique_slot
ON reservation(resource_id, start_at, end_at);
```

### 6.4 멱등 결제 포트

```java
public interface PaymentPort {
    PaymentResult pay(OrderId orderId, Money amount, String idempotencyKey);
}

public class PaymentService {
    private final PaymentPort port;
    public PaymentService(PaymentPort port) { this.port = port; }

    public PaymentResult capture(Order order) {
        String key = order.getId().value();
        return port.pay(order.getId(), order.totalPrice(), key);
    }
}
```

### 6.5 재시도 정책(낙관적 락 실패 시)

```java
public <T> T withRetry(Supplier<T> op, int maxAttempts) {
    int attempt = 0;
    while (true) {
        try { return op.get(); }
        catch (OptimisticLockException e) {
            if (++attempt >= maxAttempts) throw e;
            try { Thread.sleep((long) Math.pow(2, attempt) * 10); } catch (InterruptedException ignored) {}
        }
    }
}
```

### 6.6 동시성 테스트(간단 스케치)

```java
ExecutorService pool = Executors.newFixedThreadPool(32);
var tasks = IntStream.range(0, 100)
    .mapToObj(i -> (Callable<Boolean>) () -> service.reserve(resourceId, slot))
    .toList();
var results = pool.invokeAll(tasks);
long success = results.stream().filter(f -> {
    try { return f.get(); } catch (Exception e) { return false; }
}).count();
System.out.println("성공 건수=" + success);
```

---

## 7) 체크리스트

✅ 애그리게잇 단위의 **트랜잭션 경계**가 명확한가?
✅ 충돌 패턴(자주/드물다)에 따라 **비관적 vs 낙관적** 선택이 합리적인가?
✅ **유니크 인덱스** 등 DB 제약으로 최종 방어선이 구축되어 있는가?
✅ **재시도/타임아웃/백오프** 정책이 정의되어 있는가?
✅ 분산 시나리오에서 **SAGA/Outbox** 등으로 일관성 경로가 보장되는가?
✅ 결제/예약 API는 **멱등 키**를 사용하고 있는가?

---

## 8) 학습 과제

1. 100명 동시 예약 시나리오를 준비하고, **비관적/낙관적/유니크 가드/큐 직렬화** 4전략의 성공률·지연·스루풋을 측정 비교하라.
2. `@Version` 충돌 시 재시도 유틸리티를 만들고, 지수 백오프 파라미터(최대 대기/시도 수)를 설정 가능하게 하라.
3. 결제/환불을 포함한 **상태 전이 + SAGA** 시나리오를 구현하고, 장애 유발 테스트(네트워크/DB 예외)로 보상 로직을 검증하라.
4. 읽기-쓰기 분리(CQRS)로 예약 현황 대시보드를 구현하고, **최종 일관성 지연**에 대한 UX/문서화를 준비하라.

---

📌 이 문서는 HexaPass의 **예약 경쟁/결제/상태 전이**를 중심으로 동시성과 트랜잭션 설계를 학습·실습할 수 있도록 구성했다.
