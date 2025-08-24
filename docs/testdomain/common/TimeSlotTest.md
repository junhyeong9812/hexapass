# TimeSlotTest.md

## 클래스 개요
`TimeSlotTest`는 시간대를 다루는 `TimeSlot` 도메인 객체의 기능을 검증하는 테스트 클래스입니다. 이 클래스는 시간대의 생성, 겹침 검사, 인접성, 포함 관계, 시간 정보 처리 등의 핵심 기능들을 테스트합니다.

## 왜 TimeSlot 객체가 필요한가?
- **시간 범위 관리**: 예약 시간, 운영 시간 등의 시간 범위를 명확하게 표현
- **시간 충돌 방지**: 겹치는 예약을 사전에 감지하고 방지
- **비즈니스 규칙 캡슐화**: 같은 날짜 내에서만 시간대 허용, 최소 시간 단위 등
- **시간 연산 추상화**: 복잡한 시간 계산을 도메인 객체 내부로 캡슐화

```java
package com.hexapass.domain.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

// TimeSlot 도메인 객체의 모든 기능을 검증하는 테스트 클래스
@DisplayName("TimeSlot 클래스 테스트")
class TimeSlotTest {

    // 테스트에서 사용할 기준 시간들을 설정
    // LocalDateTime.of(): 특정 연월일시분으로 LocalDateTime 객체 생성
    private final LocalDateTime baseDateTime = LocalDateTime.of(2025, 1, 15, 10, 0); // 2025-01-15 10:00
    private final LocalDateTime oneHourLater = baseDateTime.plusHours(1); // 11:00
    private final LocalDateTime twoHoursLater = baseDateTime.plusHours(2); // 12:00

    @DisplayName("TimeSlot 객체 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 시작시간과 종료시간으로 TimeSlot을 생성할 수 있다")
        void createValidTimeSlot() {
            // When: 정적 팩토리 메서드로 TimeSlot 생성
            // TimeSlot.of(): 시작시간과 종료시간으로 TimeSlot 생성
            TimeSlot timeSlot = TimeSlot.of(baseDateTime, oneHourLater);

            // Then: 생성된 TimeSlot의 속성들 검증
            assertThat(timeSlot).isNotNull();
            // getStartTime(): 시작시간 반환
            assertThat(timeSlot.getStartTime()).isEqualTo(baseDateTime);
            // getEndTime(): 종료시간 반환
            assertThat(timeSlot.getEndTime()).isEqualTo(oneHourLater);
            // getDurationMinutes(): 지속시간을 분 단위로 반환
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(60L);
        }

        @Test
        @DisplayName("시작시간과 지속시간으로 TimeSlot을 생성할 수 있다")
        void createWithDuration() {
            // When: 시작시간과 분 단위 지속시간으로 생성
            // ofDuration(): 시작시간과 분 단위 지속시간으로 TimeSlot 생성
            TimeSlot timeSlot = TimeSlot.ofDuration(baseDateTime, 90); // 1시간 30분

            // Then: 지속시간을 바탕으로 종료시간이 계산되었는지 확인
            assertThat(timeSlot.getStartTime()).isEqualTo(baseDateTime);
            // plusMinutes(): LocalDateTime에 분을 더하는 메서드
            assertThat(timeSlot.getEndTime()).isEqualTo(baseDateTime.plusMinutes(90));
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(90L);
        }

        @Test
        @DisplayName("1시간 단위 TimeSlot을 생성할 수 있다")
        void createOneHourSlot() {
            // When: 1시간 고정 시간대 생성 편의 메서드
            // oneHour(): 1시간짜리 TimeSlot을 생성하는 편의 메서드
            TimeSlot timeSlot = TimeSlot.oneHour(baseDateTime);

            // Then: 1시간 지속시간 확인
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(60L);
            // getDurationHours(): 지속시간을 시간 단위로 반환
            assertThat(timeSlot.getDurationHours()).isEqualTo(1L);
        }

        @Test
        @DisplayName("30분 단위 TimeSlot을 생성할 수 있다")
        void createHalfHourSlot() {
            // When: 30분 고정 시간대 생성 편의 메서드
            // halfHour(): 30분짜리 TimeSlot을 생성하는 편의 메서드
            TimeSlot timeSlot = TimeSlot.halfHour(baseDateTime);

            // Then: 30분 지속시간 확인
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(30L);
        }

        @Test
        @DisplayName("시작시간이 종료시간과 같거나 늦으면 예외가 발생한다")
        void createWithInvalidTimeOrder() {
            // When & Then: 잘못된 시간 순서로 생성 시도
            // 비즈니스 규칙: 시작시간은 반드시 종료시간보다 이전이어야 함
            assertThatThrownBy(() -> TimeSlot.of(oneHourLater, baseDateTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작시간은 종료시간보다 이전이어야 합니다");

            // 같은 시간도 허용하지 않음 (최소 지속시간 필요)
            assertThatThrownBy(() -> TimeSlot.of(baseDateTime, baseDateTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작시간은 종료시간보다 이전이어야 합니다");
        }

        @Test
        @DisplayName("다른 날짜의 시간으로 생성하면 예외가 발생한다")
        void createWithDifferentDates() {
            // Given: 다음날의 시간
            // with(): LocalDateTime의 특정 필드를 변경하여 새 객체 생성
            LocalDateTime nextDayTime = baseDateTime.plusDays(1).withHour(9);

            // When & Then: 다른 날짜 간의 TimeSlot 생성 시도
            // 비즈니스 규칙: TimeSlot은 같은 날짜 내에서만 허용
            assertThatThrownBy(() -> TimeSlot.of(baseDateTime, nextDayTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작시간과 종료시간은 같은 날짜여야 합니다");
        }

        @ParameterizedTest
        @DisplayName("null 시간으로 생성하면 예외가 발생한다")
        @MethodSource("provideNullTimeCombinations")
        void createWithNullTime(LocalDateTime startTime, LocalDateTime endTime) {
            // When & Then: null 시간으로 생성 시도
            // 방어적 프로그래밍: null 입력에 대한 안전성 검증
            assertThatThrownBy(() -> TimeSlot.of(startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null일 수 없습니다");
        }

        // 다양한 null 조합을 제공하는 데이터 소스
        static Stream<Arguments> provideNullTimeCombinations() {
            LocalDateTime validTime = LocalDateTime.now();
            return Stream.of(
                    Arguments.of(null, validTime),
                    Arguments.of(validTime, null),
                    Arguments.of(null, null)
            );
        }

        @ParameterizedTest
        @DisplayName("0 이하의 지속시간으로 생성하면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -30})
        void createWithInvalidDuration(int invalidDuration) {
            // When & Then: 0 이하의 지속시간으로 생성 시도
            // 비즈니스 규칙: 최소 1분 이상의 지속시간 필요
            assertThatThrownBy(() -> TimeSlot.ofDuration(baseDateTime, invalidDuration))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("지속시간은 0보다 커야 합니다");
        }
    }

    @DisplayName("TimeSlot 겹침 검사 테스트")
    @Nested
    class OverlapTest {

        @Test
        @DisplayName("완전히 겹치는 시간대들을 올바르게 판단한다")
        void detectCompleteOverlap() {
            // Given: 포함 관계에 있는 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, twoHoursLater);
            TimeSlot slot2 = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));

            // When & Then: 겹침 검사 (양방향 검증)
            // overlaps(): 두 시간대가 겹치는지 확인하는 비즈니스 메서드
            assertThat(slot1.overlaps(slot2)).isTrue();
            assertThat(slot2.overlaps(slot1)).isTrue(); // 대칭성 확인
        }

        @Test
        @DisplayName("부분적으로 겹치는 시간대들을 올바르게 판단한다")
        void detectPartialOverlap() {
            // Given: 일부가 겹치는 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then: 부분 겹침 검증
            assertThat(slot1.overlaps(slot2)).isTrue();
            assertThat(slot2.overlaps(slot1)).isTrue();
        }

        @Test
        @DisplayName("경계에서 만나는 시간대들은 겹치지 않는다")
        void noBoundaryOverlap() {
            // Given: 끝점에서 만나는 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then: 경계는 겹침으로 처리하지 않음
            // 비즈니스 규칙: DateRange와 달리 TimeSlot은 경계를 겹침으로 보지 않음
            // (예약 시간의 연속성을 허용하기 위함)
            assertThat(slot1.overlaps(slot2)).isFalse();
            assertThat(slot2.overlaps(slot1)).isFalse();
        }

        @Test
        @DisplayName("완전히 분리된 시간대들은 겹치지 않는다")
        void detectNoOverlap() {
            // Given: 완전히 분리된 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, oneHourLater.plusMinutes(30));

            // When & Then: 겹치지 않음 확인
            assertThat(slot1.overlaps(slot2)).isFalse();
            assertThat(slot2.overlaps(slot1)).isFalse();
        }

        @Test
        @DisplayName("null과는 겹치지 않는다")
        void noOverlapWithNull() {
            // Given: 유효한 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then: null 안전성 확인
            assertThat(slot.overlaps(null)).isFalse();
        }
    }

    @DisplayName("TimeSlot 인접성 검사 테스트")
    @Nested
    class AdjacencyTest {

        @Test
        @DisplayName("바로 이어지는 시간대들을 인접하다고 판단한다")
        void detectAdjacency() {
            // Given: 연속된 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then: 인접성 검증
            // isAdjacent(): 두 시간대가 인접한지 확인
            // 끝 시간과 시작 시간이 정확히 만나면 인접
            assertThat(slot1.isAdjacent(slot2)).isTrue();
            assertThat(slot2.isAdjacent(slot1)).isTrue(); // 대칭성 확인
        }

        @Test
        @DisplayName("겹치는 시간대들은 인접하지 않는다")
        void overlappingSlotsNotAdjacent() {
            // Given: 겹치는 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then: 겹치는 시간대는 인접하지 않음
            // 비즈니스 규칙: 겹침과 인접은 상호 배타적
            assertThat(slot1.isAdjacent(slot2)).isFalse();
            assertThat(slot2.isAdjacent(slot1)).isFalse();
        }

        @Test
        @DisplayName("분리된 시간대들은 인접하지 않는다")
        void separatedSlotsNotAdjacent() {
            // Given: 사이에 간격이 있는 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater.plusMinutes(30), twoHoursLater);

            // When & Then: 분리된 시간대는 인접하지 않음
            assertThat(slot1.isAdjacent(slot2)).isFalse();
            assertThat(slot2.isAdjacent(slot1)).isFalse();
        }

        @Test
        @DisplayName("null과는 인접하지 않는다")
        void noAdjacencyWithNull() {
            // Given: 유효한 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then: null 안전성 확인
            assertThat(slot.isAdjacent(null)).isFalse();
        }
    }

    @DisplayName("TimeSlot 포함 검사 테스트")
    @Nested
    class ContainsTest {

        @Test
        @DisplayName("시간대 내의 시간을 포함한다고 판단한다")
        void containsTimeWithinSlot() {
            // Given: 2시간 지속되는 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, twoHoursLater);

            // When & Then: 시간대 내 특정 시점들의 포함 여부 확인
            // contains(): 특정 시점이 시간대에 포함되는지 확인
            assertThat(slot.contains(baseDateTime)).isTrue(); // 시작시간 포함
            assertThat(slot.contains(baseDateTime.plusMinutes(90))).isTrue(); // 중간 시간 포함
            // 종료시간은 포함하지 않음 (반열린구간 [start, end))
            assertThat(slot.contains(twoHoursLater)).isFalse(); 
        }

        @Test
        @DisplayName("시간대 밖의 시간을 포함하지 않는다고 판단한다")
        void doesNotContainTimeOutsideSlot() {
            // Given: 제한된 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));

            // When & Then: 시간대 밖 시점들의 포함 여부 확인
            assertThat(slot.contains(baseDateTime)).isFalse(); // 시작시간 이전
            assertThat(slot.contains(twoHoursLater)).isFalse(); // 종료시간 이후
        }

        @Test
        @DisplayName("다른 시간대가 완전히 포함되는지 확인한다")
        void containsAnotherTimeSlot() {
            // Given: 크고 작은 시간대들
            TimeSlot outerSlot = TimeSlot.of(baseDateTime, twoHoursLater);
            TimeSlot innerSlot = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));
            TimeSlot overlappingSlot = TimeSlot.of(oneHourLater, twoHoursLater.plusMinutes(30));

            // When & Then: 시간대 포함 관계 검증
            // 시간대와 시간대 간의 포함 관계를 확인하는 오버로딩된 contains 메서드
            assertThat(outerSlot.contains(innerSlot)).isTrue();
            assertThat(outerSlot.contains(overlappingSlot)).isFalse();
        }

        @Test
        @DisplayName("null 시간은 포함하지 않는다")
        void doesNotContainNull() {
            // Given: 유효한 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then: null 안전성 확인
            // 메서드 오버로딩으로 LocalDateTime과 TimeSlot 둘 다 처리
            assertThat(slot.contains((LocalDateTime) null)).isFalse();
            assertThat(slot.contains((TimeSlot) null)).isFalse();
        }
    }

    @DisplayName("TimeSlot 순서 비교 테스트")
    @Nested
    class OrderComparisonTest {

        @Test
        @DisplayName("이전 시간대인지 확인한다")
        void checkIfBefore() {
            // Given: 시간적으로 앞선 시간대와 뒤따르는 시간대
            TimeSlot earlierSlot = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot laterSlot = TimeSlot.of(oneHourLater.plusMinutes(30), twoHoursLater);

            // When & Then: 시간적 순서 확인
            // isBefore(): 다른 시간대보다 시간적으로 앞선지 확인
            assertThat(earlierSlot.isBefore(laterSlot)).isTrue();
            assertThat(laterSlot.isBefore(earlierSlot)).isFalse();
        }

        @Test
        @DisplayName("이후 시간대인지 확인한다")
        void checkIfAfter() {
            // Given: 시간적으로 앞선 시간대와 뒤따르는 시간대
            TimeSlot earlierSlot = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot laterSlot = TimeSlot.of(oneHourLater.plusMinutes(30), twoHoursLater);

            // When & Then: 시간적 순서 확인
            // isAfter(): 다른 시간대보다 시간적으로 뒤에 있는지 확인
            assertThat(laterSlot.isAfter(earlierSlot)).isTrue();
            assertThat(earlierSlot.isAfter(laterSlot)).isFalse();
        }

        @Test
        @DisplayName("겹치는 시간대들은 이전/이후가 아니다")
        void overlappingSlotsNotBeforeOrAfter() {
            // Given: 겹치는 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then: 겹치는 시간대는 순서를 정의할 수 없음
            // 비즈니스 규칙: 겹치는 시간대는 순서 비교 불가
            assertThat(slot1.isBefore(slot2)).isFalse();
            assertThat(slot1.isAfter(slot2)).isFalse();
            assertThat(slot2.isBefore(slot1)).isFalse();
            assertThat(slot2.isAfter(slot1)).isFalse();
        }
    }

    @DisplayName("TimeSlot 시간 정보 테스트")
    @Nested
    class TimeInformationTest {

        @Test
        @DisplayName("지속시간을 정확히 계산한다")
        void calculateDurationCorrectly() {
            // Given: 90분 지속되는 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(90));

            // When & Then: 다양한 단위의 지속시간 확인
            // getDuration(): java.time.Duration 객체 반환
            assertThat(slot.getDuration()).isEqualTo(Duration.ofMinutes(90));
            // getDurationMinutes(): 지속시간을 분으로 반환
            assertThat(slot.getDurationMinutes()).isEqualTo(90L);
            // getDurationHours(): 지속시간을 시간으로 반환 (소수점 버림)
            assertThat(slot.getDurationHours()).isEqualTo(1L); // 90분 = 1시간
        }

        @Test
        @DisplayName("현재 시점을 기준으로 과거/현재/미래를 판단한다")
        void determineTimeStatus() {
            // Given: 현재 시간을 기준으로 한 시간대들
            // LocalDateTime.now(): 현재 시간 반환
            LocalDateTime now = LocalDateTime.now();
            TimeSlot pastSlot = TimeSlot.of(now.minusHours(2), now.minusHours(1));
            TimeSlot currentSlot = TimeSlot.of(now.minusMinutes(30), now.plusMinutes(30));
            TimeSlot futureSlot = TimeSlot.of(now.plusHours(1), now.plusHours(2));

            // When & Then: 시간 상태 판단 검증
            // isPast(), isCurrent(), isFuture(): 현재 시간 기준 상태 확인
            assertThat(pastSlot.isPast()).isTrue();
            assertThat(pastSlot.isCurrent()).isFalse();
            assertThat(pastSlot.isFuture()).isFalse();

            assertThat(currentSlot.isPast()).isFalse();
            assertThat(currentSlot.isCurrent()).isTrue(); // 현재 시간 포함
            assertThat(currentSlot.isFuture()).isFalse();

            assertThat(futureSlot.isPast()).isFalse();
            assertThat(futureSlot.isCurrent()).isFalse();
            assertThat(futureSlot.isFuture()).isTrue();
        }

        @Test
        @DisplayName("오늘의 시간대인지 확인한다")
        void checkIfToday() {
            // Given: 오늘과 내일의 시간대
            LocalDateTime today = LocalDateTime.now();
            LocalDateTime tomorrow = today.plusDays(1);

            // withHour(): 시간 부분만 변경하여 새 LocalDateTime 생성
            TimeSlot todaySlot = TimeSlot.of(today.withHour(10), today.withHour(11));
            TimeSlot tomorrowSlot = TimeSlot.of(tomorrow.withHour(10), tomorrow.withHour(11));

            // When & Then: 오늘 날짜 여부 확인
            // isToday(): 시간대가 오늘 날짜에 속하는지 확인
            assertThat(todaySlot.isToday()).isTrue();
            assertThat(tomorrowSlot.isToday()).isFalse();
        }
    }

    @DisplayName("TimeSlot 변환 테스트")
    @Nested
    class TransformationTest {

        @Test
        @DisplayName("시작시간을 변경한 새로운 시간대를 만들 수 있다")
        void changeStartTime() {
            // Given: 원본 시간대
            TimeSlot original = TimeSlot.of(baseDateTime, twoHoursLater);
            LocalDateTime newStartTime = baseDateTime.plusMinutes(30);

            // When: 시작시간 변경 (불변 객체이므로 새 객체 반환)
            // withStartTime(): 시작시간을 변경한 새로운 TimeSlot 반환
            TimeSlot modified = original.withStartTime(newStartTime);

            // Then: 변경된 시간대와 원본 보존 확인
            assertThat(modified.getStartTime()).isEqualTo(newStartTime);
            assertThat(modified.getEndTime()).isEqualTo(twoHoursLater);
            // 불변성 확인: 원본 객체는 변경되지 않음
            assertThat(original.getStartTime()).isEqualTo(baseDateTime);
        }

        @Test
        @DisplayName("종료시간을 변경한 새로운 시간대를 만들 수 있다")
        void changeEndTime() {
            // Given: 원본 시간대
            TimeSlot original = TimeSlot.of(baseDateTime, twoHoursLater);
            LocalDateTime newEndTime = twoHoursLater.plusMinutes(30);

            // When: 종료시간 변경
            // withEndTime(): 종료시간을 변경한 새로운 TimeSlot 반환
            TimeSlot modified = original.withEndTime(newEndTime);

            // Then: 변경된 시간대와 원본 보존 확인
            assertThat(modified.getStartTime()).isEqualTo(baseDateTime);
            assertThat(modified.getEndTime()).isEqualTo(newEndTime);
            // 불변성 확인
            assertThat(original.getEndTime()).isEqualTo(twoHoursLater);
        }

        @Test
        @DisplayName("시간대를 이동할 수 있다")
        void moveTimeSlot() {
            // Given: 1시간 지속 시간대
            TimeSlot original = TimeSlot.of(baseDateTime, oneHourLater);

            // When: 30분 뒤로 이동
            // moveBy(): 시작시간과 종료시간을 동일하게 이동
            TimeSlot moved = original.moveBy(30); // 30분 뒤로 이동

            // Then: 이동된 시간대 확인 (지속시간은 동일)
            assertThat(moved.getStartTime()).isEqualTo(baseDateTime.plusMinutes(30));
            assertThat(moved.getEndTime()).isEqualTo(oneHourLater.plusMinutes(30));
            assertThat(moved.getDurationMinutes()).isEqualTo(original.getDurationMinutes());
        }

        @Test
        @DisplayName("시간대를 연장할 수 있다")
        void extendTimeSlot() {
            // Given: 1시간 지속 시간대
            TimeSlot original = TimeSlot.of(baseDateTime, oneHourLater);

            // When: 30분 연장
            // extend(): 종료시간을 지정된 분만큼 연장
            TimeSlot extended = original.extend(30); // 30분 연장

            // Then: 연장된 시간대 확인
            assertThat(extended.getStartTime()).isEqualTo(baseDateTime);
            assertThat(extended.getEndTime()).isEqualTo(oneHourLater.plusMinutes(30));
            assertThat(extended.getDurationMinutes()).isEqualTo(90L);
        }

        @Test
        @DisplayName("시간대를 단축할 수 있다")
        void shortenTimeSlot() {
            // Given: 2시간 지속 시간대
            TimeSlot original = TimeSlot.of(baseDateTime, twoHoursLater); // 120분

            // When: 30분 단축
            // shorten(): 종료시간을 지정된 분만큼 앞당김
            TimeSlot shortened = original.shorten(30); // 30분 단축

            // Then: 단축된 시간대 확인
            assertThat(shortened.getStartTime()).isEqualTo(baseDateTime);
            assertThat(shortened.getEndTime()).isEqualTo(twoHoursLater.minusMinutes(30));
            assertThat(shortened.getDurationMinutes()).isEqualTo(90L);
        }

        @Test
        @DisplayName("과도한 단축은 예외가 발생한다")
        void excessiveShorteningThrowsException() {
            // Given: 30분 지속 시간대
            TimeSlot shortSlot = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(30));

            // When & Then: 지속시간보다 많이 단축하려 할 때 예외 발생
            // 비즈니스 규칙: 단축 후에도 최소 지속시간이 유지되어야 함
            assertThatThrownBy(() -> shortSlot.shorten(60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("단축 후 종료시간이 시작시간보다 이전이거나 같을 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("음수로 연장/단축하면 예외가 발생한다")
        @ValueSource(ints = {-1, -30, -60})
        void negativeExtensionOrShorteningThrowsException(int negativeMinutes) {
            // Given: 기본 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then: 음수 값으로 연장/단축 시 예외 발생
            // 비즈니스 규칙: 연장/단축은 양수 값만 허용
            assertThatThrownBy(() -> slot.extend(negativeMinutes))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> slot.shorten(negativeMinutes))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @DisplayName("TimeSlot 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 시작시간과 종료시간을 가진 시간대들은 동등하다")
        void equalityWithSameTimes() {
            // Given: 같은 시간으로 두 시간대 생성
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then: 동등성과 해시코드 일치 확인
            // equals(): 시작시간과 종료시간이 모두 같으면 동등
            assertThat(slot1).isEqualTo(slot2);
            // hashCode(): equals가 true이면 hashCode도 같아야 함
            assertThat(slot1.hashCode()).isEqualTo(slot2.hashCode());
        }

        @Test
        @DisplayName("다른 시간을 가진 시간대들은 동등하지 않다")
        void inequalityWithDifferentTimes() {
            // Given: 다른 시간의 두 시간대
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));

            // When & Then: 비동등성 확인
            assertThat(slot1).isNotEqualTo(slot2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given: 유효한 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then: null과의 비교 (null 안전성)
            assertThat(slot).isNotEqualTo(null);
        }
    }

    @DisplayName("TimeSlot toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 형식으로 출력된다")
        void toStringFormat() {
            // Given: 1시간 지속 시간대
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When: 문자열 변환
            String result = slot.toString();

            // Then: 형식 확인 (시작시간, 종료시간, 지속시간 포함)
            // toString(): 개발자 친화적인 문자열 표현 제공
            assertThat(result).contains("2025-01-15 10:00");
            assertThat(result).contains("2025-01-15 11:00");
            assertThat(result).contains("60분");
        }
    }
}
```

## 주요 설계 원칙 및 특징

### 1. **불변 객체 (Immutable Object)**
- `TimeSlot`은 생성 후 변경되지 않는 불변 객체
- 모든 변환 메서드는 새로운 객체를 반환

### 2. **비즈니스 규칙 캡슐화**
- 같은 날짜 내에서만 시간대 허용
- 최소 지속시간 보장 (0분 초과)
- 시간 겹침/인접성 로직 캡슐화

### 3. **정적 팩토리 메서드**
- `of()`, `oneHour()`, `halfHour()` 등의 명확한 생성 메서드
- 다양한 생성 방식 지원 (시간 범위, 지속시간 등)

### 4. **시간 연산 추상화**
- 복잡한 시간 계산을 도메인 객체 내부로 캡슐화
- `Duration` 클래스와의 통합으로 정확한 시간 계산

### 5. **방어적 프로그래밍**
- null 입력에 대한 안전한 처리
- 잘못된 시간 순서나 음수 지속시간에 대한 예외 처리

### 사용되는 주요 Java 클래스와 기능

#### **LocalDateTime**
```java
// 날짜와 시간을 함께 표현하는 불변 클래스
LocalDateTime start = LocalDateTime.of(2025, 1, 15, 10, 0);
// 타임존 정보 없는 로컬 날짜/시간 표현
```

#### **Duration**
```java
// 두 시점 간의 시간 간격을 나타내는 클래스
Duration duration = Duration.between(start, end);
// 나노초 단위까지 정확한 시간 간격 계산
```

#### **메서드 오버로딩**
```java
// 같은 메서드 이름으로 다른 매개변수 타입 지원
public boolean contains(LocalDateTime time) { ... }
public boolean contains(TimeSlot other) { ... }
```

### 왜 이런 구조로 설계했는가?

1. **예약 시스템의 핵심**: 시간대 관리는 예약 시스템의 가장 중요한 기능
2. **시간 충돌 방지**: 겹치는 예약을 사전에 감지하여 비즈니스 오류 방지
3. **정확성 보장**: `Duration` 클래스 사용으로 정밀한 시간 계산
4. **사용자 친화적**: 1시간, 30분 같은 일반적인 시간대 생성 편의 메서드 제공
5. **확장성**: 새로운 시간 관련 기능을 쉽게 추가할 수 있는 구조