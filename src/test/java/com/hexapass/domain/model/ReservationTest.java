package com.hexapass.domain.model;

import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.type.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Reservation 엔티티 테스트")
class ReservationTest {

    private final LocalDateTime futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    private final TimeSlot futureTimeSlot = TimeSlot.of(futureTime, futureTime.plusHours(1));

    @DisplayName("Reservation 생성 테스트")
    @Nested
    class CreationTest {

        @Test
        @DisplayName("유효한 정보로 예약을 생성할 수 있다")
        void createValidReservation() {
            // Given
            String reservationId = "R001";
            String memberId = "M001";
            String resourceId = "RES001";

            // When
            Reservation reservation = Reservation.create(reservationId, memberId, resourceId, futureTimeSlot);

            // Then
            assertThat(reservation.getReservationId()).isEqualTo(reservationId);
            assertThat(reservation.getMemberId()).isEqualTo(memberId);
            assertThat(reservation.getResourceId()).isEqualTo(resourceId);
            assertThat(reservation.getTimeSlot()).isEqualTo(futureTimeSlot);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(reservation.getCreatedAt()).isNotNull();
            assertThat(reservation.isActive()).isFalse(); // REQUESTED는 아직 active하지 않음
        }

        @Test
        @DisplayName("메모가 포함된 예약을 생성할 수 있다")
        void createReservationWithNotes() {
            // Given
            String notes = "창가 자리 요청";

            // When
            Reservation reservation = Reservation.createWithNotes("R001", "M001", "RES001", futureTimeSlot, notes);

            // Then
            assertThat(reservation.getNotes()).isEqualTo(notes);
        }

        @Test
        @DisplayName("예약 생성 시 초기 상태 이력이 기록된다")
        void createReservationRecordsInitialHistory() {
            // When
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // Then
            List<Reservation.StatusChangeHistory> history = reservation.getStatusHistory();
            assertThat(history).hasSize(1);

            Reservation.StatusChangeHistory initialHistory = history.get(0);
            assertThat(initialHistory.getFromStatus()).isNull(); // 초기 생성
            assertThat(initialHistory.getToStatus()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(initialHistory.getReason()).isEqualTo("예약 생성");
            assertThat(initialHistory.getChangedAt()).isNotNull();
        }

        @ParameterizedTest
        @DisplayName("필수 필드가 null이거나 빈 값이면 예외가 발생한다")
        @MethodSource("provideInvalidStringFields")
        void createWithInvalidStringFields(String reservationId, String memberId, String resourceId) {
            // When & Then
            assertThatThrownBy(() -> Reservation.create(reservationId, memberId, resourceId, futureTimeSlot))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null이거나 빈 값일 수 없습니다");
        }

        static Stream<Arguments> provideInvalidStringFields() {
            return Stream.of(
                    Arguments.of(null, "M001", "RES001"),
                    Arguments.of("", "M001", "RES001"),
                    Arguments.of("  ", "M001", "RES001"),
                    Arguments.of("R001", null, "RES001"),
                    Arguments.of("R001", "", "RES001"),
                    Arguments.of("R001", "  ", "RES001"),
                    Arguments.of("R001", "M001", null),
                    Arguments.of("R001", "M001", ""),
                    Arguments.of("R001", "M001", "  ")
            );
        }

        @Test
        @DisplayName("null TimeSlot으로 생성하면 예외가 발생한다")
        void createWithNullTimeSlot() {
            // When & Then
            assertThatThrownBy(() -> Reservation.create("R001", "M001", "RES001", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("예약 시간대는 null일 수 없습니다");
        }

        @Test
        @DisplayName("과거 시간으로 예약하면 예외가 발생한다")
        void createWithPastTime() {
            // Given
            LocalDateTime pastTime = LocalDateTime.now().minusHours(2);
            TimeSlot pastTimeSlot = TimeSlot.of(pastTime, pastTime.plusHours(1));

            // When & Then
            assertThatThrownBy(() -> Reservation.create("R001", "M001", "RES001", pastTimeSlot))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("과거 시간으로는 예약할 수 없습니다");
        }

        @Test
        @DisplayName("너무 먼 미래 시간으로 예약하면 예외가 발생한다")
        void createWithTooFarFutureTime() {
            // Given
            LocalDateTime farFutureTime = LocalDateTime.now().plusDays(400); // 1년 초과
            TimeSlot farFutureTimeSlot = TimeSlot.of(farFutureTime, farFutureTime.plusHours(1));

            // When & Then
            assertThatThrownBy(() -> Reservation.create("R001", "M001", "RES001", farFutureTimeSlot))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("예약은 최대 1년 후까지만 가능합니다");
        }
    }

    @DisplayName("Reservation 상태 전이 테스트")
    @Nested
    class StatusTransitionTest {

        private Reservation reservation;

        @Test
        @DisplayName("REQUESTED 상태에서 CONFIRMED로 전환할 수 있다")
        void transitionFromRequestedToConfirmed() {
            // Given
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);

            // When
            reservation.confirm();

            // Then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(reservation.getConfirmedAt()).isNotNull();
            assertThat(reservation.isActive()).isTrue();

            // 상태 이력 확인
            assertThat(reservation.getStatusHistory()).hasSize(2);
            assertThat(reservation.getLatestStatusChange().getToStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("CONFIRMED 상태에서 IN_USE로 전환할 수 있다")
        void transitionFromConfirmedToInUse() {
            // Given
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.confirm();

            // When
            reservation.startUsing();

            // Then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.IN_USE);
            assertThat(reservation.getStartedAt()).isNotNull();
            assertThat(reservation.isActive()).isTrue();
        }

        @Test
        @DisplayName("IN_USE 상태에서 COMPLETED로 전환할 수 있다")
        void transitionFromInUseToCompleted() {
            // Given
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.confirm();
            reservation.startUsing();

            // When
            reservation.complete();

            // Then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(reservation.getCompletedAt()).isNotNull();
            assertThat(reservation.isActive()).isFalse();
            assertThat(reservation.isFinal()).isTrue();
        }

        @Test
        @DisplayName("여러 상태에서 CANCELLED로 전환할 수 있다")
        void transitionToCancelledFromVariousStates() {
            // REQUESTED -> CANCELLED
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.cancel("사용자 취소");
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCancellationReason()).isEqualTo("사용자 취소");

            // CONFIRMED -> CANCELLED
            reservation = Reservation.create("R002", "M001", "RES001", futureTimeSlot);
            reservation.confirm();
            reservation.cancel("일정 변경");
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

            // IN_USE -> CANCELLED
            reservation = Reservation.create("R003", "M001", "RES001", futureTimeSlot);
            reservation.confirm();
            reservation.startUsing();
            reservation.cancel("응급상황");
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("시스템에 의한 자동 취소가 가능하다")
        void autoCancel() {
            // Given
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When
            reservation.autoCancel("결제 실패");

            // Then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(reservation.getCancellationReason()).isEqualTo("시스템 자동 취소: 결제 실패");
        }

        @ParameterizedTest
        @DisplayName("잘못된 상태 전이 시 예외가 발생한다")
        @EnumSource(value = ReservationStatus.class, names = {"COMPLETED", "CANCELLED"})
        void invalidStatusTransitionThrowsException(ReservationStatus finalStatus) {
            // Given - 최종 상태의 예약
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            // 강제로 최종 상태로 만들기 위해 리플렉션 사용하지 않고 정상 경로로 만듦
            if (finalStatus == ReservationStatus.COMPLETED) {
                reservation.confirm();
                reservation.startUsing();
                reservation.complete();
            } else if (finalStatus == ReservationStatus.CANCELLED) {
                reservation.cancel("테스트 취소");
            }

            // When & Then - 최종 상태에서는 어떤 전이도 불가능
            assertThatThrownBy(() -> reservation.confirm())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("예약 상태를");
        }

        @Test
        @DisplayName("REQUESTED에서 IN_USE로 직접 전환하면 예외가 발생한다")
        void cannotSkipConfirmedState() {
            // Given
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When & Then
            assertThatThrownBy(() -> reservation.startUsing())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("예약 상태를");
        }

        @Test
        @DisplayName("취소 사유 없이 취소하면 예외가 발생한다")
        void cannotCancelWithoutReason() {
            // Given
            reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When & Then
            assertThatThrownBy(() -> reservation.cancel(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("취소 사유는 null이거나 빈 값일 수 없습니다");

            assertThatThrownBy(() -> reservation.cancel(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("취소 사유는 null이거나 빈 값일 수 없습니다");
        }
    }

    @DisplayName("Reservation 충돌 검사 테스트")
    @Nested
    class ConflictTest {

        @Test
        @DisplayName("같은 리소스의 겹치는 시간대는 충돌한다")
        void detectConflictWithSameResourceOverlappingTime() {
            // Given
            Reservation reservation1 = Reservation.create("R001", "M001", "RES001",
                    TimeSlot.of(futureTime, futureTime.plusHours(2)));
            Reservation reservation2 = Reservation.create("R002", "M002", "RES001",
                    TimeSlot.of(futureTime.plusHours(1), futureTime.plusHours(3)));

            // When & Then
            assertThat(reservation1.conflictsWith(reservation2)).isTrue();
            assertThat(reservation2.conflictsWith(reservation1)).isTrue();
        }

        @Test
        @DisplayName("다른 리소스는 시간이 겹쳐도 충돌하지 않는다")
        void noConflictWithDifferentResource() {
            // Given
            Reservation reservation1 = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            Reservation reservation2 = Reservation.create("R002", "M002", "RES002", futureTimeSlot); // 다른 리소스

            // When & Then
            assertThat(reservation1.conflictsWith(reservation2)).isFalse();
            assertThat(reservation2.conflictsWith(reservation1)).isFalse();
        }

        @Test
        @DisplayName("같은 리소스의 인접한 시간대는 충돌하지 않는다")
        void noConflictWithAdjacentTime() {
            // Given
            Reservation reservation1 = Reservation.create("R001", "M001", "RES001",
                    TimeSlot.of(futureTime, futureTime.plusHours(1)));
            Reservation reservation2 = Reservation.create("R002", "M002", "RES001",
                    TimeSlot.of(futureTime.plusHours(1), futureTime.plusHours(2))); // 바로 이어지는 시간

            // When & Then
            assertThat(reservation1.conflictsWith(reservation2)).isFalse();
            assertThat(reservation2.conflictsWith(reservation1)).isFalse();
        }

        @Test
        @DisplayName("TimeSlot과의 충돌을 확인할 수 있다")
        void checkConflictWithTimeSlot() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            TimeSlot overlappingSlot = TimeSlot.of(futureTime.plusMinutes(30), futureTime.plusHours(1).plusMinutes(30));
            TimeSlot nonOverlappingSlot = TimeSlot.of(futureTime.plusHours(2), futureTime.plusHours(3));

            // When & Then
            assertThat(reservation.conflictsWith(overlappingSlot)).isTrue();
            assertThat(reservation.conflictsWith(nonOverlappingSlot)).isFalse();
        }

        @Test
        @DisplayName("null과는 충돌하지 않는다")
        void noConflictWithNull() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When & Then
            assertThat(reservation.conflictsWith((Reservation) null)).isFalse();
            assertThat(reservation.conflictsWith((TimeSlot) null)).isFalse();
        }
    }

    @DisplayName("Reservation 상태 정보 테스트")
    @Nested
    class StatusInformationTest {

        @Test
        @DisplayName("예약 상태별 활성/최종 상태를 올바르게 판단한다")
        void determineStatusProperties() {
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // REQUESTED - 아직 활성 아님
            assertThat(reservation.isActive()).isFalse();
            assertThat(reservation.isFinal()).isFalse();
            assertThat(reservation.isCancellable()).isTrue();

            // CONFIRMED - 활성 상태
            reservation.confirm();
            assertThat(reservation.isActive()).isTrue();
            assertThat(reservation.isFinal()).isFalse();
            assertThat(reservation.isCancellable()).isTrue();

            // IN_USE - 활성 상태
            reservation.startUsing();
            assertThat(reservation.isActive()).isTrue();
            assertThat(reservation.isFinal()).isFalse();
            assertThat(reservation.isCancellable()).isTrue();

            // COMPLETED - 최종 상태
            reservation.complete();
            assertThat(reservation.isActive()).isFalse();
            assertThat(reservation.isFinal()).isTrue();
            assertThat(reservation.isCancellable()).isFalse();
        }

        @Test
        @DisplayName("취소된 예약의 상태를 올바르게 판단한다")
        void determineCancelledReservationStatus() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.cancel("테스트 취소");

            // When & Then
            assertThat(reservation.isActive()).isFalse();
            assertThat(reservation.isFinal()).isTrue();
            assertThat(reservation.isCancellable()).isFalse();
        }

        @Test
        @DisplayName("노쇼 여부를 올바르게 판단한다")
        void determineNoShowStatus() {
            // Given - 과거 시간의 CONFIRMED 예약 (실제로는 생성할 수 없지만 테스트를 위해 현재 시간 이후로 생성 후 시간이 지났다고 가정)
            LocalDateTime pastTime = LocalDateTime.now().minusHours(2);
            TimeSlot pastTimeSlot = TimeSlot.of(pastTime, pastTime.plusHours(1));

            // 현재 시간보다 미래 시간으로 만든 후 상태만 확인용으로 테스트
            Reservation futureReservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            futureReservation.confirm();

            Reservation requestedReservation = Reservation.create("R002", "M001", "RES001", futureTimeSlot);

            // When & Then
            assertThat(futureReservation.isNoShow()).isFalse(); // 아직 미래 시간
            assertThat(requestedReservation.isNoShow()).isFalse(); // REQUESTED 상태는 노쇼 판정 안함
        }
    }

    @DisplayName("Reservation 시간 정보 테스트")
    @Nested
    class TimeInformationTest {

        @Test
        @DisplayName("예약 소요 시간을 올바르게 계산한다")
        void calculateDuration() {
            // Given
            TimeSlot twoHourSlot = TimeSlot.of(futureTime, futureTime.plusHours(2));
            Reservation reservation = Reservation.create("R001", "M001", "RES001", twoHourSlot);

            // When & Then
            assertThat(reservation.getDurationMinutes()).isEqualTo(120L);
        }

        @Test
        @DisplayName("예약 시간까지 남은 시간을 올바르게 계산한다")
        void calculateMinutesUntilReservation() {
            // Given - 1시간 후 예약
            LocalDateTime oneHourLater = LocalDateTime.now().plusHours(1);
            TimeSlot oneHourLaterSlot = TimeSlot.of(oneHourLater, oneHourLater.plusHours(1));
            Reservation reservation = Reservation.create("R001", "M001", "RES001", oneHourLaterSlot);

            // When
            long minutesUntil = reservation.getMinutesUntilReservation();

            // Then - 약 60분 (정확히는 59분~60분 사이)
            assertThat(minutesUntil).isBetween(55L, 65L);
        }

        @Test
        @DisplayName("과거 예약의 경우 남은 시간이 0이다")
        void pastReservationHasZeroMinutesUntil() {
            // Given - 과거 시간의 예약은 직접 생성할 수 없으므로, 미래 시간으로 생성한 후 시간 경과 상황을 시뮬레이션
            LocalDateTime nearFutureTime = LocalDateTime.now().plusSeconds(1);
            TimeSlot nearFutureSlot = TimeSlot.of(nearFutureTime, nearFutureTime.plusHours(1));
            Reservation reservation = Reservation.create("R001", "M001", "RES001", nearFutureSlot);

            // When - 잠깐 기다린 후 (실제 테스트에서는 Thread.sleep 사용하지 않는 것이 좋지만 예시용)
            try {
                Thread.sleep(1100); // 1.1초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then
            assertThat(reservation.getMinutesUntilReservation()).isEqualTo(0L);
        }

        @Test
        @DisplayName("예약 변경 가능 시간을 올바르게 판단한다")
        void determineModifiability() {
            // Given - 2시간 후 예약 (1시간 전까지 수정 가능)
            LocalDateTime twoHoursLater = LocalDateTime.now().plusHours(2);
            TimeSlot twoHoursLaterSlot = TimeSlot.of(twoHoursLater, twoHoursLater.plusHours(1));
            Reservation modifiableReservation = Reservation.create("R001", "M001", "RES001", twoHoursLaterSlot);

            // 30분 후 예약 (1시간 전 초과이므로 수정 불가)
            LocalDateTime thirtyMinutesLater = LocalDateTime.now().plusMinutes(30);
            TimeSlot thirtyMinutesLaterSlot = TimeSlot.of(thirtyMinutesLater, thirtyMinutesLater.plusHours(1));
            Reservation nonModifiableReservation = Reservation.create("R002", "M001", "RES001", thirtyMinutesLaterSlot);

            // When & Then
            assertThat(modifiableReservation.isModifiable()).isTrue();
            assertThat(nonModifiableReservation.isModifiable()).isFalse();
        }

        @Test
        @DisplayName("최종 상태의 예약은 수정할 수 없다")
        void finalStatusReservationNotModifiable() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.cancel("테스트 취소");

            // When & Then
            assertThat(reservation.isModifiable()).isFalse();
        }
    }

    @DisplayName("Reservation 메타데이터 관리 테스트")
    @Nested
    class MetadataManagementTest {

        @Test
        @DisplayName("메모를 설정할 수 있다")
        void setNotes() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            String notes = "창가 자리 요청";

            // When
            reservation.setNotes(notes);

            // Then
            assertThat(reservation.getNotes()).isEqualTo(notes);
        }

        @Test
        @DisplayName("메모를 추가할 수 있다")
        void addNotes() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.setNotes("창가 자리 요청");

            // When
            reservation.addNotes("조용한 공간 선호");

            // Then
            assertThat(reservation.getNotes()).isEqualTo("창가 자리 요청 | 조용한 공간 선호");
        }

        @Test
        @DisplayName("빈 메모는 무시된다")
        void ignoreEmptyNotes() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When
            reservation.setNotes("");
            reservation.addNotes(null);
            reservation.addNotes("  ");

            // Then
            assertThat(reservation.getNotes()).isNull();
        }

        @Test
        @DisplayName("상태 변경 이력을 조회할 수 있다")
        void getStatusHistory() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When
            reservation.confirm();
            reservation.startUsing();
            reservation.complete();

            // Then
            List<Reservation.StatusChangeHistory> history = reservation.getStatusHistory();
            assertThat(history).hasSize(4); // 생성 + 확정 + 사용시작 + 완료

            assertThat(history.get(0).getToStatus()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(history.get(1).getToStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(history.get(2).getToStatus()).isEqualTo(ReservationStatus.IN_USE);
            assertThat(history.get(3).getToStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }

        @Test
        @DisplayName("최근 상태 변경 정보를 조회할 수 있다")
        void getLatestStatusChange() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.confirm();

            // When
            Reservation.StatusChangeHistory latest = reservation.getLatestStatusChange();

            // Then
            assertThat(latest.getFromStatus()).isEqualTo(ReservationStatus.REQUESTED);
            assertThat(latest.getToStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(latest.getReason()).isEqualTo("예약 확정");
        }
    }

    @DisplayName("Reservation 동등성 테스트")
    @Nested
    class EqualityTest {

        @Test
        @DisplayName("같은 reservationId를 가진 예약들은 동등하다")
        void equalityWithSameReservationId() {
            // Given
            Reservation reservation1 = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            Reservation reservation2 = Reservation.create("R001", "M002", "RES002", futureTimeSlot); // ID만 같음

            // When & Then
            assertThat(reservation1).isEqualTo(reservation2);
            assertThat(reservation1.hashCode()).isEqualTo(reservation2.hashCode());
        }

        @Test
        @DisplayName("다른 reservationId를 가진 예약들은 동등하지 않다")
        void inequalityWithDifferentReservationId() {
            // Given
            Reservation reservation1 = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            Reservation reservation2 = Reservation.create("R002", "M001", "RES001", futureTimeSlot);

            // When & Then
            assertThat(reservation1).isNotEqualTo(reservation2);
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        void inequalityWithNull() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When & Then
            assertThat(reservation).isNotEqualTo(null);
        }
    }

    @DisplayName("Reservation toString 및 summary 테스트")
    @Nested
    class DisplayTest {

        @Test
        @DisplayName("toString이 올바른 정보를 포함한다")
        void toStringContainsCorrectInfo() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);

            // When
            String result = reservation.toString();

            // Then
            assertThat(result).contains("R001");
            assertThat(result).contains("M001");
            assertThat(result).contains("RES001");
            assertThat(result).contains("REQUESTED");
        }

        @Test
        @DisplayName("getSummary가 예약 정보를 요약해서 보여준다")
        void summaryContainsReservationInfo() {
            // Given
            Reservation reservation = Reservation.createWithNotes("R001", "M001", "RES001", futureTimeSlot, "창가 자리");
            reservation.confirm();

            // When
            String summary = reservation.getSummary();

            // Then
            assertThat(summary).contains("예약 R001");
            assertThat(summary).contains("회원 M001");
            assertThat(summary).contains("리소스 RES001");
            assertThat(summary).contains("예약확정");
            assertThat(summary).contains("창가 자리");
        }

        @Test
        @DisplayName("취소된 예약의 summary에 취소 사유가 포함된다")
        void cancelledReservationSummaryIncludesReason() {
            // Given
            Reservation reservation = Reservation.create("R001", "M001", "RES001", futureTimeSlot);
            reservation.cancel("일정 변경");

            // When
            String summary = reservation.getSummary();

            // Then
            assertThat(summary).contains("예약취소");
            assertThat(summary).contains("[일정 변경]");
        }
    }
}