# DateRange.java - 상세 주석 및 설명

## 클래스 개요
`DateRange`는 시작일과 종료일을 포함하는 날짜 범위를 나타내는 **값 객체(Value Object)**입니다.
불변 객체로 설계되어 한 번 생성되면 상태를 변경할 수 없으며, 양 끝 날짜를 모두 포함하는 구간([startDate, endDate])으로 정의됩니다.

## 왜 이런 클래스가 필요한가?
1. **도메인 개념의 명확한 표현**: 예약 시스템에서 "기간"이라는 개념을 명확히 표현
2. **타입 안전성**: `LocalDate start, LocalDate end` 대신 `DateRange period` 사용으로 매개변수 순서 실수 방지
3. **응집도**: 날짜 범위 관련 로직을 한 곳에 모음
4. **재사용성**: 다양한 도메인에서 날짜 범위 개념 재사용

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.common;

import java.time.LocalDate; // 날짜를 나타내는 Java 8+ 불변 클래스
import java.time.temporal.ChronoUnit; // 시간 단위 계산을 위한 열거형
import java.util.Objects; // equals, hashCode 계산을 위한 유틸리티

/**
 * 시작일과 종료일을 포함하는 날짜 범위를 나타내는 값 객체
 * 불변 객체로 설계되어 생성 후 상태 변경 불가
 * 양 끝 날짜를 모두 포함하는 구간으로 정의
 * 
 * 값 객체(Value Object)의 특징:
 * 1. 불변성: 생성 후 상태 변경 불가
 * 2. 동등성: 값이 같으면 같은 객체로 취급
 * 3. 자가 검증: 생성 시 유효성 검사
 * 4. 부수 효과 없음: 메서드 호출이 객체 상태를 변경하지 않음
 */
public final class DateRange { // final: 상속 불가, 불변성 보장

    // private final: 외부에서 직접 접근 불가, 생성 후 변경 불가
    private final LocalDate startDate; // 시작일 (포함)
    private final LocalDate endDate;   // 종료일 (포함)

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * 왜 private 생성자인가?
     * 1. 객체 생성 방법 제어: 팩토리 메서드를 통해서만 생성
     * 2. 유효성 검사 강제: 모든 생성 경로에서 검증 보장
     * 3. 명확한 의도 표현: of(), singleDay() 등으로 생성 의도 명확화
     */
    private DateRange(LocalDate startDate, LocalDate endDate) {
        // validateNotNull: null 체크 후 원본 값 반환, 실패 시 예외
        this.startDate = validateNotNull(startDate, "시작일");
        this.endDate = validateNotNull(endDate, "종료일");
        // 시작일이 종료일보다 늦으면 예외 발생
        validateDateOrder(startDate, endDate);
    }

    /**
     * 시작일과 종료일을 지정하여 DateRange 생성
     * 
     * 정적 팩토리 메서드(Static Factory Method) 패턴:
     * 1. 생성자보다 명확한 의미 전달
     * 2. 매개변수에 따른 다양한 생성 방법 제공
     * 3. 캐싱, 싱글톤 등 인스턴스 제어 가능 (현재는 미사용)
     */
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate); // private 생성자 호출
    }

    /**
     * 단일 날짜로 DateRange 생성 (시작일 = 종료일)
     * 
     * 편의 메서드: 하루짜리 기간을 쉽게 생성
     * 예: 하루 이벤트, 당일 예약 등
     */
    public static DateRange singleDay(LocalDate date) {
        return new DateRange(date, date); // 시작일 = 종료일
    }

    /**
     * 오늘부터 지정된 일수만큼의 DateRange 생성
     * 
     * 현재 시점 기준 기간 생성의 편의 메서드
     */
    public static DateRange fromTodayFor(int days) {
        // 가드 클로즈: 잘못된 입력에 대해 빠른 실패
        if (days < 1) {
            // IllegalArgumentException: 잘못된 인수에 대한 표준 예외
            throw new IllegalArgumentException("일수는 1 이상이어야 합니다. 입력값: " + days);
        }

        LocalDate today = LocalDate.now(); // 현재 날짜 (시스템 기본 시간대)
        // days일 포함하려면 -1: 
        // 예를 들어, 3일이면 오늘 + 2일 더 = 총 3일
        LocalDate endDate = today.plusDays(days - 1);
        return new DateRange(today, endDate);
    }

    /**
     * 지정된 날짜부터 일정 기간의 DateRange 생성
     * 
     * 특정 시작일 기준 기간 생성
     */
    public static DateRange fromDateFor(LocalDate startDate, int days) {
        // null 체크: 가장 기본적인 방어적 프로그래밍
        if (startDate == null) {
            throw new IllegalArgumentException("시작일은 null일 수 없습니다");
        }
        if (days < 1) {
            throw new IllegalArgumentException("일수는 1 이상이어야 합니다. 입력값: " + days);
        }

        LocalDate endDate = startDate.plusDays(days - 1);
        return new DateRange(startDate, endDate);
    }

    // =========================
    // 날짜 범위 연산 메서드들
    // =========================

    /**
     * 다른 날짜 범위와 겹치는지 확인
     * 
     * 겹침 판단 로직: A.end >= B.start && B.end >= A.start
     * 이는 두 구간이 겹치는 표준적인 수학적 공식
     */
    public boolean overlaps(DateRange other) {
        if (other == null) {
            return false; // null과는 겹칠 수 없음
        }

        // !isBefore(): >= 의 의미 (LocalDate에는 >= 메서드가 없음)
        // 논리: 이 범위의 끝이 다른 범위의 시작보다 늦거나 같고,
        //      다른 범위의 끝이 이 범위의 시작보다 늦거나 같으면 겹침
        return !this.endDate.isBefore(other.startDate) &&
                !other.endDate.isBefore(this.startDate);
    }

    /**
     * 지정된 날짜가 이 범위에 포함되는지 확인 (양 끝 포함)
     * 
     * 폐구간 [startDate, endDate] 개념
     */
    public boolean contains(LocalDate date) {
        if (date == null) {
            return false; // null 날짜는 포함하지 않음
        }

        // !isBefore(): >= 의 의미, !isAfter(): <= 의 의미
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 다른 날짜 범위가 이 범위에 완전히 포함되는지 확인
     * 
     * 부분 집합 관계 확인
     */
    public boolean contains(DateRange other) {
        if (other == null) {
            return false; // null 범위는 포함할 수 없음
        }

        // 다른 범위의 시작이 이 범위의 시작보다 늦거나 같고,
        // 다른 범위의 끝이 이 범위의 끝보다 이르거나 같으면 포함
        return !other.startDate.isBefore(this.startDate) &&
                !other.endDate.isAfter(this.endDate);
    }

    /**
     * 이 날짜 범위가 다른 범위에 완전히 포함되는지 확인
     * 
     * 메서드 위임(Method Delegation): 중복 코드 제거
     */
    public boolean isContainedBy(DateRange other) {
        return other != null && other.contains(this); // null 체크 후 위임
    }

    /**
     * 다른 날짜 범위와 인접한지 확인 (바로 이어지는지)
     * 
     * 인접성: 한 범위의 다음날이 다른 범위의 시작일
     * 예: [2024-01-01, 2024-01-03]과 [2024-01-04, 2024-01-06]은 인접
     */
    public boolean isAdjacentTo(DateRange other) {
        if (other == null) {
            return false;
        }

        // plusDays(1): 다음날 계산
        // equals(): LocalDate는 값 객체이므로 값 비교
        return this.endDate.plusDays(1).equals(other.startDate) ||
                other.endDate.plusDays(1).equals(this.startDate);
    }

    // =========================
    // 날짜 범위 정보 메서드들
    // =========================

    /**
     * 날짜 범위의 일수 계산 (양 끝 포함)
     * 
     * ChronoUnit.DAYS.between(): 두 날짜 사이의 일수 계산
     * +1: 양끝 포함 구간이므로 (예: 1일~3일 = 3일)
     */
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 현재 날짜 기준으로 이 범위가 과거인지 확인
     */
    public boolean isPast() {
        return endDate.isBefore(LocalDate.now()); // 종료일이 오늘보다 이전
    }

    /**
     * 현재 날짜 기준으로 이 범위가 미래인지 확인
     */
    public boolean isFuture() {
        return startDate.isAfter(LocalDate.now()); // 시작일이 오늘보다 이후
    }

    /**
     * 현재 날짜가 이 범위에 포함되는지 확인 (현재 진행중)
     * 
     * 메서드 재사용: contains 메서드 활용
     */
    public boolean isCurrent() {
        return contains(LocalDate.now());
    }

    /**
     * 단일 날짜 범위인지 확인 (시작일 = 종료일)
     * 
     * LocalDate.equals(): 값 객체의 동등성 비교
     */
    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }

    // =========================
    // 날짜 범위 변환 메서드들
    // =========================

    /**
     * 시작일을 변경한 새로운 DateRange 반환
     * 
     * 불변 객체의 특징: 기존 객체는 변경하지 않고 새 객체 반환
     * "withXxx" 네이밍: 불변 객체에서 값 변경 시 사용하는 관례
     */
    public DateRange withStartDate(LocalDate newStartDate) {
        return new DateRange(newStartDate, this.endDate);
    }

    /**
     * 종료일을 변경한 새로운 DateRange 반환
     */
    public DateRange withEndDate(LocalDate newEndDate) {
        return new DateRange(this.startDate, newEndDate);
    }

    /**
     * 지정된 일수만큼 연장한 새로운 DateRange 반환
     * 
     * 연장: 종료일을 늘려서 기간을 길게 만듦
     */
    public DateRange extend(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("연장 일수는 0 이상이어야 합니다. 입력값: " + days);
        }

        return new DateRange(this.startDate, this.endDate.plusDays(days));
    }

    /**
     * 지정된 일수만큼 단축한 새로운 DateRange 반환
     * 
     * 단축: 종료일을 당겨서 기간을 짧게 만듦
     */
    public DateRange shorten(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("단축 일수는 0 이상이어야 합니다. 입력값: " + days);
        }

        LocalDate newEndDate = this.endDate.minusDays(days);
        // 단축 후에도 시작일 <= 종료일 조건 유지
        if (newEndDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("단축 후 종료일이 시작일보다 이전이 될 수 없습니다");
        }

        return new DateRange(this.startDate, newEndDate);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    /**
     * equals 메서드 오버라이드
     * 
     * 값 객체의 핵심: 값이 같으면 같은 객체로 취급
     * Object.equals() 계약을 준수해야 함:
     * 1. 반사성(reflexive): x.equals(x) == true
     * 2. 대칭성(symmetric): x.equals(y) == y.equals(x)
     * 3. 이행성(transitive): x.equals(y) && y.equals(z) => x.equals(z)
     * 4. 일관성(consistent): 여러 번 호출해도 같은 결과
     * 5. null 처리: x.equals(null) == false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // 같은 참조 -> true (성능 최적화)
        if (obj == null || getClass() != obj.getClass()) return false; // null 또는 다른 클래스 -> false

        DateRange dateRange = (DateRange) obj; // 안전한 캐스팅 (위에서 타입 확인)
        // Objects.equals(): null-safe 비교 유틸리티
        return Objects.equals(startDate, dateRange.startDate) &&
                Objects.equals(endDate, dateRange.endDate);
    }

    /**
     * hashCode 메서드 오버라이드
     * 
     * equals/hashCode 계약: equals가 true면 hashCode도 같아야 함
     * HashMap, HashSet 등에서 올바른 동작을 위해 필요
     */
    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate); // Objects.hash(): 다중 값의 해시코드 계산
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 디버깅과 로깅을 위한 문자열 표현
     * 단일일 경우와 기간 경우를 구분하여 가독성 향상
     */
    @Override
    public String toString() {
        if (isSingleDay()) {
            return startDate.toString(); // "2024-01-01" 형태
        }
        // String.format(): C 스타일 문자열 포매팅
        return String.format("%s ~ %s (%d일)", startDate, endDate, getDays());
    }

    // =========================
    // Getter 메서드들
    // =========================

    /**
     * 시작일 반환
     * 
     * LocalDate는 불변 객체이므로 방어적 복사 불필요
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * 종료일 반환
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    /**
     * null 체크 및 유효성 검사
     * 
     * 방어적 프로그래밍: 잘못된 입력에 대한 빠른 실패
     * 의미있는 에러 메시지로 디버깅 지원
     */
    private LocalDate validateNotNull(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return date; // 메서드 체이닝을 위한 원본 반환
    }

    /**
     * 날짜 순서 검증
     * 
     * 불변식(Invariant) 보장: 시작일 <= 종료일
     */
    private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            // String.format(): 구체적인 값이 포함된 오류 메시지
            throw new IllegalArgumentException(
                    String.format("시작일은 종료일보다 이전이거나 같아야 합니다. (시작일: %s, 종료일: %s)",
                            startDate, endDate));
        }
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. 값 객체 (Value Object) 특징
- **불변성**: 한 번 생성되면 변경할 수 없음
- **동등성**: 값이 같으면 같은 객체로 취급
- **자가 검증**: 생성 시 유효한 상태만 허용

### 2. 사용된 Java API 및 개념

#### LocalDate (Java 8+)
- `isBefore()`, `isAfter()`: 날짜 비교
- `plusDays()`, `minusDays()`: 날짜 계산 (불변 객체이므로 새 인스턴스 반환)
- `equals()`: 값 기반 동등성 비교

#### ChronoUnit
- `between()`: 두 시간 단위 사이의 차이 계산

#### Objects 유틸리티
- `equals()`: null-safe 동등성 비교
- `hash()`: 여러 값의 해시코드 계산

### 3. 예외 처리 전략
- **IllegalArgumentException**: 잘못된 매개변수에 대한 표준 예외
- **빠른 실패 (Fail-Fast)**: 문제 발생 시 즉시 예외 발생
- **의미있는 메시지**: 디버깅을 위한 구체적인 오류 정보

### 4. 메서드 네이밍 규칙
- `of()`: 정적 팩토리 메서드
- `withXxx()`: 불변 객체의 값 변경
- `isXxx()`: boolean 반환 메서드
- `contains()`: 포함 관계 확인

이러한 설계를 통해 DateRange는 안전하고 표현력 있는 도메인 객체가 되었습니다.