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

@DisplayName("TimeSlot 클래스 테스트")
class TimeSlotTest {

    private final LocalDateTime baseDateTime = LocalDateTime.of(2025, 1, 15, 10, 0); // 2025-01-15 10:00
    private final LocalDateTime oneHourLater = baseDateTime.plusHours(1); // 11:00
    private final LocalDateTime twoHoursLater = baseDateTime.plusHours(2); // 12:00

    @DisplayName("TimeSlot 객체 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 시작시간과 종료시간으로 TimeSlot을 생성할 수 있다")
        void createValidTimeSlot() {
            // When
            TimeSlot timeSlot = TimeSlot.of(baseDateTime, oneHourLater);

            // Then
            assertThat(timeSlot).isNotNull();
            assertThat(timeSlot.getStartTime()).isEqualTo(baseDateTime);
            assertThat(timeSlot.getEndTime()).isEqualTo(oneHourLater);
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(60L);
        }

        @Test
        @DisplayName("시작시간과 지속시간으로 TimeSlot을 생성할 수 있다")
        void createWithDuration() {
            // When
            TimeSlot timeSlot = TimeSlot.ofDuration(baseDateTime, 90); // 1시간 30분

            // Then
            assertThat(timeSlot.getStartTime()).isEqualTo(baseDateTime);
            assertThat(timeSlot.getEndTime()).isEqualTo(baseDateTime.plusMinutes(90));
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(90L);
        }

        @Test
        @DisplayName("1시간 단위 TimeSlot을 생성할 수 있다")
        void createOneHourSlot() {
            // When
            TimeSlot timeSlot = TimeSlot.oneHour(baseDateTime);

            // Then
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(60L);
            assertThat(timeSlot.getDurationHours()).isEqualTo(1L);
        }

        @Test
        @DisplayName("30분 단위 TimeSlot을 생성할 수 있다")
        void createHalfHourSlot() {
            // When
            TimeSlot timeSlot = TimeSlot.halfHour(baseDateTime);

            // Then
            assertThat(timeSlot.getDurationMinutes()).isEqualTo(30L);
        }

        @Test
        @DisplayName("시작시간이 종료시간과 같거나 늦으면 예외가 발생한다")
        void createWithInvalidTimeOrder() {
            // When & Then
            assertThatThrownBy(() -> TimeSlot.of(oneHourLater, baseDateTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작시간은 종료시간보다 이전이어야 합니다");

            assertThatThrownBy(() -> TimeSlot.of(baseDateTime, baseDateTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작시간은 종료시간보다 이전이어야 합니다");
        }

        @Test
        @DisplayName("다른 날짜의 시간으로 생성하면 예외가 발생한다")
        void createWithDifferentDates() {
            // Given
            LocalDateTime nextDayTime = baseDateTime.plusDays(1).withHour(9);

            // When & Then
            assertThatThrownBy(() -> TimeSlot.of(baseDateTime, nextDayTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작시간과 종료시간은 같은 날짜여야 합니다");
        }

        @ParameterizedTest
        @DisplayName("null 시간으로 생성하면 예외가 발생한다")
        @MethodSource("provideNullTimeCombinations")
        void createWithNullTime(LocalDateTime startTime, LocalDateTime endTime) {
            // When & Then
            assertThatThrownBy(() -> TimeSlot.of(startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null일 수 없습니다");
        }

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
            // When & Then
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
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, twoHoursLater);
            TimeSlot slot2 = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));

            // When & Then
            assertThat(slot1.overlaps(slot2)).isTrue();
            assertThat(slot2.overlaps(slot1)).isTrue();
        }

        @Test
        @DisplayName("부분적으로 겹치는 시간대들을 올바르게 판단한다")
        void detectPartialOverlap() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then
            assertThat(slot1.overlaps(slot2)).isTrue();
            assertThat(slot2.overlaps(slot1)).isTrue();
        }

        @Test
        @DisplayName("경계에서 만나는 시간대들은 겹치지 않는다")
        void noBoundaryOverlap() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then
            assertThat(slot1.overlaps(slot2)).isFalse();
            assertThat(slot2.overlaps(slot1)).isFalse();
        }

        @Test
        @DisplayName("완전히 분리된 시간대들은 겹치지 않는다")
        void detectNoOverlap() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, oneHourLater.plusMinutes(30));

            // When & Then
            assertThat(slot1.overlaps(slot2)).isFalse();
            assertThat(slot2.overlaps(slot1)).isFalse();
        }

        @Test
        @DisplayName("null과는 겹치지 않는다")
        void noOverlapWithNull() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then
            assertThat(slot.overlaps(null)).isFalse();
        }
    }

    @DisplayName("TimeSlot 인접성 검사 테스트")
    @Nested
    class AdjacencyTest {

        @Test
        @DisplayName("바로 이어지는 시간대들을 인접하다고 판단한다")
        void detectAdjacency() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then
            assertThat(slot1.isAdjacent(slot2)).isTrue();
            assertThat(slot2.isAdjacent(slot1)).isTrue();
        }

        @Test
        @DisplayName("겹치는 시간대들은 인접하지 않는다")
        void overlappingSlotsNotAdjacent() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then
            assertThat(slot1.isAdjacent(slot2)).isFalse();
            assertThat(slot2.isAdjacent(slot1)).isFalse();
        }

        @Test
        @DisplayName("분리된 시간대들은 인접하지 않는다")
        void separatedSlotsNotAdjacent() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater.plusMinutes(30), twoHoursLater);

            // When & Then
            assertThat(slot1.isAdjacent(slot2)).isFalse();
            assertThat(slot2.isAdjacent(slot1)).isFalse();
        }

        @Test
        @DisplayName("null과는 인접하지 않는다")
        void noAdjacencyWithNull() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then
            assertThat(slot.isAdjacent(null)).isFalse();
        }
    }

    @DisplayName("TimeSlot 포함 검사 테스트")
    @Nested
    class ContainsTest {

        @Test
        @DisplayName("시간대 내의 시간을 포함한다고 판단한다")
        void containsTimeWithinSlot() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, twoHoursLater);

            // When & Then
            assertThat(slot.contains(baseDateTime)).isTrue(); // 시작시간 포함
            assertThat(slot.contains(baseDateTime.plusMinutes(90))).isTrue(); // 중간 시간 포함
            assertThat(slot.contains(twoHoursLater)).isFalse(); // 종료시간 미포함
        }

        @Test
        @DisplayName("시간대 밖의 시간을 포함하지 않는다고 판단한다")
        void doesNotContainTimeOutsideSlot() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));

            // When & Then
            assertThat(slot.contains(baseDateTime)).isFalse(); // 시작시간 이전
            assertThat(slot.contains(twoHoursLater)).isFalse(); // 종료시간 이후
        }

        @Test
        @DisplayName("다른 시간대가 완전히 포함되는지 확인한다")
        void containsAnotherTimeSlot() {
            // Given
            TimeSlot outerSlot = TimeSlot.of(baseDateTime, twoHoursLater);
            TimeSlot innerSlot = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));
            TimeSlot overlappingSlot = TimeSlot.of(oneHourLater, twoHoursLater.plusMinutes(30));

            // When & Then
            assertThat(outerSlot.contains(innerSlot)).isTrue();
            assertThat(outerSlot.contains(overlappingSlot)).isFalse();
        }

        @Test
        @DisplayName("null 시간은 포함하지 않는다")
        void doesNotContainNull() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then
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
            // Given
            TimeSlot earlierSlot = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot laterSlot = TimeSlot.of(oneHourLater.plusMinutes(30), twoHoursLater);

            // When & Then
            assertThat(earlierSlot.isBefore(laterSlot)).isTrue();
            assertThat(laterSlot.isBefore(earlierSlot)).isFalse();
        }

        @Test
        @DisplayName("이후 시간대인지 확인한다")
        void checkIfAfter() {
            // Given
            TimeSlot earlierSlot = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot laterSlot = TimeSlot.of(oneHourLater.plusMinutes(30), twoHoursLater);

            // When & Then
            assertThat(laterSlot.isAfter(earlierSlot)).isTrue();
            assertThat(earlierSlot.isAfter(laterSlot)).isFalse();
        }

        @Test
        @DisplayName("겹치는 시간대들은 이전/이후가 아니다")
        void overlappingSlotsNotBeforeOrAfter() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater.plusMinutes(30));
            TimeSlot slot2 = TimeSlot.of(oneHourLater, twoHoursLater);

            // When & Then
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
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(90));

            // When & Then
            assertThat(slot.getDuration()).isEqualTo(Duration.ofMinutes(90));
            assertThat(slot.getDurationMinutes()).isEqualTo(90L);
            assertThat(slot.getDurationHours()).isEqualTo(1L); // 90분 = 1시간
        }

        @Test
        @DisplayName("현재 시점을 기준으로 과거/현재/미래를 판단한다")
        void determineTimeStatus() {
            // Given - 현재 시간을 기준으로 한 시간대들
            LocalDateTime now = LocalDateTime.now();
            TimeSlot pastSlot = TimeSlot.of(now.minusHours(2), now.minusHours(1));
            TimeSlot currentSlot = TimeSlot.of(now.minusMinutes(30), now.plusMinutes(30));
            TimeSlot futureSlot = TimeSlot.of(now.plusHours(1), now.plusHours(2));

            // When & Then
            assertThat(pastSlot.isPast()).isTrue();
            assertThat(pastSlot.isCurrent()).isFalse();
            assertThat(pastSlot.isFuture()).isFalse();

            assertThat(currentSlot.isPast()).isFalse();
            assertThat(currentSlot.isCurrent()).isTrue();
            assertThat(currentSlot.isFuture()).isFalse();

            assertThat(futureSlot.isPast()).isFalse();
            assertThat(futureSlot.isCurrent()).isFalse();
            assertThat(futureSlot.isFuture()).isTrue();
        }

        @Test
        @DisplayName("오늘의 시간대인지 확인한다")
        void checkIfToday() {
            // Given
            LocalDateTime today = LocalDateTime.now();
            LocalDateTime tomorrow = today.plusDays(1);

            TimeSlot todaySlot = TimeSlot.of(today.withHour(10), today.withHour(11));
            TimeSlot tomorrowSlot = TimeSlot.of(tomorrow.withHour(10), tomorrow.withHour(11));

            // When & Then
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
            // Given
            TimeSlot original = TimeSlot.of(baseDateTime, twoHoursLater);
            LocalDateTime newStartTime = baseDateTime.plusMinutes(30);

            // When
            TimeSlot modified = original.withStartTime(newStartTime);

            // Then
            assertThat(modified.getStartTime()).isEqualTo(newStartTime);
            assertThat(modified.getEndTime()).isEqualTo(twoHoursLater);
            // 원본은 변경되지 않음
            assertThat(original.getStartTime()).isEqualTo(baseDateTime);
        }

        @Test
        @DisplayName("종료시간을 변경한 새로운 시간대를 만들 수 있다")
        void changeEndTime() {
            // Given
            TimeSlot original = TimeSlot.of(baseDateTime, twoHoursLater);
            LocalDateTime newEndTime = twoHoursLater.plusMinutes(30);

            // When
            TimeSlot modified = original.withEndTime(newEndTime);

            // Then
            assertThat(modified.getStartTime()).isEqualTo(baseDateTime);
            assertThat(modified.getEndTime()).isEqualTo(newEndTime);
            // 원본은 변경되지 않음
            assertThat(original.getEndTime()).isEqualTo(twoHoursLater);
        }

        @Test
        @DisplayName("시간대를 이동할 수 있다")
        void moveTimeSlot() {
            // Given
            TimeSlot original = TimeSlot.of(baseDateTime, oneHourLater);

            // When
            TimeSlot moved = original.moveBy(30); // 30분 뒤로 이동

            // Then
            assertThat(moved.getStartTime()).isEqualTo(baseDateTime.plusMinutes(30));
            assertThat(moved.getEndTime()).isEqualTo(oneHourLater.plusMinutes(30));
            assertThat(moved.getDurationMinutes()).isEqualTo(original.getDurationMinutes());
        }

        @Test
        @DisplayName("시간대를 연장할 수 있다")
        void extendTimeSlot() {
            // Given
            TimeSlot original = TimeSlot.of(baseDateTime, oneHourLater);

            // When
            TimeSlot extended = original.extend(30); // 30분 연장

            // Then
            assertThat(extended.getStartTime()).isEqualTo(baseDateTime);
            assertThat(extended.getEndTime()).isEqualTo(oneHourLater.plusMinutes(30));
            assertThat(extended.getDurationMinutes()).isEqualTo(90L);
        }

        @Test
        @DisplayName("시간대를 단축할 수 있다")
        void shortenTimeSlot() {
            // Given
            TimeSlot original = TimeSlot.of(baseDateTime, twoHoursLater); // 120분

            // When
            TimeSlot shortened = original.shorten(30); // 30분 단축

            // Then
            assertThat(shortened.getStartTime()).isEqualTo(baseDateTime);
            assertThat(shortened.getEndTime()).isEqualTo(twoHoursLater.minusMinutes(30));
            assertThat(shortened.getDurationMinutes()).isEqualTo(90L);
        }

        @Test
        @DisplayName("과도한 단축은 예외가 발생한다")
        void excessiveShorteningThrowsException() {
            // Given
            TimeSlot shortSlot = TimeSlot.of(baseDateTime, baseDateTime.plusMinutes(30));

            // When & Then
            assertThatThrownBy(() -> shortSlot.shorten(60))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("단축 후 종료시간이 시작시간보다 이전이거나 같을 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("음수로 연장/단축하면 예외가 발생한다")
        @ValueSource(ints = {-1, -30, -60})
        void negativeExtensionOrShorteningThrowsException(int negativeMinutes) {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then
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
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then
            assertThat(slot1).isEqualTo(slot2);
            assertThat(slot1.hashCode()).isEqualTo(slot2.hashCode());
        }

        @Test
        @DisplayName("다른 시간을 가진 시간대들은 동등하지 않다")
        void inequalityWithDifferentTimes() {
            // Given
            TimeSlot slot1 = TimeSlot.of(baseDateTime, oneHourLater);
            TimeSlot slot2 = TimeSlot.of(baseDateTime.plusMinutes(30), oneHourLater.plusMinutes(30));

            // When & Then
            assertThat(slot1).isNotEqualTo(slot2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When & Then
            assertThat(slot).isNotEqualTo(null);
        }
    }

    @DisplayName("TimeSlot toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("toString이 올바른 형식으로 출력된다")
        void toStringFormat() {
            // Given
            TimeSlot slot = TimeSlot.of(baseDateTime, oneHourLater);

            // When
            String result = slot.toString();

            // Then
            assertThat(result).contains("2025-01-15 10:00");
            assertThat(result).contains("2025-01-15 11:00");
            assertThat(result).contains("60분");
        }
    }
}