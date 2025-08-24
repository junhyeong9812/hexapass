package com.hexapass.domain.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DateRange 클래스 테스트")
class DateRangeTest {

    private final LocalDate today = LocalDate.now();
    private final LocalDate tomorrow = today.plusDays(1);
    private final LocalDate dayAfterTomorrow = today.plusDays(2);
    private final LocalDate nextWeek = today.plusDays(7);

    @DisplayName("DateRange 객체 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 시작일과 종료일로 DateRange를 생성할 수 있다")
        void createValidDateRange() {
            // When
            DateRange dateRange = DateRange.of(today, nextWeek);

            // Then
            assertThat(dateRange).isNotNull();
            assertThat(dateRange.getStartDate()).isEqualTo(today);
            assertThat(dateRange.getEndDate()).isEqualTo(nextWeek);
        }

        @Test
        @DisplayName("시작일과 종료일이 같은 단일 날짜 범위를 생성할 수 있다")
        void createSingleDayRange() {
            // When
            DateRange singleDay = DateRange.singleDay(today);

            // Then
            assertThat(singleDay.getStartDate()).isEqualTo(today);
            assertThat(singleDay.getEndDate()).isEqualTo(today);
            assertThat(singleDay.isSingleDay()).isTrue();
            assertThat(singleDay.getDays()).isEqualTo(1L);
        }

        @Test
        @DisplayName("오늘부터 지정된 일수만큼의 범위를 생성할 수 있다")
        void createFromTodayForDays() {
            // When
            DateRange range = DateRange.fromTodayFor(7);

            // Then
            assertThat(range.getStartDate()).isEqualTo(today);
            assertThat(range.getEndDate()).isEqualTo(today.plusDays(6)); // 7일 포함하므로 +6
            assertThat(range.getDays()).isEqualTo(7L);
        }

        @Test
        @DisplayName("지정된 날짜부터 일정 기간의 범위를 생성할 수 있다")
        void createFromDateForDays() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);

            // When
            DateRange range = DateRange.fromDateFor(startDate, 30);

            // Then
            assertThat(range.getStartDate()).isEqualTo(startDate);
            assertThat(range.getEndDate()).isEqualTo(startDate.plusDays(29)); // 30일 포함
            assertThat(range.getDays()).isEqualTo(30L);
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외가 발생한다")
        void createWithStartDateAfterEndDate() {
            // Given
            LocalDate startDate = tomorrow;
            LocalDate endDate = today;

            // When & Then
            assertThatThrownBy(() -> DateRange.of(startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("시작일은 종료일보다 이전이거나 같아야 합니다");
        }

        @ParameterizedTest
        @DisplayName("null 날짜로 생성하면 예외가 발생한다")
        @MethodSource("provideNullDateCombinations")
        void createWithNullDate(LocalDate startDate, LocalDate endDate) {
            // When & Then
            assertThatThrownBy(() -> DateRange.of(startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null일 수 없습니다");
        }

        static Stream<Arguments> provideNullDateCombinations() {
            LocalDate validDate = LocalDate.now();
            return Stream.of(
                    Arguments.of(null, validDate),
                    Arguments.of(validDate, null),
                    Arguments.of(null, null)
            );
        }

        @ParameterizedTest
        @DisplayName("0 이하의 일수로 생성하면 예외가 발생한다")
        @ValueSource(ints = {0, -1, -10})
        void createWithInvalidDays(int invalidDays) {
            // When & Then
            assertThatThrownBy(() -> DateRange.fromTodayFor(invalidDays))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("일수는 1 이상이어야 합니다");
        }
    }

    @DisplayName("DateRange 겹침 검사 테스트")
    @Nested
    class OverlapTest {

        @Test
        @DisplayName("완전히 겹치는 범위들을 올바르게 판단한다")
        void detectCompleteOverlap() {
            // Given
            DateRange range1 = DateRange.of(today, nextWeek);
            DateRange range2 = DateRange.of(today.plusDays(1), nextWeek.minusDays(1));

            // When & Then
            assertThat(range1.overlaps(range2)).isTrue();
            assertThat(range2.overlaps(range1)).isTrue();
        }

        @Test
        @DisplayName("부분적으로 겹치는 범위들을 올바르게 판단한다")
        void detectPartialOverlap() {
            // Given
            DateRange range1 = DateRange.of(today, today.plusDays(5));
            DateRange range2 = DateRange.of(today.plusDays(3), today.plusDays(8));

            // When & Then
            assertThat(range1.overlaps(range2)).isTrue();
            assertThat(range2.overlaps(range1)).isTrue();
        }

        @Test
        @DisplayName("경계에서 만나는 범위들은 겹치는 것으로 판단한다")
        void detectBoundaryOverlap() {
            // Given
            DateRange range1 = DateRange.of(today, today.plusDays(5));
            DateRange range2 = DateRange.of(today.plusDays(5), today.plusDays(10));

            // When & Then
            assertThat(range1.overlaps(range2)).isTrue();
            assertThat(range2.overlaps(range1)).isTrue();
        }

        @Test
        @DisplayName("완전히 분리된 범위들은 겹치지 않는다")
        void detectNoOverlap() {
            // Given
            DateRange range1 = DateRange.of(today, today.plusDays(3));
            DateRange range2 = DateRange.of(today.plusDays(5), today.plusDays(8));

            // When & Then
            assertThat(range1.overlaps(range2)).isFalse();
            assertThat(range2.overlaps(range1)).isFalse();
        }

        @Test
        @DisplayName("null과는 겹치지 않는다")
        void noOverlapWithNull() {
            // Given
            DateRange range = DateRange.of(today, nextWeek);

            // When & Then
            assertThat(range.overlaps(null)).isFalse();
        }
    }

    @DisplayName("DateRange 포함 검사 테스트")
    @Nested
    class ContainsTest {

        @Test
        @DisplayName("범위 내의 날짜를 포함한다고 판단한다")
        void containsDateWithinRange() {
            // Given
            DateRange range = DateRange.of(today, nextWeek);

            // When & Then
            assertThat(range.contains(today)).isTrue(); // 시작일 포함
            assertThat(range.contains(nextWeek)).isTrue(); // 종료일 포함
            assertThat(range.contains(today.plusDays(3))).isTrue(); // 중간 날짜 포함
        }

        @Test
        @DisplayName("범위 밖의 날짜를 포함하지 않는다고 판단한다")
        void doesNotContainDateOutsideRange() {
            // Given
            DateRange range = DateRange.of(today.plusDays(1), today.plusDays(5));

            // When & Then
            assertThat(range.contains(today)).isFalse(); // 시작일 이전
            assertThat(range.contains(today.plusDays(6))).isFalse(); // 종료일 이후
        }

        @Test
        @DisplayName("다른 범위가 완전히 포함되는지 확인한다")
        void containsAnotherRange() {
            // Given
            DateRange outerRange = DateRange.of(today, today.plusDays(10));
            DateRange innerRange = DateRange.of(today.plusDays(2), today.plusDays(8));
            DateRange overlappingRange = DateRange.of(today.plusDays(5), today.plusDays(15));

            // When & Then
            assertThat(outerRange.contains(innerRange)).isTrue();
            assertThat(outerRange.contains(overlappingRange)).isFalse();
            assertThat(innerRange.isContainedBy(outerRange)).isTrue();
        }

        @Test
        @DisplayName("null 날짜는 포함하지 않는다")
        void doesNotContainNull() {
            // Given
            DateRange range = DateRange.of(today, nextWeek);

            // When & Then
            assertThat(range.contains((LocalDate) null)).isFalse();
            assertThat(range.contains((DateRange) null)).isFalse();
        }
    }

    @DisplayName("DateRange 인접성 검사 테스트")
    @Nested
    class AdjacencyTest {

        @Test
        @DisplayName("바로 이어지는 범위들을 인접하다고 판단한다")
        void detectAdjacency() {
            // Given
            DateRange range1 = DateRange.of(today, today.plusDays(5));
            DateRange range2 = DateRange.of(today.plusDays(6), today.plusDays(10));

            // When & Then
            assertThat(range1.isAdjacentTo(range2)).isTrue();
            assertThat(range2.isAdjacentTo(range1)).isTrue();
        }

        @Test
        @DisplayName("겹치는 범위들은 인접하지 않는다")
        void overlappingRangesNotAdjacent() {
            // Given
            DateRange range1 = DateRange.of(today, today.plusDays(5));
            DateRange range2 = DateRange.of(today.plusDays(3), today.plusDays(8));

            // When & Then
            assertThat(range1.isAdjacentTo(range2)).isFalse();
            assertThat(range2.isAdjacentTo(range1)).isFalse();
        }

        @Test
        @DisplayName("분리된 범위들은 인접하지 않는다")
        void separatedRangesNotAdjacent() {
            // Given
            DateRange range1 = DateRange.of(today, today.plusDays(3));
            DateRange range2 = DateRange.of(today.plusDays(6), today.plusDays(10));

            // When & Then
            assertThat(range1.isAdjacentTo(range2)).isFalse();
            assertThat(range2.isAdjacentTo(range1)).isFalse();
        }
    }

    @DisplayName("DateRange 시간 정보 테스트")
    @Nested
    class TimeInformationTest {

        @Test
        @DisplayName("날짜 범위의 일수를 정확히 계산한다")
        void calculateDaysCorrectly() {
            // Given
            DateRange singleDay = DateRange.singleDay(today);
            DateRange weekRange = DateRange.of(today, today.plusDays(6)); // 7일
            DateRange monthRange = DateRange.of(today, today.plusDays(29)); // 30일

            // When & Then
            assertThat(singleDay.getDays()).isEqualTo(1L);
            assertThat(weekRange.getDays()).isEqualTo(7L);
            assertThat(monthRange.getDays()).isEqualTo(30L);
        }

        @Test
        @DisplayName("현재 시점을 기준으로 과거/현재/미래를 판단한다")
        void determineTimeStatus() {
            // Given
            DateRange pastRange = DateRange.of(today.minusDays(10), today.minusDays(5));
            DateRange currentRange = DateRange.of(today.minusDays(2), today.plusDays(2));
            DateRange futureRange = DateRange.of(today.plusDays(5), today.plusDays(10));

            // When & Then
            assertThat(pastRange.isPast()).isTrue();
            assertThat(pastRange.isFuture()).isFalse();
            assertThat(pastRange.isCurrent()).isFalse();

            assertThat(currentRange.isPast()).isFalse();
            assertThat(currentRange.isFuture()).isFalse();
            assertThat(currentRange.isCurrent()).isTrue();

            assertThat(futureRange.isPast()).isFalse();
            assertThat(futureRange.isFuture()).isTrue();
            assertThat(futureRange.isCurrent()).isFalse();
        }

        @Test
        @DisplayName("단일 날짜 범위인지 확인한다")
        void checkSingleDay() {
            // Given
            DateRange singleDay = DateRange.singleDay(today);
            DateRange multiDay = DateRange.of(today, tomorrow);

            // When & Then
            assertThat(singleDay.isSingleDay()).isTrue();
            assertThat(multiDay.isSingleDay()).isFalse();
        }
    }

    @DisplayName("DateRange 변환 테스트")
    @Nested
    class TransformationTest {

        @Test
        @DisplayName("시작일을 변경한 새로운 범위를 만들 수 있다")
        void changeStartDate() {
            // Given
            DateRange original = DateRange.of(today, nextWeek);
            LocalDate newStartDate = today.plusDays(2);

            // When
            DateRange modified = original.withStartDate(newStartDate);

            // Then
            assertThat(modified.getStartDate()).isEqualTo(newStartDate);
            assertThat(modified.getEndDate()).isEqualTo(nextWeek);
            // 원본은 변경되지 않음
            assertThat(original.getStartDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("종료일을 변경한 새로운 범위를 만들 수 있다")
        void changeEndDate() {
            // Given
            DateRange original = DateRange.of(today, nextWeek);
            LocalDate newEndDate = nextWeek.plusDays(3);

            // When
            DateRange modified = original.withEndDate(newEndDate);

            // Then
            assertThat(modified.getStartDate()).isEqualTo(today);
            assertThat(modified.getEndDate()).isEqualTo(newEndDate);
            // 원본은 변경되지 않음
            assertThat(original.getEndDate()).isEqualTo(nextWeek);
        }

        @Test
        @DisplayName("기간을 연장할 수 있다")
        void extendRange() {
            // Given
            DateRange original = DateRange.of(today, today.plusDays(7));

            // When
            DateRange extended = original.extend(3);

            // Then
            assertThat(extended.getStartDate()).isEqualTo(today);
            assertThat(extended.getEndDate()).isEqualTo(today.plusDays(10));
            assertThat(extended.getDays()).isEqualTo(11L);
        }

        @Test
        @DisplayName("기간을 단축할 수 있다")
        void shortenRange() {
            // Given
            DateRange original = DateRange.of(today, today.plusDays(10));

            // When
            DateRange shortened = original.shorten(3);

            // Then
            assertThat(shortened.getStartDate()).isEqualTo(today);
            assertThat(shortened.getEndDate()).isEqualTo(today.plusDays(7));
            assertThat(shortened.getDays()).isEqualTo(8L);
        }

        @Test
        @DisplayName("과도한 단축은 예외가 발생한다")
        void excessiveShorteningThrowsException() {
            // Given
            DateRange shortRange = DateRange.of(today, today.plusDays(2));

            // When & Then
            assertThatThrownBy(() -> shortRange.shorten(5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("단축 후 종료일이 시작일보다 이전이 될 수 없습니다");
        }

        @ParameterizedTest
        @DisplayName("음수로 연장/단축하면 예외가 발생한다")
        @ValueSource(ints = {-1, -5, -10})
        void negativeExtensionOrShorteningThrowsException(int negativeDays) {
            // Given
            DateRange range = DateRange.of(today, nextWeek);

            // When & Then
            assertThatThrownBy(() -> range.extend(negativeDays))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> range.shorten(negativeDays))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @DisplayName("DateRange 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 시작일과 종료일을 가진 범위들은 동등하다")
        void equalityWithSameDates() {
            // Given
            DateRange range1 = DateRange.of(today, nextWeek);
            DateRange range2 = DateRange.of(today, nextWeek);

            // When & Then
            assertThat(range1).isEqualTo(range2);
            assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
        }

        @Test
        @DisplayName("다른 날짜를 가진 범위들은 동등하지 않다")
        void inequalityWithDifferentDates() {
            // Given
            DateRange range1 = DateRange.of(today, nextWeek);
            DateRange range2 = DateRange.of(tomorrow, nextWeek);

            // When & Then
            assertThat(range1).isNotEqualTo(range2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            DateRange range = DateRange.of(today, nextWeek);

            // When & Then
            assertThat(range).isNotEqualTo(null);
        }
    }

    @DisplayName("DateRange toString 테스트")
    @Nested
    class ToStringTest {

        @Test
        @DisplayName("단일 날짜 범위의 toString은 날짜만 표시한다")
        void singleDayToString() {
            // Given
            DateRange singleDay = DateRange.singleDay(today);

            // When
            String result = singleDay.toString();

            // Then
            assertThat(result).isEqualTo(today.toString());
        }

        @Test
        @DisplayName("다중 날짜 범위의 toString은 범위와 일수를 표시한다")
        void multiDayToString() {
            // Given
            DateRange range = DateRange.of(today, today.plusDays(6)); // 7일

            // When
            String result = range.toString();

            // Then
            assertThat(result).contains(today.toString());
            assertThat(result).contains(today.plusDays(6).toString());
            assertThat(result).contains("7일");
        }
    }
}