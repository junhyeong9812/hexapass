# STEP 1: 순수 OOP 도메인 모델링

> **목표**: 복잡한 프레임워크나 라이브러리 없이 순수 Java OOP로 핵심 도메인 모델을 구현합니다.  
> **소요 시간**: 약 1주 (20-30시간)  
> **선행 조건**: Java 기본 문법, OOP 개념, JUnit 기초

## 🎯 이번 단계의 학습 목표

### 핵심 개념
- **값 객체(Value Object)** vs **엔티티(Entity)** 구분
- **불변 객체(Immutable Object)** 설계와 장점
- **불변식(Invariant)** 보장 방법
- **도메인 주도 설계(DDD)** 기본 원칙
- **테스트 주도 개발(TDD)** 기초

### 실무 스킬
- 생성자와 팩토리 메서드를 통한 객체 생성 제어
- `equals()`와 `hashCode()` 올바른 구현
- 예외 설계와 에러 핸들링
- 단위 테스트 작성 방법

## 📋 1단계 체크리스트

### 구현할 클래스들
- [ ] **Money** (금액 값 객체)
- [ ] **DateRange** (날짜 범위 값 객체)
- [ ] **TimeSlot** (시간대 값 객체)
- [ ] **Member** (회원 엔티티)
- [ ] **MembershipPlan** (멤버십 플랜 엔티티)
- [ ] **Reservation** (예약 엔티티)
- [ ] **기본 열거형들** (MemberStatus, ReservationStatus 등)

### 테스트 작성
- [ ] 각 클래스별 단위 테스트
- [ ] 성공/실패/경계값 시나리오
- [ ] 불변식 위반 테스트
- [ ] equals/hashCode 테스트

## 🏗️ 구현 순서와 상세 가이드

### Phase 1: 기본 값 객체 구현 (2-3일)

#### 1.1 Money 클래스

```java
// src/main/java/com/hexapass/domain/common/Money.java
```

**구현해야 할 기능:**
- 금액과 통화를 함께 관리
- 불변 객체로 설계 (세터 없음)
- 산술 연산 (add, subtract, multiply, divide)
- 비교 연산 (compareTo, equals)

**핵심 불변식:**
- 금액은 0 이상이어야 함 (음수 불허)
- 통화는 null이 될 수 없음
- 서로 다른 통화끼리는 직접 연산 불가

**구현 예시:**
```java
public final class Money implements Comparable<Money> {
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = validateAmount(amount);
        this.currency = validateCurrency(currency);
    }
    
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
    
    public static Money won(long amount) {
        return of(BigDecimal.valueOf(amount), Currency.getInstance("KRW"));
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return Money.of(this.amount.add(other.amount), this.currency);
    }
    
    // 나머지 메서드들...
}
```

**테스트 작성 포인트:**
```java
class MoneyTest {
    @Test
    void 같은_통화끼리_더할_수_있다() {
        Money money1 = Money.won(1000);
        Money money2 = Money.won(500);
        
        Money result = money1.add(money2);
        
        assertThat(result).isEqualTo(Money.won(1500));
    }
    
    @Test
    void 다른_통화끼리_연산하면_예외가_발생한다() {
        Money won = Money.won(1000);
        Money usd = Money.of(BigDecimal.valueOf(10), Currency.getInstance("USD"));
        
        assertThatThrownBy(() -> won.add(usd))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("다른 통화끼리 연산할 수 없습니다");
    }
    
    @Test
    void 음수_금액으로_생성하면_예외가_발생한다() {
        assertThatThrownBy(() -> Money.won(-1000))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

#### 1.2 DateRange 클래스

**구현해야 할 기능:**
- 시작일과 종료일을 포함하는 기간 표현
- 겹침 검사 (overlaps)
- 포함 검사 (contains)
- 기간 계산 (duration)

**핵심 불변식:**
- 시작일 ≤ 종료일
- null 날짜 불허
- 시작일과 종료일이 모두 포함되는 구간

**구현 예시:**
```java
public final class DateRange {
    private final LocalDate startDate;
    private final LocalDate endDate;
    
    private DateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = validateNotNull(startDate, "시작일");
        this.endDate = validateNotNull(endDate, "종료일");
        validateDateOrder(startDate, endDate);
    }
    
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }
    
    public static DateRange singleDay(LocalDate date) {
        return new DateRange(date, date);
    }
    
    public boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) && 
               !other.endDate.isBefore(this.startDate);
    }
    
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1; // 양끝 포함
    }
}
```

#### 1.3 TimeSlot 클래스

**구현해야 할 기능:**
- 구체적인 시작시간과 종료시간
- 시간대 겹침 검사
- 인접 시간대 확인
- 소요 시간 계산

**핵심 불변식:**
- 시작시간 < 종료시간
- 같은 날짜 내의 시간대만 허용
- null 시간 불허

**구현 예시:**
```java
public final class TimeSlot {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    
    private TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = validateNotNull(startTime, "시작시간");
        this.endTime = validateNotNull(endTime, "종료시간");
        validateTimeOrder(startTime, endTime);
        validateSameDate(startTime, endTime);
    }
    
    public static TimeSlot of(LocalDateTime startTime, LocalDateTime endTime) {
        return new TimeSlot(startTime, endTime);
    }
    
    public boolean overlaps(TimeSlot other) {
        return startTime.isBefore(other.endTime) && endTime.isAfter(other.startTime);
    }
    
    public boolean isAdjacent(TimeSlot other) {
        return endTime.equals(other.startTime) || other.endTime.equals(startTime);
    }
    
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }
}
```

### Phase 2: 열거형과 상태 클래스 (1일)

#### 2.1 상태 열거형들

```java
// 회원 상태
public enum MemberStatus {
    ACTIVE("활성"),
    SUSPENDED("정지"),
    WITHDRAWN("탈퇴");
    
    private final String description;
    
    MemberStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

// 예약 상태
public enum ReservationStatus {
    REQUESTED("예약요청"),
    CONFIRMED("예약확정"), 
    IN_USE("사용중"),
    COMPLETED("사용완료"),
    CANCELLED("예약취소");
    
    private final String description;
    
    ReservationStatus(String description) {
        this.description = description;
    }
    
    public boolean canTransitionTo(ReservationStatus newStatus) {
        // 상태 전이 규칙 정의
        switch (this) {
            case REQUESTED:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == IN_USE || newStatus == CANCELLED;
            case IN_USE:
                return newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED:
            case CANCELLED:
                return false; // 최종 상태
            default:
                return false;
        }
    }
}

// 멤버십 플랜 타입
public enum PlanType {
    MONTHLY("월간권", 30),
    YEARLY("연간권", 365),
    PERIOD("기간제", 0); // 기간은 별도 지정
    
    private final String displayName;
    private final int defaultDays;
    
    PlanType(String displayName, int defaultDays) {
        this.displayName = displayName;
        this.defaultDays = defaultDays;
    }
}
```

### Phase 3: 엔티티 구현 (3-4일)

#### 3.1 MembershipPlan 엔티티

```java
public class MembershipPlan {
    private final String planId;
    private final String name;
    private final PlanType type;
    private final Money price;
    private final int durationDays;
    private final Set<String> privileges; // 이용 가능한 리소스 타입들
    
    private MembershipPlan(String planId, String name, PlanType type, 
                           Money price, int durationDays, Set<String> privileges) {
        this.planId = validateNotBlank(planId, "플랜 ID");
        this.name = validateNotBlank(name, "플랜명");
        this.type = validateNotNull(type, "플랜 타입");
        this.price = validateNotNull(price, "가격");
        this.durationDays = validatePositive(durationDays, "이용 기간");
        this.privileges = Set.copyOf(privileges); // 불변 복사본
    }
    
    public static MembershipPlan create(String planId, String name, PlanType type,
                                      Money price, int durationDays, Set<String> privileges) {
        return new MembershipPlan(planId, name, type, price, durationDays, privileges);
    }
    
    public boolean hasPrivilege(String resourceType) {
        return privileges.contains(resourceType);
    }
    
    public Money calculateProRatedPrice(int remainingDays) {
        if (remainingDays >= durationDays) {
            return price;
        }
        
        BigDecimal ratio = BigDecimal.valueOf(remainingDays)
                                   .divide(BigDecimal.valueOf(durationDays), 2, RoundingMode.HALF_UP);
        return price.multiply(ratio);
    }
    
    // equals, hashCode는 planId 기준
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipPlan)) return false;
        MembershipPlan that = (MembershipPlan) o;
        return Objects.equals(planId, that.planId);
    }
}
```

#### 3.2 Member 엔티티

```java
public class Member {
    private final String memberId;
    private final String name;
    private final String email;
    private final String phone;
    private MemberStatus status;
    private MembershipPlan currentPlan;
    private DateRange membershipPeriod;
    
    private Member(String memberId, String name, String email, String phone) {
        this.memberId = validateNotBlank(memberId, "회원 ID");
        this.name = validateNotBlank(name, "회원명");
        this.email = validateEmail(email);
        this.phone = validatePhone(phone);
        this.status = MemberStatus.ACTIVE; // 기본값
    }
    
    public static Member create(String memberId, String name, String email, String phone) {
        return new Member(memberId, name, email, phone);
    }
    
    public void assignMembership(MembershipPlan plan, DateRange period) {
        validateNotNull(plan, "멤버십 플랜");
        validateNotNull(period, "멤버십 기간");
        validateMembershipPeriod(period);
        
        this.currentPlan = plan;
        this.membershipPeriod = period;
    }
    
    public boolean canReserve(String resourceType, LocalDate reservationDate) {
        if (status != MemberStatus.ACTIVE) {
            return false;
        }
        
        if (currentPlan == null || membershipPeriod == null) {
            return false;
        }
        
        if (!membershipPeriod.contains(reservationDate)) {
            return false;
        }
        
        return currentPlan.hasPrivilege(resourceType);
    }
    
    public void suspend() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴한 회원은 정지할 수 없습니다");
        }
        this.status = MemberStatus.SUSPENDED;
    }
    
    public void activate() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("탈퇴한 회원은 활성화할 수 없습니다");
        }
        this.status = MemberStatus.ACTIVE;
    }
    
    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        // 탈퇴 시 추가 비즈니스 로직 (예: 예약 자동 취소 등)
    }
    
    // equals, hashCode는 memberId 기준
}
```

#### 3.3 Reservation 엔티티

```java
public class Reservation {
    private final String reservationId;
    private final String memberId;
    private final String resourceId;
    private final TimeSlot timeSlot;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    
    private Reservation(String reservationId, String memberId, String resourceId, TimeSlot timeSlot) {
        this.reservationId = validateNotBlank(reservationId, "예약 ID");
        this.memberId = validateNotBlank(memberId, "회원 ID");
        this.resourceId = validateNotBlank(resourceId, "리소스 ID");
        this.timeSlot = validateNotNull(timeSlot, "예약 시간");
        this.status = ReservationStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
        
        validateReservationTime();
    }
    
    public static Reservation create(String reservationId, String memberId, 
                                   String resourceId, TimeSlot timeSlot) {
        return new Reservation(reservationId, memberId, resourceId, timeSlot);
    }
    
    public void confirm() {
        if (!status.canTransitionTo(ReservationStatus.CONFIRMED)) {
            throw new IllegalStateException(
                String.format("예약 상태를 %s에서 %s로 변경할 수 없습니다", status, ReservationStatus.CONFIRMED));
        }
        
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
    
    public void startUsing() {
        if (!status.canTransitionTo(ReservationStatus.IN_USE)) {
            throw new IllegalStateException("확정된 예약만 사용을 시작할 수 있습니다");
        }
        
        this.status = ReservationStatus.IN_USE;
    }
    
    public void complete() {
        if (status != ReservationStatus.IN_USE) {
            throw new IllegalStateException("사용중인 예약만 완료할 수 있습니다");
        }
        
        this.status = ReservationStatus.COMPLETED;
    }
    
    public void cancel(String reason) {
        if (status == ReservationStatus.COMPLETED || status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 완료되거나 취소된 예약은 취소할 수 없습니다");
        }
        
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = validateNotBlank(reason, "취소 사유");
    }
    
    public boolean isActive() {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.IN_USE;
    }
    
    public boolean conflictsWith(TimeSlot otherTimeSlot) {
        return this.timeSlot.overlaps(otherTimeSlot);
    }
    
    private void validateReservationTime() {
        if (timeSlot.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("과거 시간으로는 예약할 수 없습니다");
        }
    }
}
```

### Phase 4: 테스트 작성 (2-3일)

#### 4.1 값 객체 테스트 패턴

```java
class MoneyTest {
    
    @DisplayName("Money 생성 테스트")
    @Nested
    class CreationTest {
        
        @Test
        @DisplayName("유효한 금액과 통화로 생성할 수 있다")
        void createValidMoney() {
            Money money = Money.of(BigDecimal.valueOf(1000), Currency.getInstance("KRW"));
            
            assertThat(money).isNotNull();
            assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        }
        
        @Test
        @DisplayName("음수 금액으로 생성하면 예외가 발생한다")
        void createWithNegativeAmount() {
            assertThatThrownBy(() -> Money.won(-1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금액은 0 이상이어야 합니다");
        }
        
        @Test
        @DisplayName("null 통화로 생성하면 예외가 발생한다")
        void createWithNullCurrency() {
            assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(1000), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("통화는 null일 수 없습니다");
        }
    }
    
    @DisplayName("Money 연산 테스트")
    @Nested  
    class OperationTest {
        
        @Test
        @DisplayName("같은 통화끼리 더할 수 있다")
        void addSameCurrency() {
            Money money1 = Money.won(1000);
            Money money2 = Money.won(500);
            
            Money result = money1.add(money2);
            
            assertThat(result).isEqualTo(Money.won(1500));
        }
        
        @ParameterizedTest
        @DisplayName("다른 통화끼리 연산하면 예외가 발생한다")
        @MethodSource("provideDifferentCurrencies")
        void operateWithDifferentCurrencies(Money money1, Money money2) {
            assertThatThrownBy(() -> money1.add(money2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("다른 통화");
        }
        
        static Stream<Arguments> provideDifferentCurrencies() {
            return Stream.of(
                Arguments.of(Money.won(1000), Money.usd(10)),
                Arguments.of(Money.usd(10), Money.eur(5))
            );
        }
    }
    
    @DisplayName("Money 동등성 테스트")  
    @Nested
    class EqualityTest {
        
        @Test
        @DisplayName("같은 금액과 통화는 동등하다")
        void equalityWithSameAmountAndCurrency() {
            Money money1 = Money.won(1000);
            Money money2 = Money.won(1000);
            
            assertThat(money1).isEqualTo(money2);
            assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
        }
        
        @Test
        @DisplayName("다른 금액은 동등하지 않다")  
        void inequalityWithDifferentAmount() {
            Money money1 = Money.won(1000);
            Money money2 = Money.won(2000);
            
            assertThat(money1).isNotEqualTo(money2);
        }
    }
}
```

#### 4.2 엔티티 테스트 패턴

```java
class ReservationTest {
    
    private Member member;
    private String resourceId;
    private TimeSlot futureTimeSlot;
    
    @BeforeEach
    void setUp() {
        member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
        resourceId = "ROOM_001";
        futureTimeSlot = TimeSlot.of(
            LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
            LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0)
        );
    }
    
    @Test
    @DisplayName("유효한 정보로 예약을 생성할 수 있다")
    void createReservation() {
        Reservation reservation = Reservation.create("R001", member.getMemberId(), resourceId, futureTimeSlot);
        
        assertThat(reservation.getReservationId()).isEqualTo("R001");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.getCreatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("과거 시간으로 예약하면 예외가 발생한다")
    void createReservationWithPastTime() {
        TimeSlot pastTimeSlot = TimeSlot.of(
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1)
        );
        
        assertThatThrownBy(() -> 
            Reservation.create("R001", member.getMemberId(), resourceId, pastTimeSlot))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("과거 시간");
    }
    
    @Test
    @DisplayName("예약 상태를 순서대로 변경할 수 있다")
    void changeStatusInOrder() {
        Reservation reservation = Reservation.create("R001", member.getMemberId(), resourceId, futureTimeSlot);
        
        // REQUESTED -> CONFIRMED
        reservation.confirm();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isNotNull();
        
        // CONFIRMED -> IN_USE
        reservation.startUsing();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.IN_USE);
        
        // IN_USE -> COMPLETED
        reservation.complete();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }
    
    @Test
    @DisplayName("잘못된 상태 전이시 예외가 발생한다")
    void invalidStatusTransition() {
        Reservation reservation = Reservation.create("R001", member.getMemberId(), resourceId, futureTimeSlot);
        
        // REQUESTED -> IN_USE (CONFIRMED 생략)
        assertThatThrownBy(() -> reservation.startUsing())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("확정된 예약만");
    }
    
    @Test
    @DisplayName("예약 시간 충돌을 검사할 수 있다")
    void checkTimeConflict() {
        Reservation reservation = Reservation.create("R001", member.getMemberId(), resourceId, futureTimeSlot);
        
        // 겹치는 시간대
        TimeSlot overlappingSlot = TimeSlot.of(
            futureTimeSlot.getStartTime().plusMinutes(30),
            futureTimeSlot.getEndTime().plusMinutes(30)
        );
        
        // 겹치지 않는 시간대
        TimeSlot nonOverlappingSlot = TimeSlot.of(
            futureTimeSlot.getEndTime(),
            futureTimeSlot.getEndTime().plusHours(1)
        );
        
        assertThat(reservation.conflictsWith(overlappingSlot)).isTrue();
        assertThat(reservation.conflictsWith(nonOverlappingSlot)).isFalse();
    }
}
```

### Phase 5: 통합 시나리오 테스트 (1일)

```java
class MembershipScenarioTest {
    
    @Test
    @DisplayName("회원 가입부터 예약까지의 전체 시나리오")
    void fullMembershipScenario() {
        // Given: 회원 생성
        Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
        
        // 멤버십 플랜 생성
        MembershipPlan monthlyPlan = MembershipPlan.create(
            "PLAN_MONTHLY",
            "월간 프리미엄",
            PlanType.MONTHLY,
            Money.won(50000),
            30,
            Set.of("GYM", "POOL", "SAUNA")
        );
        
        // 멤버십 기간 설정 (오늘부터 30일)
        DateRange membershipPeriod = DateRange.of(
            LocalDate.now(),
            LocalDate.now().plusDays(30)
        );
        
        // When: 멤버십 할당
        member.assignMembership(monthlyPlan, membershipPeriod);
        
        // Then: 예약 권한 확인
        assertThat(member.canReserve("GYM", LocalDate.now().plusDays(1))).isTrue();
        assertThat(member.canReserve("STUDY_ROOM", LocalDate.now().plusDays(1))).isFalse(); // 권한 없는 리소스
        assertThat(member.canReserve("GYM", LocalDate.now().plusDays(40))).isFalse(); // 멤버십 기간 외
        
        // When: 예약 생성
        TimeSlot timeSlot = TimeSlot.of(
            LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0),
            LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0)
        );
        
        Reservation reservation = Reservation.create("R001", member.getMemberId(), "GYM_001", timeSlot);
        
        // Then: 예약 상태 확인
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
        
        // When: 예약 확정
        reservation.confirm();
        
        // Then: 예약 완료 확인
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.isActive()).isTrue();
    }
    
    @Test
    @DisplayName("회원 정지 후 예약 불가 시나리오")
    void suspendedMemberCannotReserve() {
        // Given
        Member member = Member.create("M001", "김회원", "member@test.com", "010-1234-5678");
        MembershipPlan plan = createBasicPlan();
        DateRange period = DateRange.of(LocalDate.now(), LocalDate.now().plusDays(30));
        
        member.assignMembership(plan, period);
        
        // When: 회원 정지
        member.suspend();
        
        // Then: 예약 불가
        assertThat(member.canReserve("GYM", LocalDate.now().plusDays(1))).isFalse();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }
    
    private MembershipPlan createBasicPlan() {
        return MembershipPlan.create(
            "PLAN_BASIC",
            "기본 플랜",
            PlanType.MONTHLY,
            Money.won(30000),
            30,
            Set.of("GYM")
        );
    }
}
```

## 🎯 1단계 완료 기준

### 기능적 완료 기준
- [ ] 모든 값 객체가 불변으로 구현됨
- [ ] 모든 엔티티가 equals/hashCode를 올바르게 구현함
- [ ] 비즈니스 규칙 위반 시 적절한 예외 발생
- [ ] 상태 전이 규칙이 코드로 강제됨

### 테스트 완료 기준
- [ ] 단위 테스트 커버리지 80% 이상
- [ ] 모든 불변식 위반 케이스 테스트
- [ ] 경계값 테스트 (null, 빈 문자열, 음수 등)
- [ ] 통합 시나리오 테스트 3개 이상

### 코드 품질 기준
- [ ] 메서드당 라인 수 20줄 이하
- [ ] 순환 복잡도 10 이하
- [ ] 의미있는 변수명과 메서드명
- [ ] 도메인 용어가 코드에 그대로 반영

## 🚀 다음 단계 준비

1단계가 완료되면 다음을 확인하세요:

1. **리팩토링 포인트 식별**: `docs/refactoring-notes.md`에 개선할 점 기록
2. **SOLID 원칙 위반 사항 점검**: 특히 SRP, OCP 위반 클래스들
3. **중복 코드 정리**: 비슷한 검증 로직이나 생성 패턴들
4. **다음 단계 계획**: 전략 패턴 적용할 할인 정책부터 시작

**축하합니다! 🎉**
순수 OOP로 도메인 모델의 기초를 완성했습니다. 이제 SOLID 원칙과 디자인 패턴을 적용해서 더욱 유연하고 확장 가능한 구조로 발전시켜 나가겠습니다.