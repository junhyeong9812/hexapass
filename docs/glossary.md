# 도메인 용어집 (Domain Glossary)

> **중요**: 이 용어집의 모든 용어는 코드의 클래스명, 메서드명에 그대로 반영되어야 합니다.
> 도메인 전문가와 개발자 간의 의사소통을 위한 **유비쿼터스 언어(Ubiquitous Language)**입니다.

## 🏛️ 핵심 도메인 개념

### Member (회원)
- **정의**: 서비스를 이용하는 고객으로, 고유한 식별자와 멤버십을 가짐
- **속성**: memberId, name, email, phone, membershipPlan, status
- **상태**: ACTIVE(활성), SUSPENDED(정지), WITHDRAWN(탈퇴)
- **책임**: 예약 생성, 멤버십 관리, 개인정보 관리
- **코드 예시**: `Member.createReservation()`, `Member.changePlan()`

### MembershipPlan (멤버십 플랜)
- **정의**: 회원이 선택할 수 있는 구독 상품으로, 이용 권한과 혜택을 정의
- **타입**: MONTHLY(월간), YEARLY(연간), PERIOD(기간제)
- **속성**: planId, name, price, duration, privileges
- **권한**: 이용 가능한 리소스 타입, 동시 예약 수, 할인율
- **코드 예시**: `MembershipPlan.calculatePrice()`, `MembershipPlan.hasPrivilege()`

### Resource (리소스)
- **정의**: 예약 가능한 시설, 장비, 또는 서비스
- **타입**: ROOM(룸), EQUIPMENT(장비), CLASS(강의), COURT(코트)
- **속성**: resourceId, name, type, capacity, location, operatingHours
- **책임**: 예약 가능 여부 확인, 정원 관리
- **코드 예시**: `Resource.isAvailable()`, `Resource.checkCapacity()`

### Reservation (예약)
- **정의**: 특정 회원이 특정 시간에 특정 리소스를 이용하기 위한 예약
- **상태**: REQUESTED(요청), CONFIRMED(확정), IN_USE(사용중), COMPLETED(완료), CANCELLED(취소)
- **속성**: reservationId, memberId, resourceId, timeSlot, status, createdAt
- **불변식**: 예약 시간은 현재 시간 이후, 리소스 정원 범위 내
- **코드 예시**: `Reservation.confirm()`, `Reservation.cancel()`

## 💰 결제 및 할인 도메인

### Money (금액)
- **정의**: 통화와 금액을 함께 관리하는 값 객체
- **속성**: amount(BigDecimal), currency(Currency)
- **불변식**: amount ≥ 0, currency not null
- **연산**: add, subtract, multiply, divide
- **코드 예시**: `Money.of(10000, Currency.KRW)`, `money.add(otherMoney)`

### Payment (결제)
- **정의**: 실제 금전 거래를 나타내는 엔티티
- **타입**: CARD(카드), CASH(현금), POINT(포인트), BANK_TRANSFER(계좌이체)
- **상태**: PENDING(대기), SUCCESS(성공), FAILED(실패), REFUNDED(환불)
- **속성**: paymentId, amount, method, status, processedAt
- **코드 예시**: `Payment.process()`, `Payment.refund()`

### Discount (할인)
- **정의**: 결제 시 적용되는 할인 혜택
- **타입**: RATE(정률), AMOUNT(정액), COUPON(쿠폰)
- **속성**: discountId, type, value, conditions, validFrom, validTo
- **제약**: 정률 할인은 0-100%, 정액 할인은 양수
- **코드 예시**: `Discount.apply()`, `Discount.isValid()`

### DiscountPolicy (할인 정책)
- **정의**: 할인 적용 규칙을 캡슐화하는 전략 객체
- **구현**: RateDiscountPolicy, AmountDiscountPolicy, CouponDiscountPolicy
- **책임**: 할인 적용 가능 여부 판단, 할인 금액 계산
- **패턴**: Strategy Pattern 적용
- **코드 예시**: `discountPolicy.calculate(originalAmount)`

## 📅 시간 및 일정 도메인

### DateRange (날짜 범위)
- **정의**: 시작일과 종료일을 포함하는 기간을 나타내는 값 객체
- **속성**: startDate, endDate
- **불변식**: startDate ≤ endDate, null 불허
- **연산**: contains, overlaps, merge, split
- **코드 예시**: `DateRange.of(start, end)`, `range.overlaps(other)`

### TimeSlot (시간대)
- **정의**: 구체적인 시작시간과 종료시간을 가지는 예약 시간대
- **속성**: startTime(LocalDateTime), endTime(LocalDateTime)
- **불변식**: startTime < endTime, 동일 날짜
- **연산**: duration, overlaps, adjacent
- **코드 예시**: `TimeSlot.of(start, end)`, `timeSlot.getDuration()`

### Schedule (스케줄)
- **정의**: 리소스별 운영 시간과 예약 가능한 시간대를 정의
- **속성**: resourceId, operatingDays, timeSlots, holidays
- **책임**: 예약 가능 시간 확인, 휴일 처리
- **코드 예시**: `Schedule.getAvailableSlots()`, `Schedule.isHoliday()`

## 🎯 정책 및 규칙 도메인

### Policy (정책)
- **정의**: 비즈니스 규칙을 객체로 캡슐화한 추상 개념
- **하위 타입**: CancellationPolicy, ReservationPolicy, RefundPolicy
- **패턴**: Strategy Pattern, Specification Pattern
- **책임**: 조건 검증, 규칙 적용
- **코드 예시**: `policy.apply()`, `policy.isApplicable()`

### CancellationPolicy (취소 정책)
- **정의**: 예약 취소 시 적용되는 수수료와 조건을 정의
- **규칙**: 시간대별 차등 수수료 (24시간 전 무료, 이후 50% 등)
- **속성**: timeThresholds, feeRates, exceptions
- **코드 예시**: `cancellationPolicy.calculateFee(reservation, cancelTime)`

### ReservationPolicy (예약 정책)
- **정의**: 예약 생성 시 적용되는 제약 조건들
- **규칙**: 동시 예약 제한, 선예약 기간, 멤버십별 권한
- **조합**: Specification Pattern으로 복합 조건 표현
- **코드 예시**: `reservationPolicy.canReserve(member, resource, timeSlot)`

## 🔧 기술 도메인

### Specification (사양)
- **정의**: 비즈니스 규칙을 조합 가능한 객체로 표현하는 패턴
- **연산**: and(), or(), not()
- **목적**: 복잡한 조건을 명확하게 표현하고 재사용
- **구현**: `AndSpecification`, `OrSpecification`, `NotSpecification`
- **코드 예시**: `membershipSpec.and(timeSpec).and(capacitySpec)`

### Result<T> (결과)
- **정의**: 성공/실패를 명시적으로 표현하는 값 객체
- **상태**: SUCCESS(성공), FAILURE(실패)
- **속성**: value(T), error(ErrorCode), message
- **목적**: 예외 대신 명시적인 오류 처리
- **코드 예시**: `Result.success(reservation)`, `Result.failure(ErrorCode.CAPACITY_FULL)`

### ErrorCode (오류 코드)
- **정의**: 시스템에서 발생할 수 있는 오류를 분류한 열거형
- **분류**: VALIDATION, BUSINESS_RULE, SYSTEM_ERROR
- **구조**: 도메인별 프리픽스 + 순번 (MEMBER_001, RESERVATION_001)
- **코드 예시**: `MEMBER_EMAIL_DUPLICATE`, `RESERVATION_TIME_CONFLICT`

## 📊 애그리게잇과 경계

### Reservation Aggregate (예약 애그리게잇)
- **루트**: Reservation
- **구성**: Reservation, Payment, CancellationHistory
- **불변식**: 예약과 결제 상태 일치, 취소 이력 순서 보장
- **경계**: 예약 관련 모든 비즈니스 규칙을 내부에서 관리

### Member Aggregate (회원 애그리게잇)
- **루트**: Member
- **구성**: Member, MembershipPlan, ContactInfo
- **불변식**: 유효한 멤버십 보유, 연락처 정보 일치
- **경계**: 회원 정보와 멤버십 변경의 일관성 보장

## 🔄 상태 전이

### 예약 상태 전이
```
REQUESTED → CONFIRMED → IN_USE → COMPLETED
    ↓          ↓         ↓
  CANCELLED  CANCELLED  CANCELLED
```

### 결제 상태 전이
```
PENDING → SUCCESS → REFUNDED
   ↓
FAILED
```

### 회원 상태 전이
```
ACTIVE ⇄ SUSPENDED
   ↓
WITHDRAWN
```

## 📝 도메인 서비스

### ReservationService
- **목적**: 예약 생성 시 복잡한 비즈니스 규칙 조합
- **책임**: 시간 충돌 검사, 정원 확인, 권한 검증
- **협력**: Member, Resource, ReservationPolicy

### PricingService
- **목적**: 복합 할인 정책 적용과 최종 가격 계산
- **책임**: 할인 조합, 세금 계산, 반올림 처리
- **협력**: DiscountPolicy, Money, MembershipPlan

### NotificationService
- **목적**: 도메인 이벤트 발생 시 알림 처리
- **책임**: 예약 확정/취소 알림, 멤버십 만료 알림
- **패턴**: Observer Pattern, Domain Event

## 🎨 디자인 패턴 적용

### Strategy Pattern (전략 패턴)
- **적용**: DiscountPolicy, CancellationPolicy
- **이점**: 정책 변경 시 기존 코드 수정 없음
- **예시**: `context.setPolicy(newDiscountPolicy)`

### Decorator Pattern (데코레이터 패턴)
- **적용**: 다중 할인 조합
- **이점**: 런타임에 할인 조합 변경 가능
- **예시**: `new CouponDiscount(new MembershipDiscount(basePrice))`

### State Pattern (상태 패턴)
- **적용**: Reservation 상태별 행위 변경
- **이점**: 상태별 로직 캡슐화, 새로운 상태 추가 용이
- **예시**: `reservation.getState().cancel()`

### Specification Pattern (사양 패턴)
- **적용**: 복합 예약 조건 검증
- **이점**: 비즈니스 규칙의 명시적 표현과 조합
- **예시**: `membershipSpec.and(timeSpec).isSatisfiedBy(request)`

---

**활용법**:
- 코드 리뷰 시 이 용어집 참조
- 새로운 기능 추가 시 용어 정의 먼저 확인
- 도메인 전문가와 소통 시 공통 언어로 사용
- 클래스/메서드명 작성 시 용어집의 영문 표현 활용