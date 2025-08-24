# HexaPass-11-TDD & 테스트 — 개념정리 (리팩토링 버전)

## 1) 정의

**TDD(Test-Driven Development)** 는 Red(실패) → Green(통과) → Refactor(리팩토링)의 짧은 사이클로 설계를 이끌어내는 개발 기법이다. 테스트는 **설계 제약**이자 **명세**이며, 회귀 방지망이다.

* **테스트 피라미드**: Unit(많이) > Integration(일부) > E2E/UI(소수)
* **테스트 더블**: Dummy, Stub, Mock, Spy, Fake
* **행위 주도 개발(BDD)**: 도메인 시나리오(유비쿼터스 언어)로 테스트를 표현

---

## 2) 장점

1. 빠른 피드백으로 설계 개선, 과설계 예방.
2. 회귀 방지, 리팩토링 안전망 제공.
3. 명세로서의 테스트 → 문서 대체(살아있는 문서).

---

## 3) 단점 / 주의점

* 초기 속도 저하 체감, 러닝커브.
* **과도한 모킹**은 리팩토링 저항과 취약 테스트를 유발.
* UI/E2E에 과도하게 의존하면 느리고 불안정한 테스트 스위트가 됨.

---

## 4) 대안/보완 기법

* **BDD**(Cucumber/JGiven): 시나리오-중심 명세화.
* **Property-based**(jqwik/QuickTheories): 성질을 자동 탐색.
* **계약 테스트**(Pact): 서비스 간/포트-어댑터 경계의 호환성 보장.
* **스냅샷/골든 마스터**: 레거시/출력 고정 검증에 유용.

---

## 5) HexaPass 테스트 설계 지침

* 도메인 불변식/사양/상태 전이 → **단위 테스트 우선**.
* 유스케이스/서비스 → **포트 모킹**으로 슬라이스 테스트.
* 어댑터/인프라 → **계약 테스트** + 소규모 통합 테스트.
* 경쟁 상황/트랜잭션 → **동시성 테스트**(스레드/스트레스) 별도 운영.

---

## 6) 예시: 단위 테스트 (JUnit, Given-When-Then)

```java
class DateRangeTest {
    @Test
    void 겹치는_구간이면_overlap_true() {
        // Given
        DateRange a = DateRange.of("2025-08-24T10:00", "2025-08-24T12:00");
        DateRange b = DateRange.of("2025-08-24T11:00", "2025-08-24T13:00");
        // When
        boolean result = a.overlaps(b);
        // Then
        assertTrue(result);
    }
}
```

### 예시: 도메인 서비스 슬라이스 테스트 (포트 모킹)

```java
class ReservationServiceTest {
    ReservationRepositoryPort repo = mock(ReservationRepositoryPort.class);
    PaymentPort payment = mock(PaymentPort.class);
    ReservationService service = new ReservationService(repo, payment);

    @Test
    void 예약_생성시_결제호출과_저장이_일어난다() {
        // Given
        when(payment.pay(any(), any())).thenReturn(PaymentResult.success());
        Member m = Fixtures.member(); Resource r = Fixtures.room(); DateRange dr = Fixtures.tomorrow10to11();
        // When
        Reservation res = service.reserve(m, r, dr);
        // Then
        verify(repo).save(res);
        verify(payment).pay(res.getOrder(), res.totalPrice());
    }
}
```

### 예시: Property-based 테스트 (jqwik)

```java
@Property
void 두_구간의_겹침은_대칭이다(@ForAll DateRange a, @ForAll DateRange b) {
    assumeTrue(a.isValid() && b.isValid());
    assertThat(a.overlaps(b)).isEqualTo(b.overlaps(a));
}
```

### 예시: Cucumber BDD (개요)

```
Feature: 예약 상태 전이
  Scenario: 확정된 예약은 취소할 수 없다
    Given 확정된 예약이 있다
    When 사용자가 예약을 취소하면
    Then "이미 확정된 예약은 취소 불가" 예외가 발생한다
```

---

## 7) 계약 테스트 (포트/어댑터)

예: `PaymentPort` 계약을 테스트 더블과 실제 어댑터에 동일하게 적용.

```java
interface PaymentContract {
    PaymentPort port();

    @Test
    default void 멱등키가_같으면_중복결제되지_않는다() {
        Order o = Fixtures.order(); Money amount = o.totalPrice();
        String key = o.getId().value();
        PaymentResult r1 = port().pay(o.getId(), amount, key);
        PaymentResult r2 = port().pay(o.getId(), amount, key);
        assertEquals(r1, r2);
    }
}

class KakaoPaymentAdapterTest implements PaymentContract {
    public PaymentPort port() { return new KakaoPaymentAdapter(...); }
}
```

---

## 8) 테스트 데이터와 픽스처

* **Object Mother / Test Data Builder**: 가독성 있는 픽스처 생성.
* **고정 시계(Clock)**: 시간 의존 로직을 제어.
* **Testcontainers**: 실제 DB/브로커와의 통합 테스트에 사용.

```java
public class MemberBuilder {
    private String id = UUID.randomUUID().toString();
    private MembershipPlan plan = MembershipPlan.monthly();
    public MemberBuilder id(String id){ this.id = id; return this; }
    public MemberBuilder plan(MembershipPlan p){ this.plan = p; return this; }
    public Member build(){ return new Member(id, plan); }
}
```

---

## 9) 안티패턴 체크

* 비즈니스 규칙이 **서비스 모킹 네트워크**에 묻혀 보이지 않음.
* E2E 테스트만 잔뜩, 단위/슬라이스 부족.
* 테스트가 구현 세부에 과도 결합(리팩토링에 취약).

---

## 10) CI 파이프라인 팁

* **빠른 단위 테스트** 우선 실행 → 실패 시 즉시 피드백.
* 커버리지 임계값(라인/분기)보다 **중요 로직 커버**를 지표로 관리.
* 병렬화와 캐시를 활용해 빌드 시간 단축.

---

## 11) 체크리스트

✅ 테스트 이름이 도메인 언어를 표현하는가?
✅ 실패하는 테스트로 요구를 먼저 드러냈는가(RED)?
✅ 외부 의존은 포트로 모킹/더블링 했는가?
✅ 중요한 불변식/전이를 단위 테스트로 커버했는가?
✅ 통합/E2E는 핵심 경로에 한정했는가?

---

## 12) 학습 과제

1. `DateRange`에 대해 겹침 연산의 교환법칙/결합법칙을 Property-based로 검증.
2. `ReservationService`에 경쟁 상황을 모사하는 동시성 테스트 추가.
3. `PaymentPort` 계약 테스트를 작성해 인메모리/실어댑터 모두 통과시키기.
4. Cucumber 시나리오를 기반으로 상태 전이 BDD 테스트를 작성하고 리그레션 방지망으로 사용.

---

📌 이 문서는 HexaPass의 **도메인 불변식/상태 전이/포트 계약**을 중심으로 테스트 전략을 실습하는 학습형 자료다.
