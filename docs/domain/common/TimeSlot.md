# TimeSlot.java - 상세 주석 및 설명

## 클래스 개요
`TimeSlot`은 구체적인 시작시간과 종료시간을 가지는 시간대를 나타내는 **값 객체(Value Object)**입니다.
불변 객체로 설계되어 생성 후 상태 변경이 불가능하며, 같은 날짜 내의 시간대만 허용하고 시작시간 < 종료시간을 보장합니다.

## 왜 이런 클래스가 필요한가?
1. **예약 시스템의 핵심**: 시설 예약 시간을 명확하게 표현
2. **시간 충돌 방지**: 겹치는 예약 시간대를 쉽게 감지
3. **타입 안전성**: `LocalDateTime start, LocalDateTime end` 대신 `TimeSlot` 사용
4. **도메인 로직 집중**: 시간 관련 비즈니스 로직을 한 곳에 모음

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.common;

import java.time.Duration; // 시간 간격을 나타내는 클래스 (ISO-8601 기준)
import java.time.LocalDateTime; // 날짜와 시간을 함께 나타내는 불변 클래스
import java.time.format.DateTimeFormatter; // 날짜/시간 형식 지정을 위한 클래스
import java.util.Objects; // equals, hashCode 등 유틸리티 메서드

/**
 * 구체적인 시작시간과 종료시간을 가지는 시간대를 나타내는 값 객체
 * 불변 객체로 설계되어 생성 후 상태 변경 불가
 * 같은 날짜 내의 시간대만 허용하며 시작시간 < 종료시간 보장
 * 
 * 값 객체 특징:
 * 1. 불변성: 생성 후 상태 변경 불가
 * 2. 동등성: 값이 같으면 같은 객체로 취급
 * 3. 자가 검증: 생성 시 유효성 보장
 */
public final class TimeSlot { // final: 상속 불가

    private final LocalDateTime startTime; // 시작시간 (불변)
    private final LocalDateTime endTime;   // 종료시간 (불변)

    // 출력용 날짜/시간 포맷터 (정적 상수)
    // "yyyy-MM-dd HH:mm" 형태로 표시 (예: 2024-01-15 14:30)
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * private 생성자 사용 이유:
     * 1. 객체 생성 방법 제어
     * 2. 모든 생성에서 유효성 검사 강제
     * 3. 팩토리 메서드로 생성 의도 명확화
     */
    private TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = validateNotNull(startTime, "시작시간");
        this.endTime = validateNotNull(endTime, "종료시간");
        validateTimeOrder(startTime, endTime);    // 시간 순서 검증
        validateSameDate(startTime, endTime);     // 같은 날짜 검증
    }

    /**
     * 시작시간과 종료시간을 지정하여 TimeSlot 생성
     * 
     * 기본 팩토리 메서드
     */
    public static TimeSlot of(LocalDateTime startTime, LocalDateTime endTime) {
        return new TimeSlot(startTime, endTime);
    }

    /**
     * 시작시간과 지속시간(분)으로 TimeSlot 생성
     * 
     * 편의 메서드: 회의 1시간, 수업 90분 등 지속시간으로 생성
     */
    public static TimeSlot ofDuration(LocalDateTime startTime, int durationMinutes) {
        if (startTime == null) {
            throw new IllegalArgumentException("시작시간은 null일 수 없습니다");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("지속시간은 0보다 커야 합니다. 입력값: " + durationMinutes);
        }

        // plusMinutes(): 지정된 분만큼 더한 새로운 LocalDateTime 반환
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        return new TimeSlot(startTime, endTime);
    }

    /**
     * 1시간 단위 TimeSlot 생성 편의 메서드
     * 
     * 가장 일반적인 예약 단위 (60분)
     */
    public static TimeSlot oneHour(LocalDateTime startTime) {
        return ofDuration(startTime, 60);
    }

    /**
     * 30분 단위 TimeSlot 생성 편의 메서드
     * 
     * 짧은 상담, 간단한 미팅 등에 활용
     */
    public static TimeSlot halfHour(LocalDateTime startTime) {
        return ofDuration(startTime, 30);
    }

    // =========================
    // 시간대 관계 확인 메서드들
    // =========================

    /**
     * 다른 시간대와 겹치는지 확인
     * 
     * 겹침 조건: A.start < B.end && B.start < A.end
     * 
     * 예시:
     * A: [09:00 ~ 11:00], B: [10:00 ~ 12:00] -> 겹침 (10:00~11:00)
     * A: [09:00 ~ 10:00], B: [10:00 ~ 11:00] -> 겹치지 않음 (끝점이 시작점과 만남)
     */
    public boolean overlaps(TimeSlot other) {
        if (other == null) {
            return false;
        }

        // isBefore(): < 비교 (같으면 false)
        // 논리: 이 시간대의 시작이 다른 시간대의 끝보다 이전이고,
        //      다른 시간대의 시작이 이 시간대의 끝보다 이전이면 겹침
        return this.startTime.isBefore(other.endTime) &&
                other.startTime.isBefore(this.endTime);
    }

    /**
     * 다른 시간대와 인접한지 확인 (바로 이어지는지)
     * 
     * 인접 조건: 한 시간대의 끝 = 다른 시간대의 시작
     * 예: [09:00~10:00]과 [10:00~11:00]은 인접
     */
    public boolean isAdjacent(TimeSlot other) {
        if (other == null) {
            return false;
        }

        // equals(): LocalDateTime은 값 객체이므로 값 비교
        return this.endTime.equals(other.startTime) ||
                other.endTime.equals(this.startTime);
    }

    /**
     * 지정된 시간이 이 시간대에 포함되는지 확인 (시작시간 포함, 종료시간 미포함)
     * 
     * 구간 표현: [startTime, endTime) - 반열린구간
     * 이유: 예약 시간이 정확히 맞닿을 때 중복을 방지
     * 예: [09:00~10:00)과 [10:00~11:00)은 겹치지 않음
     */
    public boolean contains(LocalDateTime time) {
        if (time == null) {
            return false;
        }

        // !isBefore(): >= 의 의미
        // isBefore(): < 의 의미 (같으면 false)
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    /**
     * 다른 시간대가 이 시간대에 완전히 포함되는지 확인
     * 
     * 포함 조건: 다른 시간대가 이 시간대 내부에 완전히 들어감
     */
    public boolean contains(TimeSlot other) {
        if (other == null) {
            return false;
        }

        // 다른 시간대의 시작이 이 시간대의 시작보다 늦거나 같고,
        // 다른 시간대의 끝이 이 시간대의 끝보다 이르거나 같으면 포함
        return !other.startTime.isBefore(this.startTime) &&
                !other.endTime.isAfter(this.endTime);
    }

    /**
     * 이 시간대가 다른 시간대 이전인지 확인
     * 
     * 순서 비교: 이 시간대의 끝이 다른 시간대의 시작보다 이전
     */
    public boolean isBefore(TimeSlot other) {
        return other != null && this.endTime.isBefore(other.startTime);
    }

    /**
     * 이 시간대가 다른 시간대 이후인지 확인
     */
    public boolean isAfter(TimeSlot other) {
        return other != null && this.startTime.isAfter(other.endTime);
    }

    // =========================
    // 시간대 정보 메서드들
    // =========================

    /**
     * 시간대의 지속시간 계산
     * 
     * Duration 클래스:
     * - ISO-8601 기준 시간 간격 표현
     * - 나노초 단위의 정확성
     * - 시간 계산 메서드 제공 (toMinutes, toHours 등)
     */
    public Duration getDuration() {
        return Duration.between(startTime, endTime); // 두 시간 사이의 Duration 생성
    }

    /**
     * 지속시간을 분 단위로 반환
     * 
     * 예약 시스템에서 가장 일반적인 시간 단위
     */
    public long getDurationMinutes() {
        return getDuration().toMinutes(); // Duration을 분으로 변환
    }

    /**
     * 지속시간을 시간 단위로 반환
     */
    public long getDurationHours() {
        return getDuration().toHours(); // Duration을 시간으로 변환
    }

    /**
     * 현재 시간 기준으로 이 시간대가 과거인지 확인
     * 
     * 과거 판단: 종료시간이 현재시간보다 이전
     */
    public boolean isPast() {
        return endTime.isBefore(LocalDateTime.now());
    }

    /**
     * 현재 시간 기준으로 이 시간대가 미래인지 확인
     * 
     * 미래 판단: 시작시간이 현재시간보다 이후
     */
    public boolean isFuture() {
        return startTime.isAfter(LocalDateTime.now());
    }

    /**
     * 현재 시간이 이 시간대에 포함되는지 확인 (현재 진행중)
     * 
     * 진행중 판단: 현재시간이 시간대에 포함됨
     */
    public boolean isCurrent() {
        LocalDateTime now = LocalDateTime.now();
        return contains(now); // contains 메서드 재사용
    }

    /**
     * 오늘의 시간대인지 확인
     * 
     * 날짜 추출: LocalDateTime에서 LocalDate만 추출하여 비교
     */
    public boolean isToday() {
        return startTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    // =========================
    // 시간대 변환 메서드들
    // =========================

    /**
     * 시작시간을 변경한 새로운 TimeSlot 반환
     * 
     * 불변 객체: 기존 객체는 변경하지 않고 새 객체 반환
     * "withXxx" 네이밍: 불변 객체에서 값 변경 시 사용하는 관례
     */
    public TimeSlot withStartTime(LocalDateTime newStartTime) {
        return new TimeSlot(newStartTime, this.endTime);
    }

    /**
     * 종료시간을 변경한 새로운 TimeSlot 반환
     */
    public TimeSlot withEndTime(LocalDateTime newEndTime) {
        return new TimeSlot(this.startTime, newEndTime);
    }

    /**
     * 지정된 분만큼 시간대를 이동한 새로운 TimeSlot 반환
     * 
     * 이동: 시작시간과 종료시간을 동일하게 이동 (지속시간 유지)
     * 양수: 미래로 이동, 음수: 과거로 이동
     */
    public TimeSlot moveBy(int minutes) {
        LocalDateTime newStartTime = this.startTime.plusMinutes(minutes);
        LocalDateTime newEndTime = this.endTime.plusMinutes(minutes);
        return new TimeSlot(newStartTime, newEndTime);
    }

    /**
     * 지정된 분만큼 시간대를 연장한 새로운 TimeSlot 반환
     * 
     * 연장: 시작시간은 유지하고 종료시간만 늘림
     */
    public TimeSlot extend(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("연장 시간은 0 이상이어야 합니다. 입력값: " + minutes);
        }

        LocalDateTime newEndTime = this.endTime.plusMinutes(minutes);
        return new TimeSlot(this.startTime, newEndTime);
    }

    /**
     * 지정된 분만큼 시간대를 단축한 새로운 TimeSlot 반환
     * 
     * 단축: 시작시간은 유지하고 종료시간만 당김
     */
    public TimeSlot shorten(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("단축 시간은 0 이상이어야 합니다. 입력값: " + minutes);
        }

        LocalDateTime newEndTime = this.endTime.minusMinutes(minutes);
        // 단축 후에도 시작시간 < 종료시간 조건 유지
        if (newEndTime.isBefore(this.startTime) || newEndTime.equals(this.startTime)) {
            throw new IllegalArgumentException("단축 후 종료시간이 시작시간보다 이전이거나 같을 수 없습니다");
        }

        return new TimeSlot(this.startTime, newEndTime);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    /**
     * equals 메서드 오버라이드
     * 
     * 값 객체: 시작시간과 종료시간이 모두 같으면 같은 객체
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // 같은 참조
        if (obj == null || getClass() != obj.getClass()) return false;

        TimeSlot timeSlot = (TimeSlot) obj;
        // LocalDateTime.equals(): 값 기반 동등성 비교
        return Objects.equals(startTime, timeSlot.startTime) &&
                Objects.equals(endTime, timeSlot.endTime);
    }

    /**
     * hashCode 메서드 오버라이드
     * 
     * equals/hashCode 계약 준수
     */
    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 사용자 친화적 문자열 표현
     * 예: "2024-01-15 09:00 ~ 2024-01-15 10:00 (60분)"
     */
    @Override
    public String toString() {
        return String.format("%s ~ %s (%d분)",
                startTime.format(DISPLAY_FORMATTER),  // 포맷터 사용
                endTime.format(DISPLAY_FORMATTER),
                getDurationMinutes());                // 지속시간 표시
    }

    // =========================
    // Getter 메서드들
    // =========================

    /**
     * 시작시간 반환
     * 
     * LocalDateTime은 불변 객체이므로 방어적 복사 불필요
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * 종료시간 반환
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    /**
     * null 체크 및 유효성 검사
     * 
     * 방어적 프로그래밍: 잘못된 입력에 대한 빠른 실패
     */
    private LocalDateTime validateNotNull(LocalDateTime time, String fieldName) {
        if (time == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return time; // 메서드 체이닝을 위한 원본 반환
    }

    /**
     * 시간 순서 검증
     * 
     * 불변식(Invariant) 보장: 시작시간 < 종료시간
     * 같은 시간은 허용하지 않음 (0분 시간대는 의미 없음)
     */
    private void validateTimeOrder(LocalDateTime startTime, LocalDateTime endTime) {
        // isBefore(): < 비교 (같으면 false)
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    String.format("시작시간은 종료시간보다 이전이어야 합니다. (시작시간: %s, 종료시간: %s)",
                            startTime, endTime));
        }
    }

    /**
     * 같은 날짜 검증
     * 
     * 비즈니스 규칙: 하루를 넘나드는 시간대는 허용하지 않음
     * 이유: 예약 시스템에서 자정을 넘는 예약은 복잡성을 증가시킴
     */
    private void validateSameDate(LocalDateTime startTime, LocalDateTime endTime) {
        // toLocalDate(): LocalDateTime에서 날짜 부분만 추출
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new IllegalArgumentException(
                    String.format("시작시간과 종료시간은 같은 날짜여야 합니다. (시작일: %s, 종료일: %s)",
                            startTime.toLocalDate(), endTime.toLocalDate()));
        }
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. 사용된 Java 8+ Time API

#### LocalDateTime
- 시간대(TimeZone) 정보가 없는 날짜+시간
- 불변 객체로 스레드 안전
- `isBefore()`, `isAfter()`, `equals()`: 시간 비교
- `plusMinutes()`, `minusMinutes()`: 시간 계산 (새 객체 반환)
- `toLocalDate()`: 날짜 부분만 추출

#### Duration
- 두 시간점 사이의 간격을 나타냄
- 나노초 단위의 정확성
- `toMinutes()`, `toHours()`: 단위 변환
- `between()`: 두 LocalDateTime 사이의 Duration 계산

#### DateTimeFormatter
- 날짜/시간 형식 지정
- `ofPattern()`: 사용자 정의 패턴
- 스레드 안전 (불변 객체)

### 2. 시간 겹침 알고리즘
```java
// 두 구간이 겹치는 조건
// A: [start1, end1], B: [start2, end2]
boolean overlaps = start1 < end2 && start2 < end1;

// 실제 구현에서는 isBefore() 사용
boolean overlaps = this.startTime.isBefore(other.endTime) &&
                   other.startTime.isBefore(this.endTime);
```

### 3. 반열린구간 [start, end) 사용 이유
```java
TimeSlot slot1 = TimeSlot.of(
    LocalDateTime.of(2024, 1, 15, 9, 0),   // 09:00
    LocalDateTime.of(2024, 1, 15, 10, 0)   // 10:00
);

TimeSlot slot2 = TimeSlot.of(
    LocalDateTime.of(2024, 1, 15, 10, 0),  // 10:00
    LocalDateTime.of(2024, 1, 15, 11, 0)   // 11:00
);

// 겹치지 않음 - 첫 번째 예약이 10:00에 끝나고 두 번째가 10:00에 시작
boolean overlaps = slot1.overlaps(slot2); // false
```

### 4. 불변 객체의 "withXxx" 메서드 패턴
```java
TimeSlot original = TimeSlot.oneHour(LocalDateTime.of(2024, 1, 15, 9, 0));
// 시작시간만 변경한 새로운 객체
TimeSlot modified = original.withStartTime(LocalDateTime.of(2024, 1, 15, 10, 0));

// original은 변경되지 않음
System.out.println(original);  // 2024-01-15 09:00 ~ 2024-01-15 10:00 (60분)
System.out.println(modified);  // 2024-01-15 10:00 ~ 2024-01-15 10:00 (60분) - 잘못된 예시
```

### 5. 예외 처리 전략

#### 생성 시점 검증
- 모든 잘못된 상태를 생성 시점에서 차단
- 한번 생성되면 항상 유효한 상태 보장

#### 의미있는 예외 메시지
```java
// 나쁜 예
throw new IllegalArgumentException("Invalid input");

// 좋은 예  
throw new IllegalArgumentException(
    "시작시간은 종료시간보다 이전이어야 합니다. (시작시간: " + startTime + ", 종료시간: " + endTime + ")");
```

### 6. 메서드 네이밍 규칙
- `of()`: 정적 팩토리 메서드
- `ofDuration()`: 지속시간 기반 생성
- `overlaps()`, `contains()`: 관계 확인
- `isPast()`, `isFuture()`, `isCurrent()`: 상태 확인
- `withXxx()`: 불변 객체의 값 변경
- `moveBy()`, `extend()`, `shorten()`: 시간대 조작

### 7. 비즈니스 규칙의 코드 반영

#### 같은 날짜 제한
- 자정을 넘나드는 예약 방지
- 일일 단위 예약 관리 단순화

#### 최소 시간 단위
- 시작시간과 종료시간이 같으면 안됨
- 0분 시간대는 비즈니스적으로 의미 없음

이러한 설계로 TimeSlot은 예약 시스템의 핵심 시간 개념을 안전하고 명확하게 표현합니다.