# STEP 1-2: 정책 시스템 구현 (Policy System Implementation)

> **목표**: 할인 정책, 취소 정책, 예약 정책을 Strategy Pattern과 Specification Pattern을 활용하여 구현합니다.  
> **소요 시간**: 약 1주 (25-30시간)  
> **선행 조건**: STEP 1 완료, Strategy Pattern과 Specification Pattern 이해

## 🎯 정책 시스템 개요

HexaPass 프로젝트에서 정책 시스템은 비즈니스 규칙을 캡슐화하여 관리하는 핵심 구성요소입니다. 세 가지 주요 정책 영역으로 구성됩니다:

1. **할인 정책 (Discount Policy)**: 가격 할인 규칙
2. **취소 정책 (Cancellation Policy)**: 예약 취소 및 수수료 규칙
3. **예약 정책 (Reservation Policy)**: 예약 가능 여부 판단 규칙

## 📋 구현 완료 체크리스트

### 할인 정책 시스템
- [ ] **기본 할인 정책들** (정액, 정률, 멤버십, 쿠폰, 계절별)
- [ ] **복합 할인 정책** (여러 할인 조합)
- [ ] **유연한 할인 정책** (런타임 구성 가능)
- [ ] **할인 없음 정책** (Null Object Pattern)

### 취소 정책 시스템
- [ ] **표준 취소 정책** (시간대별 차등 수수료)
- [ ] **유연한 취소 정책** (첫 회 취소 혜택)
- [ ] **엄격한 취소 정책** (높은 수수료, 당일 제한)
- [ ] **취소 불가 정책** (특별 할인 상품)
- [ ] **수수료 계산기** (복잡한 수수료 규칙 처리)

### 예약 정책 시스템
- [ ] **표준 예약 정책** (기본 조건들)
- [ ] **프리미엄 예약 정책** (관대한 조건)
- [ ] **제한적 예약 정책** (엄격한 조건)
- [ ] **유연한 예약 정책** (동적 조건 조합)

## 🏛️ 아키텍처 설계 결정사항

### 패턴 선택과 트레이드오프

#### Strategy Pattern 적용
**적용 영역**: 할인 정책, 취소 정책
```java
// 장점: 정책 변경 시 기존 코드 수정 없음, 런타임 교체 가능
// 단점: 클래스 수 증가, 단순한 정책에는 과도할 수 있음

public interface DiscountPolicy {
    Money applyDiscount(Money originalPrice, DiscountContext context);
}

public class RateDiscountPolicy implements DiscountPolicy {
    // 정률 할인 구현
}
```

#### Specification Pattern 적용
**적용 영역**: 예약 정책
```java
// 장점: 복잡한 조건의 명시적 표현, 조합 가능
// 단점: 작은 조건에도 클래스 생성, 디버깅 복잡

public interface ReservationSpecification {
    boolean isSatisfiedBy(ReservationContext context);
    ReservationSpecification and(ReservationSpecification other);
    ReservationSpecification or(ReservationSpecification other);
}
```

#### 하이브리드 접근법 선택
**할인/취소 정책**에는 완전한 Specification 패턴 적용을 보류했습니다:

**이유:**
1. **도메인 특성**: 할인과 취소는 단순 true/false가 아닌 **금액 계산**이 핵심
2. **복잡도**: 시간대별 차등 수수료 같은 연속적 조건은 Specification으로 표현하기 어려움
3. **실용성**: 기존 Strategy Pattern 구조가 이미 충분히 유연함

**채택한 접근법:**
```java
// 조건 검증만 Specification, 계산은 기존 방식 유지
public class FlexibleCancellationPolicy implements CancellationPolicy {
    private final CancellationSpecification allowanceSpec; // 취소 가능 여부만
    private final CancellationFeeCalculator calculator;    // 수수료 계산 로직
}
```

## 🏗️ 구현 가이드

### Phase 1: 할인 정책 구현 (3-4일)

#### 1.1 기본 할인 정책들

**AmountDiscountPolicy (정액 할인)**
```java
public class AmountDiscountPolicy implements DiscountPolicy {
    private final Money discountAmount;
    private final Money minimumAmount;
    
    public static AmountDiscountPolicy create(Money discountAmount, String description) {
        return new AmountDiscountPolicy(discountAmount, description, null, 100);
    }
    
    public static AmountDiscountPolicy withMinimum(Money discountAmount, String description, Money minimumAmount) {
        return new AmountDiscountPolicy(discountAmount, description, minimumAmount, 100);
    }
    
    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        if (!isApplicable(context)) {
            return originalPrice;
        }
        
        // 최소 금액 체크
        if (minimumAmount != null && originalPrice.isLessThan(minimumAmount)) {
            return originalPrice;
        }
        
        // 할인 금액이 원래 가격보다 크면 0원까지만
        if (discountAmount.isGreaterThan(originalPrice)) {
            return Money.zero(originalPrice.getCurrency());
        }
        
        return originalPrice.subtract(discountAmount);
    }
}
```

**RateDiscountPolicy (정률 할인)**
```java
public class RateDiscountPolicy implements DiscountPolicy {
    private final BigDecimal discountRate; // 0.0 ~ 1.0
    private final Money maximumDiscount; // 최대 할인 한도
    
    public static RateDiscountPolicy create(BigDecimal discountRate, String description) {
        return new RateDiscountPolicy(discountRate, description, null, null, 100);
    }
    
    public static RateDiscountPolicy withCap(BigDecimal discountRate, String description, Money maximumDiscount) {
        return new RateDiscountPolicy(discountRate, description, null, maximumDiscount, 100);
    }
    
    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        Money discountAmount = originalPrice.multiply(discountRate);
        
        // 최대 할인 한도 적용
        if (maximumDiscount != null && discountAmount.isGreaterThan(maximumDiscount)) {
            discountAmount = maximumDiscount;
        }
        
        return originalPrice.subtract(discountAmount);
    }
}
```

#### 1.2 복합 할인 정책

**CompositeDiscountPolicy**
```java
public class CompositeDiscountPolicy implements DiscountPolicy {
    public enum CombinationStrategy {
        SEQUENTIAL("순차 적용 - 모든 할인을 차례로 적용"),
        BEST_DISCOUNT("최고 할인 - 가장 큰 할인만 적용"),
        PRIORITY_FIRST("우선순위 - 가장 높은 우선순위 할인만 적용");
    }
    
    private final List<DiscountPolicy> policies;
    private final CombinationStrategy strategy;
    
    public static CompositeDiscountPolicy sequential(List<DiscountPolicy> policies, String description) {
        return new CompositeDiscountPolicy(policies, description, CombinationStrategy.SEQUENTIAL);
    }
    
    public static CompositeDiscountPolicy bestDiscount(List<DiscountPolicy> policies, String description) {
        return new CompositeDiscountPolicy(policies, description, CombinationStrategy.BEST_DISCOUNT);
    }
    
    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        return switch (strategy) {
            case SEQUENTIAL -> applySequential(originalPrice, context);
            case BEST_DISCOUNT -> applyBestDiscount(originalPrice, context);
            case PRIORITY_FIRST -> applyPriorityFirst(originalPrice, context);
        };
    }
}
```

#### 1.3 할인 정책 테스트

```java
class AmountDiscountPolicyTest {
    
    @Test
    @DisplayName("정액 할인을 올바르게 적용한다")
    void applyAmountDiscount() {
        // Given
        AmountDiscountPolicy policy = AmountDiscountPolicy.create(Money.won(5000), "5천원 할인");
        Money originalPrice = Money.won(20000);
        DiscountContext context = createBasicContext();
        
        // When
        Money discountedPrice = policy.applyDiscount(originalPrice, context);
        
        // Then
        assertThat(discountedPrice).isEqualTo(Money.won(15000));
    }
    
    @Test
    @DisplayName("할인 금액이 원가보다 크면 0원이 된다")
    void discountCannotExceedOriginalPrice() {
        // Given
        AmountDiscountPolicy policy = AmountDiscountPolicy.create(Money.won(30000), "3만원 할인");
        Money originalPrice = Money.won(20000);
        DiscountContext context = createBasicContext();
        
        // When
        Money discountedPrice = policy.applyDiscount(originalPrice, context);
        
        // Then
        assertThat(discountedPrice).isEqualTo(Money.won(0));
    }
    
    @Test
    @DisplayName("최소 금액 미만이면 할인이 적용되지 않는다")
    void noDiscountBelowMinimumAmount() {
        // Given
        AmountDiscountPolicy policy = AmountDiscountPolicy.withMinimum(
            Money.won(5000), "5천원 할인", Money.won(30000));
        Money originalPrice = Money.won(20000);
        DiscountContext context = createBasicContext();
        
        // When
        Money discountedPrice = policy.applyDiscount(originalPrice, context);
        
        // Then
        assertThat(discountedPrice).isEqualTo(originalPrice); // 할인 적용 안됨
    }
}
```

### Phase 2: 취소 정책 구현 (2-3일)

#### 2.1 표준 취소 정책

**시간대별 차등 수수료 적용**
```java
public class StandardCancellationPolicy implements CancellationPolicy {
    private static final BigDecimal FREE_RATE = BigDecimal.ZERO;
    private static final BigDecimal LOW_FEE_RATE = new BigDecimal("0.20");   // 20%
    private static final BigDecimal MEDIUM_FEE_RATE = new BigDecimal("0.50"); // 50%
    private static final BigDecimal HIGH_FEE_RATE = new BigDecimal("0.80");   // 80%
    private static final BigDecimal FULL_FEE_RATE = BigDecimal.ONE;          // 100%
    
    @Override
    public Money calculateCancellationFee(Money originalPrice, CancellationContext context) {
        BigDecimal feeRate = determineFeeRate(context);
        return originalPrice.multiply(feeRate);
    }
    
    private BigDecimal determineFeeRate(CancellationContext context) {
        long hoursUntilReservation = context.getHoursUntilReservation();
        
        if (context.isAfterReservationTime()) {
            return FULL_FEE_RATE; // 100% - 예약 시간 이후
        } else if (hoursUntilReservation < 2) {
            return HIGH_FEE_RATE; // 80% - 2시간 미만
        } else if (hoursUntilReservation < 6) {
            return MEDIUM_FEE_RATE; // 50% - 2-6시간 전
        } else if (hoursUntilReservation < 24) {
            return LOW_FEE_RATE; // 20% - 6-24시간 전
        } else {
            return FREE_RATE; // 0% - 24시간 이전
        }
    }
}
```

#### 2.2 수수료 계산기 (복잡한 규칙 처리)

**CancellationFeeCalculator**
```java
public class CancellationFeeCalculator {
    public static class FeeRule {
        private final Duration minimumHoursBefore;
        private final Duration maximumHoursBefore;
        private final BigDecimal feeRate;
        private final Money fixedFee;
        private final Money maximumFee;
        
        public static FeeRule rateOnly(Duration minimumHoursBefore, Duration maximumHoursBefore,
                                       BigDecimal feeRate, String description) {
            return new FeeRule(minimumHoursBefore, maximumHoursBefore,
                    feeRate, Money.zeroWon(), null, description);
        }
        
        public static FeeRule combined(Duration minimumHoursBefore, Duration maximumHoursBefore,
                                       BigDecimal feeRate, Money fixedFee, Money maximumFee, String description) {
            return new FeeRule(minimumHoursBefore, maximumHoursBefore,
                    feeRate, fixedFee, maximumFee, description);
        }
        
        public Money calculateFee(Money originalPrice) {
            Money rateFee = originalPrice.multiply(feeRate);
            Money totalFee = rateFee.add(fixedFee);
            
            if (maximumFee != null && totalFee.compareTo(maximumFee) > 0) {
                return maximumFee;
            }
            
            return totalFee;
        }
    }
    
    private final List<FeeRule> feeRules;
    
    public Money calculateFee(Money originalPrice, CancellationContext context) {
        Duration hoursBeforeReservation = context.getTimeBetweenCancellationAndReservation();
        
        return feeRules.stream()
                .filter(rule -> rule.applies(hoursBeforeReservation))
                .findFirst()
                .map(rule -> rule.calculateFee(originalPrice))
                .orElse(originalPrice); // 규칙이 없으면 전액 수수료
    }
}
```

### Phase 3: 예약 정책 구현 (3-4일)

#### 3.1 Specification Pattern 구현

**기본 Specification 인터페이스**
```java
public interface ReservationSpecification {
    boolean isSatisfiedBy(ReservationContext context);
    String getDescription();
    
    default ReservationSpecification and(ReservationSpecification other) {
        return new AndSpecification(this, other);
    }
    
    default ReservationSpecification or(ReservationSpecification other) {
        return new OrSpecification(this, other);
    }
    
    default ReservationSpecification not() {
        return new NotSpecification(this);
    }
}
```

**구체적인 Specification 구현체들**
```java
public class ActiveMemberSpecification implements ReservationSpecification {
    private final boolean strictMembership;
    private final boolean allowSuspended;
    
    public static ActiveMemberSpecification standard() {
        return new ActiveMemberSpecification(true, false);
    }
    
    public static ActiveMemberSpecification lenient() {
        return new ActiveMemberSpecification(false, true);
    }
    
    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        Member member = context.getMember();
        
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            return false; // 탈퇴 회원은 항상 불가
        }
        
        if (member.getStatus() == MemberStatus.SUSPENDED && !allowSuspended) {
            return false;
        }
        
        if (strictMembership) {
            return member.getMembershipPlan() != null && 
                   member.getMembershipPlan().isActive();
        }
        
        return true;
    }
    
    public String getFailureReason(ReservationContext context) {
        Member member = context.getMember();
        
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            return "탈퇴한 회원은 예약할 수 없습니다";
        }
        
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            return "정지된 회원은 예약할 수 없습니다";
        }
        
        if (strictMembership && (member.getMembershipPlan() == null || !member.getMembershipPlan().isActive())) {
            return "유효한 멤버십이 필요합니다";
        }
        
        return null;
    }
}
```

#### 3.2 복합 Specification

**AndSpecification, OrSpecification, NotSpecification**
```java
public class AndSpecification implements ReservationSpecification {
    private final ReservationSpecification left;
    private final ReservationSpecification right;
    
    public AndSpecification(ReservationSpecification left, ReservationSpecification right) {
        this.left = left;
        this.right = right;
    }
    
    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return left.isSatisfiedBy(context) && right.isSatisfiedBy(context);
    }
    
    @Override
    public String getDescription() {
        return String.format("(%s) AND (%s)", left.getDescription(), right.getDescription());
    }
}
```

#### 3.3 예약 정책 구현

**StandardReservationPolicy**
```java
public class StandardReservationPolicy implements ReservationPolicy {
    private final ReservationSpecification specification;
    
    // 개별 사양들을 필드로 저장하여 재사용
    private final ActiveMemberSpecification activeMemberSpec;
    private final MembershipPrivilegeSpecification membershipPrivilegeSpec;
    private final ResourceCapacitySpecification resourceCapacitySpec;
    
    public StandardReservationPolicy() {
        this.activeMemberSpec = ActiveMemberSpecification.standard();
        this.membershipPrivilegeSpec = MembershipPrivilegeSpecification.standard();
        this.resourceCapacitySpec = ResourceCapacitySpecification.standard();
        // ... 다른 사양들
        
        // 모든 사양을 AND 조건으로 결합
        this.specification = activeMemberSpec
                .and(membershipPrivilegeSpec)
                .and(resourceCapacitySpec);
                // ... 다른 사양들 추가
    }
    
    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }
    
    @Override
    public String getViolationReason(ReservationContext context) {
        if (canReserve(context)) {
            return "예약 가능";
        }
        
        List<String> violations = new ArrayList<>();
        
        // 각 사양별 상세한 실패 사유 수집
        if (!activeMemberSpec.isSatisfiedBy(context)) {
            violations.add("회원 상태: " + activeMemberSpec.getFailureReason(context));
        }
        
        if (!membershipPrivilegeSpec.isSatisfiedBy(context)) {
            violations.add("멤버십 권한: " + membershipPrivilegeSpec.getFailureReason(context));
        }
        
        // ... 다른 사양들 체크
        
        return String.join(" | ", violations);
    }
}
```

### Phase 4: 통합 테스트와 시나리오 검증 (2일)

#### 4.1 정책 조합 테스트

```java
class PolicyIntegrationTest {
    
    @Test
    @DisplayName("할인 정책 조합 시나리오")
    void discountPolicyCombinationScenario() {
        // Given: 멤버십 할인 + 쿠폰 할인 조합
        List<DiscountPolicy> policies = List.of(
            new MembershipDiscountPolicy(),
            CouponDiscountPolicy.amountCoupon("WELCOME", Money.won(5000), 
                LocalDate.now(), LocalDate.now().plusDays(30))
        );
        
        CompositeDiscountPolicy compositePolicy = 
            CompositeDiscountPolicy.sequential(policies, "멤버십 + 쿠폰 할인");
        
        Money originalPrice = Money.won(30000);
        DiscountContext context = DiscountContext.withCoupon(member, membershipPlan, "WELCOME");
        
        // When
        Money finalPrice = compositePolicy.applyDiscount(originalPrice, context);
        
        // Then: 멤버십 10% 할인 후 쿠폰 5000원 할인 적용
        Money expectedPrice = Money.won(22000); // 30000 * 0.9 - 5000
        assertThat(finalPrice).isEqualTo(expectedPrice);
    }
    
    @Test
    @DisplayName("예약 정책 복합 조건 시나리오")
    void reservationPolicyComplexConditionScenario() {
        // Given: 프리미엄 회원의 복잡한 예약 조건
        FlexibleReservationPolicy policy = FlexibleReservationPolicy.builder("프리미엄 정책")
            .withLevel(PolicyLevel.PREMIUM)
            .withActiveMemberCheck()
            .withMembershipPrivilegeCheck()
            .withCapacityCheck()
            .withTimeValidation(365, 30) // 1년 이내, 30분 후
            .build();
        
        ReservationContext context = createPremiumReservationContext();
        
        // When
        boolean canReserve = policy.canReserve(context);
        PolicyAnalysis analysis = policy.analyze(context);
        
        // Then
        assertThat(canReserve).isTrue();
        assertThat(analysis.getSuccessRate()).isEqualTo(1.0);
        assertThat(analysis.getPassedChecks()).hasSize(4);
    }
    
    @Test
    @DisplayName("취소 정책 시간대별 수수료 시나리오")
    void cancellationPolicyTimeBasedFeeScenario() {
        // Given: 표준 취소 정책
        StandardCancellationPolicy policy = new StandardCancellationPolicy();
        Money originalPrice = Money.won(50000);
        
        // 시나리오 1: 25시간 전 취소 (무료)
        CancellationContext context1 = CancellationContext.create(
            LocalDateTime.now().plusDays(1).plusHours(1),
            LocalDateTime.now(),
            originalPrice, member, false);
        
        // 시나리오 2: 1시간 전 취소 (80% 수수료)
        CancellationContext context2 = CancellationContext.create(
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now(),
            originalPrice, member, false);
        
        // When & Then
        assertThat(policy.calculateCancellationFee(originalPrice, context1))
            .isEqualTo(Money.won(0)); // 무료
            
        assertThat(policy.calculateCancellationFee(originalPrice, context2))
            .isEqualTo(Money.won(40000)); // 80% 수수료
    }
}
```

#### 4.2 정책 변경 확장성 테스트

```java
class PolicyExtensibilityTest {
    
    @Test
    @DisplayName("새로운 할인 정책 추가 시 기존 코드 영향 없음")
    void addNewDiscountPolicyWithoutModification() {
        // Given: 새로운 VIP 할인 정책 추가
        class VipDiscountPolicy implements DiscountPolicy {
            @Override
            public Money applyDiscount(Money originalPrice, DiscountContext context) {
                // VIP 회원에게 30% 할인
                return originalPrice.multiply(new BigDecimal("0.70"));
            }
            
            @Override
            public boolean isApplicable(DiscountContext context) {
                return context.getMember().isVip();
            }
            
            @Override
            public String getDescription() {
                return "VIP 30% 할인";
            }
        }
        
        // When: 기존 정책들과 함께 사용
        List<DiscountPolicy> policies = List.of(
            new MembershipDiscountPolicy(),
            new VipDiscountPolicy() // 새 정책 추가
        );
        
        CompositeDiscountPolicy compositePolicy = 
            CompositeDiscountPolicy.bestDiscount(policies, "최고 할인 선택");
        
        // Then: 기존 코드 수정 없이 새 정책 적용
        assertThat(compositePolicy).isNotNull();
        assertThat(compositePolicy.getPolicies()).hasSize(2);
    }
}
```

## 📊 구현 결과 검토

### 아키텍처 품질 지표

| 측면 | 할인 정책 | 취소 정책 | 예약 정책 |
|------|-----------|-----------|-----------|
| **확장성** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **테스트 용이성** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **복잡도 관리** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **성능** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### 주요 성과

**할인 정책 시스템:**
- 9개의 독립적인 할인 정책 구현
- 3가지 조합 전략 제공
- 빌더 패턴으로 직관적인 정책 구성 API

**취소 정책 시스템:**
- 시간 기반 차등 수수료 체계
- 복잡한 수수료 규칙을 FeeRule로 캡슐화
- 첫 회 취소자 등 특별 정책 지원

**예약 정책 시스템:**
- Specification Pattern으로 조건 조합
- 4가지 미리 정의된 정책 레벨
- 런타임 정책 구성 및 분석 기능

### 설계 트레이드오프 정리

#### Strategy vs Specification 패턴 선택

**Strategy Pattern 채택 (할인/취소):**
- **장점**: 비즈니스 로직과 잘 맞음, 금액 계산 직관적
- **단점**: 조건 조합이 제한적
- **결론**: 도메인 특성상 적합한 선택

**Specification Pattern 채택 (예약):**
- **장점**: 복잡한 조건 조합 가능, 명시적 표현
- **단점**: 클래스 수 증가, 디버깅 복잡
- **결론**: boolean 결과가 주인 예약 조건에 적합

#### 성능 vs 유연성 트레이드오프

**성능 고려사항:**
- Specification 패턴의 객체 생성 오버헤드
- 복합 정책의 중복 계산 가능성

**최적화 방안:**
- 개별 Specification 인스턴스 재사용
- 조건 평가 순서 최적화 (실패 확률 높은 것 우선)
- 계산 결과 캐싱 (동일 컨텍스트)

## 🚀 다음 단계 준비

### 완료 확인 사항
- [ ] 모든 정책 클래스가 해당 패턴을 올바르게 구현
- [ ] 정책 조합이 예상대로 동작
- [ ] 새 정책 추가 시 기존 코드 수정 불필요
- [ ] 테스트 커버리지 85% 이상

### STEP 2에서 다룰 내용
1. **SOLID 원칙 심화 적용**
2. **의존성 주입 준비** (포트/어댑터 패턴)
3. **도메인 서비스** 분리
4. **동시성 문제** 해결 준비

### 리팩토링 포인트 기록
`docs/refactoring-notes.md`에 다음 사항들을 기록하세요:

1. **패턴 적용 효과**: Strategy vs Specification 선택 이유
2. **성능 이슈**: 복합 정책 평가 시 병목점
3. **확장성 검증**: 새 정책 추가 시나리오 테스트 결과
4. **코드 품질**: 중복 제거 및 네이밍 개선 사항

정책 시스템 구현을 통해 비즈니스 규칙을 체계적으로 관리하는 방법을 익혔습니다. 이제 이 정책들을 활용하는 도메인 서비스와 애플리케이션 계층으로 확장해 나가겠습니다.