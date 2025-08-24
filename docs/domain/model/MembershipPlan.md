# MembershipPlan.java - 상세 주석 및 설명

## 클래스 개요
`MembershipPlan`은 회원이 선택할 수 있는 구독 상품을 나타내는 **엔티티(Entity)**입니다.
이용 권한, 혜택, 가격 정책 등을 정의하며, `planId`를 기준으로 동일성을 판단합니다.

## 왜 이런 클래스가 필요한가?
1. **상품 정책 중앙 관리**: 모든 플랜 관련 규칙을 한 곳에서 관리
2. **가격 계산 로직**: 할인, 일할 계산, 업그레이드 비용 등
3. **권한 관리**: 리소스별 이용 권한과 제한사항 정의
4. **확장성**: 새로운 플랜 타입과 정책을 쉽게 추가

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.model;

import com.hexapass.domain.common.Money; // 값 객체 - 금액
import com.hexapass.domain.type.PlanType; // 열거형 - 플랜 타입
import com.hexapass.domain.type.ResourceType; // 열거형 - 리소스 타입

import java.math.BigDecimal; // 정확한 소수점 계산
import java.math.RoundingMode; // 반올림 방식 정의
import java.time.LocalDateTime; // 날짜+시간
import java.util.*; // Collection 관련 클래스들

/**
 * 멤버십 플랜을 나타내는 엔티티
 * 회원이 선택할 수 있는 구독 상품으로, 이용 권한과 혜택을 정의
 * planId를 기준으로 동일성 판단
 * 
 * 엔티티 특징:
 * 1. 식별자: planId로 구분
 * 2. 가변성: isActive 상태 변경 가능
 * 3. 비즈니스 로직: 가격 계산, 권한 확인 등
 * 4. 복잡성: 다양한 정책과 규칙 포함
 */
public class MembershipPlan {

    // === 불변 필드들 (생성 후 변경 불가) ===
    private final String planId;                    // 플랜 고유 식별자
    private final String name;                      // 플랜명 (사용자에게 표시)
    private final PlanType type;                    // 플랜 타입 (월간/연간/기간제)
    private final Money price;                      // 기본 가격
    private final int durationDays;                 // 이용 기간 (일 단위)
    private final Set<ResourceType> allowedResourceTypes; // 이용 가능한 리소스들
    private final int maxSimultaneousReservations; // 최대 동시 예약 수
    private final int maxAdvanceReservationDays;   // 최대 선예약 일수
    private final BigDecimal discountRate;         // 할인율 (0.0 ~ 1.0)
    private final LocalDateTime createdAt;         // 생성 일시

    // === 가변 필드들 ===
    private boolean isActive;                       // 활성 상태 (판매 중단 여부)

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * 복잡한 생성 파라미터를 받아 유효성 검사 후 초기화
     * 모든 필드의 유효성을 보장하는 것이 핵심
     */
    private MembershipPlan(String planId, String name, PlanType type, Money price,
                           int durationDays, Set<ResourceType> allowedResourceTypes,
                           int maxSimultaneousReservations, int maxAdvanceReservationDays,
                           BigDecimal discountRate) {
        
        // 각 필드별 유효성 검증
        this.planId = validateNotBlank(planId, "플랜 ID");
        this.name = validateNotBlank(name, "플랜명");
        this.type = validateNotNull(type, "플랜 타입");
        this.price = validateNotNull(price, "가격");
        this.durationDays = validatePositive(durationDays, "이용 기간");
        
        // Set.copyOf(): 방어적 복사로 외부 변경 방지
        this.allowedResourceTypes = Set.copyOf(validateNotEmpty(allowedResourceTypes, "이용 가능 리소스"));
        this.maxSimultaneousReservations = validatePositive(maxSimultaneousReservations, "최대 동시 예약 수");
        this.maxAdvanceReservationDays = validateNonNegative(maxAdvanceReservationDays, "최대 선예약 일수");
        this.discountRate = validateDiscountRate(discountRate);
        
        this.createdAt = LocalDateTime.now();
        this.isActive = true; // 기본값: 생성 시 활성 상태

        // 전체적인 일관성 검증 (필드 간 관계 확인)
        validatePlanConsistency();
    }

    /**
     * 기본 멤버십 플랜 생성
     * 
     * 필수 정보만으로 플랜 생성 (기본값 사용)
     * 간단한 플랜 생성 시 사용
     */
    public static MembershipPlan create(String planId, String name, PlanType type, Money price,
                                        int durationDays, Set<ResourceType> allowedResourceTypes) {
        return new MembershipPlan(
                planId, name, type, price, durationDays, allowedResourceTypes,
                3, // 기본 최대 동시 예약 수
                30, // 기본 최대 선예약 일수  
                BigDecimal.ZERO // 기본 할인율 0%
        );
    }

    /**
     * 상세 옵션이 포함된 멤버십 플랜 생성
     * 
     * 모든 옵션을 세밀하게 조정할 수 있는 생성 메서드
     */
    public static MembershipPlan createWithOptions(String planId, String name, PlanType type, Money price,
                                                   int durationDays, Set<ResourceType> allowedResourceTypes,
                                                   int maxSimultaneousReservations, int maxAdvanceReservationDays,
                                                   BigDecimal discountRate) {
        return new MembershipPlan(
                planId, name, type, price, durationDays, allowedResourceTypes,
                maxSimultaneousReservations, maxAdvanceReservationDays, discountRate
        );
    }

    /**
     * 기본 플랜들을 생성하는 팩토리 메서드들
     * 
     * 미리 정의된 표준 플랜들을 쉽게 생성할 수 있도록 제공
     * 실제 서비스에서 자주 사용되는 플랜 템플릿
     */
    
    /**
     * 기본 월간권 - 헬스장과 스터디룸만 이용 가능
     */
    public static MembershipPlan basicMonthly() {
        return create(
                "BASIC_MONTHLY",
                "기본 월간권",
                PlanType.MONTHLY,
                Money.won(50000), // 5만원
                30, // 30일
                Set.of(ResourceType.GYM, ResourceType.STUDY_ROOM) // 기본 리소스 2개
        );
    }

    /**
     * 프리미엄 월간권 - 더 많은 리소스와 혜택
     */
    public static MembershipPlan premiumMonthly() {
        return createWithOptions(
                "PREMIUM_MONTHLY",
                "프리미엄 월간권",
                PlanType.MONTHLY,
                Money.won(100000), // 10만원
                30,
                // 프리미엄 리소스들
                Set.of(ResourceType.GYM, ResourceType.POOL, ResourceType.SAUNA,
                        ResourceType.STUDY_ROOM, ResourceType.MEETING_ROOM),
                5, // 동시 예약 5개
                45, // 45일 선예약
                new BigDecimal("0.1") // 10% 할인
        );
    }

    /**
     * VIP 연간권 - 모든 혜택 포함
     */
    public static MembershipPlan vipYearly() {
        return createWithOptions(
                "VIP_YEARLY",
                "VIP 연간권", 
                PlanType.YEARLY,
                Money.won(1000000), // 100만원
                365,
                // 모든 리소스 타입을 Set에 추가
                Arrays.stream(ResourceType.values()).collect(HashSet::new, Set::add, Set::addAll),
                10, // 동시 예약 10개
                90, // 90일 선예약
                new BigDecimal("0.2") // 20% 할인
        );
    }

    // =========================
    // 비즈니스 로직 메서드들
    // =========================

    /**
     * 특정 리소스 타입 이용 권한 확인
     * 
     * 가장 기본적인 권한 확인 메서드
     * 플랜이 비활성 상태면 권한 없음
     * 
     * @param resourceType 확인할 리소스 타입
     * @return 이용 권한이 있으면 true
     */
    public boolean hasPrivilege(ResourceType resourceType) {
        return isActive && allowedResourceTypes.contains(resourceType);
    }

    /**
     * 여러 리소스 타입에 대한 권한 확인
     * 
     * 복합 시설 이용이나 패키지 권한 확인 시 사용
     * 
     * @param resourceTypes 확인할 리소스 타입들
     * @return 모든 리소스에 대한 권한이 있으면 true
     */
    public boolean hasPrivileges(Set<ResourceType> resourceTypes) {
        // Set.containsAll(): 모든 요소를 포함하는지 확인
        return isActive && allowedResourceTypes.containsAll(resourceTypes);
    }

    /**
     * 지정된 예약 수가 허용 범위 내인지 확인
     * 
     * 동시 예약 제한 확인
     * 
     * @param currentReservationCount 현재 예약 수
     * @return 추가 예약이 가능하면 true
     */
    public boolean canReserve(int currentReservationCount) {
        return isActive && currentReservationCount < maxSimultaneousReservations;
    }

    /**
     * 지정된 예약 날짜가 선예약 허용 범위 내인지 확인
     * 
     * 플랜별로 다른 선예약 허용 기간 적용
     * 
     * @param daysFromToday 오늘로부터 몇 일 후인지
     * @return 선예약이 가능하면 true
     */
    public boolean canReserveInAdvance(int daysFromToday) {
        return isActive && daysFromToday <= maxAdvanceReservationDays;
    }

    /**
     * 일할 계산된 가격 반환 (남은 일수 기준)
     * 
     * 중도 해지, 부분 이용 시 일할 계산에 사용
     * 
     * @param remainingDays 남은 이용 일수
     * @return 일할 계산된 가격
     */
    public Money calculateProRatedPrice(int remainingDays) {
        if (remainingDays <= 0) {
            return Money.zero(price.getCurrency()); // 남은 일수가 없으면 0원
        }

        if (remainingDays >= durationDays) {
            return price; // 전체 기간보다 길면 전체 가격
        }

        // 비율 계산: remainingDays / durationDays
        BigDecimal ratio = BigDecimal.valueOf(remainingDays)
                .divide(BigDecimal.valueOf(durationDays), 4, RoundingMode.HALF_UP);
        
        // Money.multiply(): 가격에 비율 곱하기
        return price.multiply(ratio);
    }

    /**
     * 할인이 적용된 가격 계산
     * 
     * 실제 고객이 지불할 가격 (할인 반영)
     * 
     * @return 할인 적용된 최종 가격
     */
    public Money getDiscountedPrice() {
        if (discountRate.equals(BigDecimal.ZERO)) {
            return price; // 할인율이 0%면 원가 그대로
        }

        // 할인 적용: 원가 × (1 - 할인율)
        // 예: 100,000원 × (1 - 0.1) = 90,000원
        BigDecimal multiplier = BigDecimal.ONE.subtract(discountRate);
        return price.multiply(multiplier);
    }

    /**
     * 할인 금액 계산
     * 
     * 고객에게 보여줄 할인 혜택 금액
     * 
     * @return 할인으로 절약되는 금액
     */
    public Money getDiscountAmount() {
        return price.subtract(getDiscountedPrice()); // 원가 - 할인가
    }

    /**
     * 업그레이드 비용 계산 (다른 플랜과의 차액)
     * 
     * 플랜 변경 시 추가 지불해야 할 금액 계산
     * 
     * @param targetPlan 업그레이드할 대상 플랜
     * @param remainingDays 현재 플랜의 남은 일수
     * @return 추가 지불 금액 (음수면 환불)
     */
    public Money calculateUpgradeCost(MembershipPlan targetPlan, int remainingDays) {
        if (targetPlan == null) {
            throw new IllegalArgumentException("대상 플랜은 null일 수 없습니다");
        }

        // 현재 플랜의 남은 가치 (환불 받을 금액)
        Money currentRefund = this.calculateProRatedPrice(remainingDays);
        
        // 대상 플랜의 전체 비용
        Money targetCost = targetPlan.calculateProRatedPrice(targetPlan.durationDays);

        // 차액 계산: 대상 플랜 비용 - 현재 플랜 환불
        return targetCost.subtract(currentRefund);
    }

    /**
     * 플랜 비활성화
     * 
     * 더 이상 판매하지 않는 플랜으로 변경
     * 기존 가입자는 그대로 이용 가능하지만 신규 가입 불가
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 플랜 활성화
     * 
     * 판매 재개
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 플랜 등급 비교 (가격 기준)
     * 
     * 플랜의 상하위 관계를 가격으로 판단
     * Comparable 인터페이스를 구현하지 않고 별도 메서드로 제공
     * 
     * @param other 비교할 다른 플랜
     * @return 양수면 이 플랜이 더 비쌈, 0이면 같음, 음수면 더 쌈
     */
    public int compareTo(MembershipPlan other) {
        if (other == null) {
            return 1; // null보다는 크다고 간주
        }
        // Money.compareTo(): Money 클래스의 비교 메서드 활용
        return this.price.compareTo(other.price);
    }

    /**
     * 상위 플랜인지 확인
     * 
     * 업그레이드/다운그레이드 판단에 사용
     * 
     * @param other 비교할 플랜
     * @return 이 플랜이 더 비싸면 true
     */
    public boolean isHigherTierThan(MembershipPlan other) {
        return compareTo(other) > 0;
    }

    /**
     * 플랜 정보 요약
     * 
     * 관리자 화면이나 고객 선택 화면에서 보여줄 요약 정보
     * 
     * @return 플랜 요약 문자열
     */
    public String getSummary() {
        return String.format("%s (%s) - %s, %d일, 리소스 %d개, 동시예약 %d개",
                name, type.getDisplayName(), getDiscountedPrice(),
                durationDays, allowedResourceTypes.size(), maxSimultaneousReservations);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    /**
     * equals 메서드 오버라이드
     * 
     * 엔티티: 식별자(planId) 기준으로만 동일성 판단
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MembershipPlan that = (MembershipPlan) obj;
        return Objects.equals(planId, that.planId);
    }

    /**
     * hashCode 메서드 오버라이드
     * 
     * planId만 사용하여 해시코드 생성
     */
    @Override
    public int hashCode() {
        return Objects.hash(planId);
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 디버깅용 간결한 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("MembershipPlan{id='%s', name='%s', type=%s, price=%s, active=%s}",
                planId, name, type, price, isActive);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getPlanId() {
        return planId;
    }

    public String getName() {
        return name;
    }

    public PlanType getType() {
        return type;
    }

    public Money getPrice() {
        return price;
    }

    public int getDurationDays() {
        return durationDays;
    }

    /**
     * 허용된 리소스 타입들 반환
     * 
     * Set.copyOf(): 방어적 복사로 외부에서 수정 불가능한 불변 복사본 반환
     */
    public Set<ResourceType> getAllowedResourceTypes() {
        return Set.copyOf(allowedResourceTypes);
    }

    public int getMaxSimultaneousReservations() {
        return maxSimultaneousReservations;
    }

    public int getMaxAdvanceReservationDays() {
        return maxAdvanceReservationDays;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }

    /**
     * 양수 검증
     * 
     * 기간, 예약 수 등 0보다 커야 하는 값들 검증
     */
    private int validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "은 0보다 커야 합니다. 입력값: " + value);
        }
        return value;
    }

    /**
     * 0 이상 검증
     * 
     * 할인율 등 0도 허용하는 값들 검증
     */
    private int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "은 0 이상이어야 합니다. 입력값: " + value);
        }
        return value;
    }

    /**
     * 빈 집합 검증
     * 
     * 이용 가능 리소스가 최소 하나는 있어야 함
     */
    private <T> Set<T> validateNotEmpty(Set<T> set, String fieldName) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 집합일 수 없습니다");
        }
        return set;
    }

    /**
     * 할인율 유효성 검증
     * 
     * 0.0 이상 1.0 이하의 값만 허용
     * 
     * @param rate 할인율 (0.0 = 0%, 1.0 = 100%)
     * @return 유효한 할인율
     */
    private BigDecimal validateDiscountRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("할인율은 null일 수 없습니다");
        }
        
        // BigDecimal.compareTo(): 값 비교 (-1, 0, 1 반환)
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("할인율은 0.0 이상 1.0 이하여야 합니다. 입력값: " + rate);
        }
        return rate;
    }

    /**
     * 플랜 전체 일관성 검증
     * 
     * 필드 간의 논리적 일관성 확인
     * 생성자 마지막에 호출하여 전체적인 유효성 보장
     */
    private void validatePlanConsistency() {
        // 플랜 타입과 기간 일치 확인
        // PlanType.isValidDuration(): enum에 정의된 유효성 검증 활용
        if (!type.isValidDuration(durationDays)) {
            throw new IllegalArgumentException(
                    String.format("플랜 타입 %s에 적합하지 않은 기간입니다. (기간: %d일)", type, durationDays));
        }

        // 선예약 일수가 플랜 기간보다 과도하게 긴지 확인
        // 비즈니스 규칙: 선예약 일수는 플랜 기간의 3배를 넘을 수 없음
        if (maxAdvanceReservationDays > durationDays * 3) {
            throw new IllegalArgumentException("선예약 일수가 플랜 기간의 3배를 초과할 수 없습니다");
        }
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. 팩토리 메서드 패턴의 다층 구조

#### 기본 → 상세 → 프리셋
```java
// 1단계: 기본 생성 (필수 정보만)
MembershipPlan.create(id, name, type, price, days, resources);

// 2단계: 상세 생성 (모든 옵션 지정)  
MembershipPlan.createWithOptions(id, name, type, price, days, resources, 
                                maxReservations, advanceDays, discountRate);

// 3단계: 프리셋 생성 (미리 정의된 템플릿)
MembershipPlan.basicMonthly();
MembershipPlan.premiumMonthly();
```

### 2. 비즈니스 로직의 응집도

#### 가격 관련 로직 집중화
```java
Money originalPrice = plan.getPrice();           // 원가
Money discountedPrice = plan.getDiscountedPrice(); // 할인가
Money discountAmount = plan.getDiscountAmount();   // 할인 금액
Money proRatedPrice = plan.calculateProRatedPrice(15); // 15일 일할 계산
```

#### 권한 관련 로직 집중화
```java
boolean canUseGym = plan.hasPrivilege(ResourceType.GYM);
boolean canReserve = plan.canReserve(currentReservationCount);
boolean canAdvanceReserve = plan.canReserveInAdvance(daysFromToday);
```

### 3. 방어적 복사 (Defensive Copy)

#### Set 컬렉션의 불변성 보장
```java
// 생성자에서: 외부 Set 변경이 내부에 영향을 주지 않도록
this.allowedResourceTypes = Set.copyOf(allowedResourceTypes);

// Getter에서: 내부 Set을 외부에서 변경하지 못하도록  
public Set<ResourceType> getAllowedResourceTypes() {
    return Set.copyOf(allowedResourceTypes);
}
```

### 4. 정밀한 유효성 검증

#### 단계별 검증
1. **개별 필드 검증**: null, 빈 값, 범위 등
2. **비즈니스 규칙 검증**: 플랜 타입과 기간 일치 등
3. **전체 일관성 검증**: 필드 간 관계 확인

#### BigDecimal을 활용한 정확한 계산
```java
// 할인율 검증: 0.0 ~ 1.0 범위 확인
private BigDecimal validateDiscountRate(BigDecimal rate) {
    if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
        throw new IllegalArgumentException("할인율은 0.0 이상 1.0 이하여야 합니다");
    }
    return rate;
}

// 일할 계산: 정확한 소수점 처리
BigDecimal ratio = BigDecimal.valueOf(remainingDays)
    .divide(BigDecimal.valueOf(durationDays), 4, RoundingMode.HALF_UP);
```

### 5. 열거형과의 협력

#### PlanType과 ResourceType 활용
```java
// PlanType의 유효성 검증 메서드 활용
if (!type.isValidDuration(durationDays)) {
    throw new IllegalArgumentException("플랜 타입에 적합하지 않은 기간입니다");
}

// ResourceType의 집합 연산 활용
boolean hasPrivileges = allowedResourceTypes.containsAll(requiredResources);
```

### 6. 실제 사용 예시

#### 플랜 생성과 활용
```java
// 기본 플랜 생성
MembershipPlan basicPlan = MembershipPlan.basicMonthly();

// 권한 확인
boolean canUsePool = basicPlan.hasPrivilege(ResourceType.POOL); // false

// 가격 계산
Money originalPrice = basicPlan.getPrice();              // 50,000 KRW
Money discountedPrice = basicPlan.getDiscountedPrice(); // 50,000 KRW (할인 없음)

// 일할 계산 (15일만 이용)
Money proRated = basicPlan.calculateProRatedPrice(15);   // 25,000 KRW
```

#### 플랜 업그레이드
```java
MembershipPlan basicPlan = MembershipPlan.basicMonthly();
MembershipPlan premiumPlan = MembershipPlan.premiumMonthly();

// 업그레이드 비용 계산 (15일 남았을 때)
Money upgradeCost = basicPlan.calculateUpgradeCost(premiumPlan, 15);
// 계산: 프리미엄 전체 가격 - 베이직 15일 일할 계산 금액

// 플랜 등급 비교
boolean isHigherTier = premiumPlan.isHigherTierThan(basicPlan); // true
```

### 7. 설계상의 트레이드오프

#### 장점
- **응집도**: 플랜 관련 모든 로직이 한 곳에 집중
- **재사용성**: 다양한 플랜을 쉽게 생성할 수 있는 팩토리 메서드들
- **안전성**: 철저한 유효성 검증과 불변성 보장
- **확장성**: 새로운 플랜 타입이나 정책을 쉽게 추가

#### 고려사항
- **복잡성**: 많은 필드와 로직으로 인한 클래스 크기 증가
- **결합도**: 여러 열거형(PlanType, ResourceType)에 의존
- **변경 영향도**: 플랜 정책 변경 시 이 클래스의 수정 필요

### 8. 향후 확장 방안

#### 전략 패턴 도입 가능성
```java
// 할인 정책을 전략 패턴으로 분리
public interface DiscountStrategy {
    Money applyDiscount(Money originalPrice);
}

// 플랜에서 전략 사용
private final DiscountStrategy discountStrategy;
public Money getDiscountedPrice() {
    return discountStrategy.applyDiscount(price);
}
```

#### 플랜 정책의 외부화
```java
// 플랜 정책을 데이터로 관리 (DB, 설정 파일 등)
public class PlanPolicy {
    private Map<String, Object> policies;
    
    public boolean canReserve(int currentCount) {
        Integer maxCount = (Integer) policies.get("maxSimultaneousReservations");
        return currentCount < maxCount;
    }
}
```

### 9. 테스트 관점에서의 설계

#### 팩토리 메서드의 테스트 용이성
```java
@Test
void 기본_월간권_생성_테스트() {
    MembershipPlan plan = MembershipPlan.basicMonthly();
    
    assertThat(plan.getType()).isEqualTo(PlanType.MONTHLY);
    assertThat(plan.getDurationDays()).isEqualTo(30);
    assertThat(plan.hasPrivilege(ResourceType.GYM)).isTrue();
    assertThat(plan.hasPrivilege(ResourceType.POOL)).isFalse();
}
```

#### 비즈니스 로직의 단위 테스트
```java
@Test 
void 일할_계산_테스트() {
    MembershipPlan plan = MembershipPlan.basicMonthly(); // 50,000원, 30일
    
    Money halfPrice = plan.calculateProRatedPrice(15); // 15일
    assertThat(halfPrice).isEqualTo(Money.won(25000)); // 25,000원
}
```

이러한 설계로 MembershipPlan은 단순한 데이터 저장소가 아닌, 멤버십과 관련된 모든 비즈니스 규칙과 계산 로직을 캡슐화한 풍부한 도메인 객체가 되었습니다.