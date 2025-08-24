# Enum 클래스들 - 상세 주석 및 설명

## Enum을 사용하는 이유
1. **타입 안전성**: 정해진 값만 사용 가능 (컴파일 타임 검증)
2. **가독성**: 의미있는 상수명으로 코드 이해도 향상
3. **유지보수성**: 새로운 상태 추가 시 컴파일러가 누락된 case 감지
4. **성능**: JVM에서 최적화된 구현 제공

---

## MemberStatus.java - 회원 상태

### 클래스 개요
회원의 생명주기 상태를 나타내는 열거형으로, 각 상태별 비즈니스 규칙과 상태 전환 로직을 포함합니다.

### 상세 주석이 추가된 코드

```java
package com.hexapass.domain.type;

/**
 * 회원 상태를 나타내는 열거형
 * 
 * Enum 사용 이유:
 * 1. 타입 안전성: String 대신 강타입 사용
 * 2. 상태 전환 로직 집중화
 * 3. 비즈니스 규칙 명확화
 * 4. IDE 자동완성 지원
 */
public enum MemberStatus {
    // 각 상수는 생성자 파라미터로 displayName과 description을 받음
    ACTIVE("활성", "정상적으로 서비스를 이용할 수 있는 상태"),
    SUSPENDED("정지", "일시적으로 서비스 이용이 제한된 상태"), 
    WITHDRAWN("탈퇴", "서비스에서 탈퇴한 상태");

    // 사용자에게 보여질 한글 이름
    private final String displayName;
    // 상태에 대한 자세한 설명
    private final String description;

    /**
     * Enum 생성자는 항상 private (명시하지 않아도 자동으로 private)
     * 각 상수 정의 시 호출됨
     * 
     * @param displayName 화면에 표시될 이름
     * @param description 상태에 대한 설명
     */
    MemberStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 화면 표시용 이름 반환
     * 
     * 다국어 지원 시 이 메서드를 확장하여 MessageSource 활용 가능
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 상태 설명 반환
     */
    public String getDescription() {
        return description;
    }

    /**
     * 활성 상태인지 확인
     * 
     * 비즈니스 로직을 enum에 포함시켜 중복 코드 제거
     * if (member.getStatus() == MemberStatus.ACTIVE) 대신
     * if (member.getStatus().isActive()) 사용 가능
     */
    public boolean isActive() {
        return this == ACTIVE; // == 비교: enum은 싱글톤이므로 안전
    }

    /**
     * 서비스 이용 가능한 상태인지 확인
     * 
     * 현재는 ACTIVE만 서비스 이용 가능하지만,
     * 향후 TRIAL(체험) 상태 등이 추가되면 여기서 확장
     */
    public boolean canUseService() {
        return this == ACTIVE;
    }

    /**
     * 다른 상태로 전환 가능한지 확인
     * 
     * 상태 전환 매트릭스를 코드로 표현
     * 비즈니스 규칙: 탈퇴한 회원은 복구 불가
     * 
     * @param newStatus 전환하려는 새로운 상태
     * @return 전환 가능 여부
     */
    public boolean canTransitionTo(MemberStatus newStatus) {
        // switch 표현식 (Java 14+) 또는 switch 문 사용
        switch (this) {
            case ACTIVE:
                // 활성 상태에서는 정지나 탈퇴로만 전환 가능
                return newStatus == SUSPENDED || newStatus == WITHDRAWN;
            case SUSPENDED:
                // 정지 상태에서는 활성화나 탈퇴로 전환 가능
                return newStatus == ACTIVE || newStatus == WITHDRAWN;
            case WITHDRAWN:
                // 탈퇴 상태에서는 다른 상태로 전환 불가 (비즈니스 규칙)
                return false;
            default:
                // 예상하지 못한 상태가 추가된 경우 안전하게 false 반환
                return false;
        }
    }
}
```

---

## PlanType.java - 멤버십 플랜 타입

### 클래스 개요
멤버십 플랜의 유형을 나타내는 열거형으로, 각 플랜별 기본 기간과 유효성 검증 로직을 포함합니다.

### 상세 주석이 추가된 코드

```java
package com.hexapass.domain.type;

/**
 * 멤버십 플랜 타입을 나타내는 열거형
 * 
 * 각 플랜 타입별로 기본 기간과 유효성 검증 로직을 포함
 * 확장성을 고려하여 설계 (향후 WEEKLY, LIFETIME 등 추가 가능)
 */
public enum PlanType {
    // 각 상수는 (표시명, 기본일수, 설명) 파라미터로 생성
    MONTHLY("월간권", 30, "한 달 단위로 자동 갱신되는 멤버십"),
    YEARLY("연간권", 365, "일 년 단위로 갱신되는 멤버십"),
    PERIOD("기간제", 0, "특정 기간 동안만 유효한 멤버십"); // 기간은 별도 지정

    private final String displayName;  // 사용자에게 보여질 이름
    private final int defaultDays;     // 기본 유효 기간 (일 단위)
    private final String description;  // 플랜에 대한 설명

    /**
     * PlanType 생성자
     * 
     * @param displayName 화면 표시명
     * @param defaultDays 기본 기간 (일), 0이면 별도 지정 필요
     * @param description 플랜 설명
     */
    PlanType(String displayName, int defaultDays, String description) {
        this.displayName = displayName;
        this.defaultDays = defaultDays;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 기본 유효 기간 반환
     * 
     * PERIOD 타입의 경우 0을 반환하므로 별도 기간 설정 필요
     */
    public int getDefaultDays() {
        return defaultDays;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 자동 갱신 타입인지 확인
     * 
     * 비즈니스 로직: MONTHLY, YEARLY는 만료 시 자동 갱신
     * PERIOD는 수동 갱신 (일회성)
     */
    public boolean isAutoRenewable() {
        return this == MONTHLY || this == YEARLY;
    }

    /**
     * 기간제 타입인지 확인
     * 
     * 기간제는 특별한 처리가 필요한 경우가 많음
     * (환불 정책, 갱신 정책 등이 다름)
     */
    public boolean isPeriodType() {
        return this == PERIOD;
    }

    /**
     * 지정된 기간이 이 플랜 타입에 적합한지 확인
     * 
     * 유효성 검증 로직을 enum에 포함시켜 
     * 플랜 생성 시 일관된 검증 보장
     * 
     * @param days 검증할 기간 (일 수)
     * @return 유효성 여부
     */
    public boolean isValidDuration(int days) {
        if (days <= 0) {
            return false; // 모든 플랜에서 0일 이하는 무효
        }

        switch (this) {
            case MONTHLY:
                // 한 달 범위: 28일(2월)~31일(긴 달) 허용
                return days >= 28 && days <= 31;
            case YEARLY:
                // 일 년 범위: 365일(평년)~366일(윤년) 허용
                return days >= 365 && days <= 366;
            case PERIOD:
                // 기간제는 1일 이상이면 모두 허용 (유연성 제공)
                return days >= 1;
            default:
                // 새로운 타입이 추가되었지만 아직 검증 로직이 없는 경우
                return false;
        }
    }
}
```

---

## ReservationStatus.java - 예약 상태

### 클래스 개요
예약의 생명주기를 나타내는 열거형으로, 상태별 비즈니스 규칙과 상태 전환 로직을 포함합니다.

### 상세 주석이 추가된 코드

```java
package com.hexapass.domain.type;

/**
 * 예약 상태를 나타내는 열거형
 * 
 * 예약의 전체 생명주기를 표현:
 * REQUESTED → CONFIRMED → IN_USE → COMPLETED
 *     ↓           ↓          ↓
 * CANCELLED   CANCELLED   CANCELLED
 */
public enum ReservationStatus {
    REQUESTED("예약요청", "예약이 요청되었지만 아직 확정되지 않은 상태"),
    CONFIRMED("예약확정", "예약이 확정되어 이용 가능한 상태"),
    IN_USE("사용중", "현재 예약된 리소스를 사용하고 있는 상태"),
    COMPLETED("사용완료", "예약된 서비스 이용이 정상적으로 완료된 상태"),
    CANCELLED("예약취소", "예약이 취소된 상태");

    private final String displayName;
    private final String description;

    /**
     * ReservationStatus 생성자
     */
    ReservationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 활성 예약 상태인지 확인 (취소되지 않고 아직 완료되지 않은 상태)
     * 
     * 활성 예약: 실제 리소스를 점유하고 있는 상태
     * 스케줄링, 충돌 검사 등에서 활용
     */
    public boolean isActive() {
        return this == CONFIRMED || this == IN_USE;
    }

    /**
     * 최종 상태인지 확인 (더 이상 변경될 수 없는 상태)
     * 
     * 최종 상태: 비즈니스 프로세스가 완료된 상태
     * 이벤트 발행, 알림 발송 등의 트리거로 활용
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 취소 가능한 상태인지 확인
     * 
     * 비즈니스 규칙: 완료된 예약은 취소 불가
     * UI에서 취소 버튼 표시 여부 결정에 활용
     */
    public boolean isCancellable() {
        return this == REQUESTED || this == CONFIRMED || this == IN_USE;
    }

    /**
     * 다른 상태로 전환 가능한지 확인
     * 
     * 상태 전환 규칙을 코드로 명시화
     * 잘못된 상태 전환 시도를 컴파일 타임에 방지
     * 
     * @param newStatus 전환하려는 새로운 상태
     * @return 전환 가능 여부
     */
    public boolean canTransitionTo(ReservationStatus newStatus) {
        switch (this) {
            case REQUESTED:
                // 요청 상태에서는 확정 또는 취소만 가능
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                // 확정 상태에서는 사용 시작 또는 취소 가능
                return newStatus == IN_USE || newStatus == CANCELLED;
            case IN_USE:
                // 사용중에는 완료 또는 취소 가능 (긴급 상황 대비)
                return newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED:
            case CANCELLED:
                // 최종 상태에서는 다른 상태로 전환 불가
                return false;
            default:
                return false;
        }
    }
}
```

---

## ResourceType.java - 리소스 타입

### 클래스 개요
예약 가능한 리소스의 분류를 나타내는 열거형으로, 리소스별 특성과 그룹화 로직을 포함합니다.

### 상세 주석이 추가된 코드

```java
package com.hexapass.domain.type;

/**
 * 예약 가능한 리소스 타입을 나타내는 열거형
 * 
 * 다양한 비즈니스 도메인의 예약 가능한 자원을 분류
 * 각 타입별로 특성에 따른 그룹화 메서드 제공
 */
public enum ResourceType {
    // === 헬스장 관련 ===
    GYM("헬스장", "헬스 기구를 이용할 수 있는 공간"),
    POOL("수영장", "수영을 할 수 있는 풀장"),
    SAUNA("사우나", "사우나 시설"),

    // === 스터디/업무 공간 ===
    STUDY_ROOM("스터디룸", "개인 또는 그룹 스터디용 룸"),
    MEETING_ROOM("회의실", "회의나 미팅용 공간"),
    OFFICE_DESK("오피스 데스크", "개인 업무용 데스크"),

    // === 스포츠 시설 ===
    TENNIS_COURT("테니스 코트", "테니스 경기용 코트"),
    BADMINTON_COURT("배드민턴 코트", "배드민턴 경기용 코트"),
    BASKETBALL_COURT("농구 코트", "농구 경기용 코트"),

    // === 교육/강의 ===
    CLASS_ROOM("강의실", "강의나 교육용 공간"),
    SEMINAR_ROOM("세미나실", "세미나나 워크샵용 공간"),

    // === 기타 ===
    PARKING_SPACE("주차공간", "차량 주차용 공간");

    private final String displayName;
    private final String description;

    /**
     * ResourceType 생성자
     */
    ResourceType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 헬스장 관련 리소스인지 확인
     * 
     * 그룹화 메서드: 비슷한 특성의 리소스들을 묶어서 처리
     * 예: 헬스장 멤버십으로 이용 가능한 시설들
     */
    public boolean isFitnessRelated() {
        return this == GYM || this == POOL || this == SAUNA;
    }

    /**
     * 스터디/업무 공간인지 확인
     * 
     * 업무용 리소스: 조용함, 인터넷 필요, 긴 이용시간 등의 공통 특성
     */
    public boolean isWorkspaceRelated() {
        return this == STUDY_ROOM || this == MEETING_ROOM || this == OFFICE_DESK;
    }

    /**
     * 스포츠 시설인지 확인
     * 
     * 스포츠 시설: 장비 필요, 복장 규정, 안전 규칙 등의 공통 특성
     */
    public boolean isSportsRelated() {
        return this == TENNIS_COURT || this == BADMINTON_COURT || this == BASKETBALL_COURT;
    }

    /**
     * 교육 시설인지 확인
     * 
     * 교육 시설: 프로젝터, 화이트보드, 좌석 배치 등의 공통 특성
     */
    public boolean isEducationRelated() {
        return this == CLASS_ROOM || this == SEMINAR_ROOM;
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. Enum 활용 패턴들

#### 상태 머신 (State Machine)
```java
// 예약 상태 전환 예시
public void changeStatus(ReservationStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus)) {
        throw new IllegalStateException(
            "Cannot transition from " + this.status + " to " + newStatus);
    }
    this.status = newStatus;
}
```

#### 전략 패턴과의 결합
```java
// PlanType별 다른 계산 로직
public Money calculatePrice(int days) {
    switch (this.planType) {
        case MONTHLY:
            return Money.won(30000);
        case YEARLY: 
            return Money.won(300000).multiply(0.9); // 10% 할인
        case PERIOD:
            return Money.won(1000).multiply(days);
    }
}
```

### 2. 비즈니스 규칙의 명시화

#### 상태 전환 매트릭스
각 enum에서 `canTransitionTo()` 메서드로 허용되는 상태 변경을 명시적으로 정의

#### 분류 로직
ResourceType의 `isFitnessRelated()` 등으로 리소스별 특성 그룹화

### 3. 확장성 고려사항

#### 새로운 상태 추가
```java
// 새로운 회원 상태 추가 시
public enum MemberStatus {
    ACTIVE, SUSPENDED, WITHDRAWN, 
    TRIAL("체험", "체험 기간 중인 상태"); // 새 상태 추가
    
    // canUseService() 메서드도 함께 수정 필요
    public boolean canUseService() {
        return this == ACTIVE || this == TRIAL; // TRIAL 추가
    }
}
```

### 4. 다국어 지원 준비
```java
// 향후 다국어 지원 시 확장 가능한 구조
public String getDisplayName(Locale locale) {
    return messageSource.getMessage(
        "member.status." + this.name().toLowerCase(), 
        null, locale);
}
```

### 5. Enum 사용의 장점 정리

#### 컴파일 타임 안전성
- 잘못된 값 사용 시 컴파일 오류 발생
- IDE의 자동완성과 타입 검사 지원

#### 성능
- JVM에서 최적화된 구현 (싱글톤, switch 최적화)
- 문자열 비교보다 빠른 == 연산

#### 유지보수성
- 새로운 상태 추가 시 누락된 case를 컴파일러가 감지
- 중앙 집중화된 상태 관리

이러한 설계로 Enum 클래스들은 단순한 상수가 아닌, 비즈니스 로직을 포함한 풍부한 도메인 객체가 되었습니다.