# HexaPass-01-DDD-개념정리 (리팩토링 버전)

## 1) 정의

\*\*도메인 주도 설계(DDD, Domain-Driven Design)\*\*는 비즈니스 규칙을 소프트웨어 모델에 직접 반영하는 접근 방식이다. 핵심은 **유비쿼터스 언어**를 통해 비즈니스 전문가와 개발자가 공통된 언어를 사용하며, 도메인 로직을 엔티티, 값 객체, 애그리게잇 등으로 모델링하는 것이다.

---

## 2) 핵심 요소

* **엔티티(Entity)**: 고유 식별자를 가지며 상태가 변하는 객체. 예: `Member`, `Reservation`.
* **값 객체(Value Object)**: 식별자가 없고 값으로 동일성을 판별하는 불변 객체. 예: `Money`, `DateRange`.
* **애그리게잇(Aggregate)**: 일관성 경계를 가진 객체 묶음. 루트 엔티티를 통해서만 접근.
* **도메인 서비스**: 특정 엔티티에 속하지 않는 도메인 규칙을 표현.
* **불변식(Invariant)**: 항상 참이어야 하는 규칙. 예: 예약은 리소스의 가능한 시간대 안에서만 가능.
* **유비쿼터스 언어**: 도메인 전문가와 개발자가 공유하는 언어를 코드/문서/대화에서 일관되게 사용.

---

## 3) 장점

1. 요구사항과 코드 간 괴리 최소화 → 비즈니스 중심 설계.
2. 도메인 규칙을 응집시켜 유지보수 용이.
3. 애그리게잇 단위로 트랜잭션을 관리할 수 있어 일관성 보장.
4. 유비쿼터스 언어로 팀 내 소통 비용 절감.

---

## 4) 단점 / 주의점

* 러닝 커브가 높아 초기 도입 비용이 큼.
* 경계 설정을 잘못하면 과도한 복잡성 유발.
* 단순 CRUD 앱에는 과설계가 될 수 있음.

---

## 5) 대안 및 비교

* **Transaction Script**: 절차적으로 각 유스케이스를 함수로 구현.

    * 장점: 단순, 빠른 구현.
    * 단점: 규칙 중복, 유지보수 어려움.
* **Active Record**: 엔티티가 DB 연산(`save`, `update`)까지 담당.

    * 장점: 직관적, 생산성 높음.
    * 단점: 도메인 규칙이 복잡해지면 모델 비대화.
* **Anemic Domain Model**: 데이터와 로직 분리.

    * 장점: 단순, 온보딩 쉬움.
    * 단점: 캡슐화 상실, 불변식 보장 어려움.

---

## 6) HexaPass 적용 예시

### Reservation 애그리게잇

```java
public class Reservation {
    private final ReservationId id;
    private final Member member;
    private final Resource resource;
    private final DateRange dateRange;

    private Reservation(Member member, Resource resource, DateRange dateRange) {
        if (!resource.isAvailable(dateRange)) {
            throw new IllegalArgumentException("해당 시간 예약 불가");
        }
        this.id = ReservationId.generate();
        this.member = member;
        this.resource = resource;
        this.dateRange = dateRange;
    }

    public static Reservation create(Member member, Resource resource, DateRange dateRange) {
        return new Reservation(member, resource, dateRange);
    }
}
```

➡️ 불변식: `dateRange`는 반드시 `resource.schedule` 범위 안에 포함.

### Membership 갱신 로직

```java
public class Membership {
    private final Period period;
    private final Member member;

    public Membership(Member member, Period period) {
        if (period.isExpired()) {
            throw new IllegalArgumentException("만료된 기간은 등록 불가");
        }
        this.member = member;
        this.period = period;
    }

    public Membership extend(Period extension) {
        if (!this.period.isAdjacent(extension)) {
            throw new IllegalArgumentException("연속되지 않는 기간은 연장 불가");
        }
        return new Membership(this.member, this.period.merge(extension));
    }
}
```

➡️ 불변식: 새 기간은 기존 종료일 직후에만 시작 가능.

---

## 7) 체크리스트 & 학습 과제

✅ 체크리스트

* [ ] 코드 용어와 `docs/glossary.md` 용어 일치 여부 확인.
* [ ] 값 객체는 불변성을 보장하는가?
* [ ] 애그리게잇 외부에서 내부 상태에 직접 접근하지 않는가?
* [ ] 불변식을 객체 생성 시 강제하고 있는가?

📝 학습 과제

1. `DateRange` 값 객체 구현 (겹침/포함/연속 여부 판별 메서드).
2. `Reservation` 생성 팩토리 메서드 작성 → 스케줄 검증.
3. `Membership` 갱신 로직 작성 → 연속되지 않으면 예외 발생.
4. 각 기능에 대한 JUnit 단위 테스트 작성 (Given-When-Then).

---

📌 이 문서는 단순 요약이 아니라 학습을 위한 예시 코드와 과제를 포함해, 실제로 따라 치며 이해할 수 있도록 구성됨.
