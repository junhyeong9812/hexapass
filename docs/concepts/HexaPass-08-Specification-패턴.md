# HexaPass-08-Specification 패턴 — 개념정리 (리팩토링 버전)

## 1) 정의

**사양(Specification) 패턴**은 비즈니스 규칙(조건)을 **명세 객체**로 캡슐화하고, 이 명세들을 `and / or / not`과 같은 **논리 연산으로 합성**해 복잡한 조건을 표현하는 패턴이다.

* 읽기 쉬운 도메인 언어로 조건을 모델링하고, 재사용/조합/테스트를 쉽게 한다.
* 인메모리 필터링(도메인 객체 평가)과 영속 계층(예: JPA Criteria)의 질의로 **양방향 매핑**을 지원할 수 있다.

---

## 2) 핵심 요소

* **Specification 인터페이스**: `isSatisfiedBy(T candidate)` (도메인에서 평가) + 선택적으로 `toPredicate()` (영속 계층 매핑)를 정의.
* **Concrete Specification**: 단일 규칙을 표현.
* **Composite Specification**: `AndSpecification`, `OrSpecification`, `NotSpecification` 등으로 합성 제공.
* **Translator/Adapter**: 도메인 사양 ↔ 영속 사양(JPA Criteria 등) 변환기.

---

## 3) 장점

1. **재사용/조합 가능**: 작은 규칙을 합성해 복잡한 규칙을 구성.
2. **가독성↑**: 도메인 언어로 조건을 드러내 요구사항 파악 용이.
3. **테스트 용이**: 규칙 단위로 단위 테스트 가능.
4. **관심사 분리**: 로직(조건)과 데이터 접근(쿼리)을 분리.

---

## 4) 단점 / 주의점

* 사양 클래스가 많아질 수 있어 **클래스 폭증** 위험.
* 영속 계층과의 매핑(예: JPA Predicate)에서 **표현 한계**나 변환 비용 발생.
* 지나치게 일반화하면 **과도한 추상화**로 복잡성↑.

---

## 5) 대안 및 비교

* **Query Object**: 질의를 객체로 캡슐화(읽기 집중형에서 유용).

    * 장점: 질의 중심, 페이징/정렬 등 인프라 기능과 자연스러운 결합.
    * 단점: 도메인 규칙 언어화는 상대적으로 약함.
* **Predicate 함수(람다)**: 간단한 케이스에 적합.

    * 장점: 코드량 적고 빠름.
    * 단점: 의미 부여/조합/테스트 관점에서 한계.
* **룰 엔진**(Drools 등): 선언적 규칙, 비개발자 참여 용이.

    * 장점: 복잡 규칙/변경 잦은 도메인에 적합.
    * 단점: 러닝커브/운영 복잡도/디버깅.

---

## 6) HexaPass 도메인 예시

### 6.1 사양 인터페이스

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);

    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }
}
```

### 6.2 예약 가능 사양들

```java
public class WithinScheduleSpec implements Specification<ReservationRequest> {
    @Override
    public boolean isSatisfiedBy(ReservationRequest req) {
        return req.resource().isAvailable(req.dateRange());
    }
}

public class NotOverlappingSpec implements Specification<ReservationRequest> {
    private final ReservationRepository repo;
    public NotOverlappingSpec(ReservationRepository repo) { this.repo = repo; }
    @Override
    public boolean isSatisfiedBy(ReservationRequest req) {
        return repo.findOverlaps(req.resource().id(), req.dateRange()).isEmpty();
    }
}

public class UnderMemberLimitSpec implements Specification<ReservationRequest> {
    @Override
    public boolean isSatisfiedBy(ReservationRequest req) {
        return req.member().activeReservationCount() < req.member().plan().maxConcurrentReservations();
    }
}
```

### 6.3 합성으로 최종 규칙 구성

```java
Specification<ReservationRequest> reservationAllowed =
        new WithinScheduleSpec()
            .and(new NotOverlappingSpec(repo))
            .and(new UnderMemberLimitSpec());

if (!reservationAllowed.isSatisfiedBy(req)) {
    throw new IllegalArgumentException("예약 조건 불충족");
}
```

### 6.4 영속 계층 매핑(JPA Criteria 예시)

사양 자체를 JPA `Specification`(Spring Data JPA)로도 만들 수 있다. 아래는 변환기 예시:

```java
public interface JpaConvertible<T> {
    javax.persistence.criteria.Predicate toPredicate(
        T root, javax.persistence.criteria.CriteriaQuery<?> query,
        javax.persistence.criteria.CriteriaBuilder cb);
}

public interface ReservationSpec extends Specification<Reservation>, JpaConvertible<Root<Reservation>> { }

public class OverlapJpaSpec implements ReservationSpec {
    private final ResourceId resourceId; private final DateRange range;
    public OverlapJpaSpec(ResourceId id, DateRange r) { this.resourceId = id; this.range = r; }

    @Override
    public boolean isSatisfiedBy(Reservation r) {
        return !r.overlaps(range) && r.resourceId().equals(resourceId);
    }

    @Override
    public Predicate toPredicate(Root<Reservation> root, CriteriaQuery<?> q, CriteriaBuilder cb) {
        Path<Instant> start = root.get("start");
        Path<Instant> end = root.get("end");
        Path<String> res = root.get("resourceId");
        Predicate sameRes = cb.equal(res, resourceId.value());
        Predicate noOverlap = cb.or(
            cb.lessThanOrEqualTo(end, range.start()),
            cb.greaterThanOrEqualTo(start, range.end())
        );
        return cb.and(sameRes, noOverlap);
    }
}
```

> 실무 팁: 도메인 사양과 JPA 사양을 **같은 인터페이스로 통합**하려 하면 복잡해질 수 있다. 보통은 **도메인 사양**과 **영속 사양**을 분리하고, 필요한 경우 어댑터/번역기를 둔다.

---

## 7) 테스트 전략

* **단위 테스트**: 각 사양의 `isSatisfiedBy`를 다양한 경계값/예외 시나리오로 검증.
* **계약 테스트**: 인메모리 평가 결과와 DB 질의 결과가 **동일**함을 보장(샘플 데이터셋 고정).
* **조합 테스트**: `and/or/not` 합성 결과가 의도대로 동작하는지 확인.

---

## 8) 체크리스트

✅ 사양 이름이 **도메인 언어**를 반영하는가? (`WithinSchedule`, `NotOverlapping` 등)
✅ 단일 사양은 하나의 규칙만 표현하는가?
✅ 합성을 통해 복잡도를 제어하고 있는가(거대 사양 금지)?
✅ 인메모리 평가와 영속 계층 질의가 **동일 의미**를 보장하는가?
✅ 사양 남용으로 클래스가 폭증하지 않는가(폴더/네이밍/팩토리 정비)?

---

## 9) 학습 과제

1. 멤버십 적용 가능 사양을 설계하라: `PlanActive AND TierEligible AND PaymentUpToDate`.
2. `BlackoutPeriodSpec`을 추가하고, 휴무 기간에는 예약이 불가능하도록 전체 사양에 결합해라.
3. 인메모리 사양과 JPA 사양을 각각 구현하고, **동일 데이터셋**에서 두 결과가 동일함을 보이는 계약 테스트를 작성하라.
4. 스펙 조합을 **데코레이터** 또는 **전략**과 비교해 보고, 적절한 경계를 문서(ADR)로 남겨라.

---

📌 이 문서는 사양 패턴을 HexaPass의 **예약/멤버십 규칙**에 적용해, 도메인 규칙의 표현력과 테스트 가능성을 높이는 실습형 자료다.
