package com.hexapass.domain.port.outbound;

import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.model.Reservation;
import com.hexapass.domain.type.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 정보 저장소 포트 (Outbound Port)
 * 예약과 관련된 복잡한 조회 조건들을 추상화
 */
public interface ReservationRepository {

    // =========================
    // 기본 CRUD 연산
    // =========================

    /**
     * 예약 ID로 예약 조회
     */
    Optional<Reservation> findById(String reservationId);

    /**
     * 예약 저장 (생성/수정)
     */
    void save(Reservation reservation);

    /**
     * 예약 삭제
     */
    void delete(String reservationId);

    /**
     * 예약 존재 여부 확인
     */
    boolean existsById(String reservationId);

    // =========================
    // 회원별 예약 조회
    // =========================

    /**
     * 회원의 모든 예약 조회
     */
    List<Reservation> findByMemberId(String memberId);

    /**
     * 회원의 활성 예약 조회 (CONFIRMED, IN_USE)
     */
    List<Reservation> findActiveReservationsByMember(String memberId);

    /**
     * 회원의 상태별 예약 조회
     */
    List<Reservation> findByMemberIdAndStatus(String memberId, ReservationStatus status);

    /**
     * 회원의 특정 기간 예약 조회
     */
    List<Reservation> findByMemberIdAndDateRange(String memberId, LocalDate startDate, LocalDate endDate);

    /**
     * 회원의 미래 예약 조회
     */
    List<Reservation> findFutureReservationsByMember(String memberId);

    // =========================
    // 리소스별 예약 조회
    // =========================

    /**
     * 리소스의 모든 예약 조회
     */
    List<Reservation> findByResourceId(String resourceId);

    /**
     * 리소스의 특정 날짜 예약 조회
     */
    List<Reservation> findByResourceIdAndDate(String resourceId, LocalDate date);

    /**
     * 리소스의 특정 기간 예약 조회
     */
    List<Reservation> findByResourceIdAndDateRange(String resourceId, LocalDate startDate, LocalDate endDate);

    /**
     * 리소스의 활성 예약 조회
     */
    List<Reservation> findActiveReservationsByResource(String resourceId);

    // =========================
    // 시간 충돌 검사용 메서드들
    // =========================

    /**
     * 특정 리소스와 시간대에 충돌하는 예약 조회
     */
    List<Reservation> findConflictingReservations(String resourceId, TimeSlot timeSlot);

    /**
     * 특정 리소스와 시간대에 충돌하는 활성 예약 조회
     */
    List<Reservation> findConflictingActiveReservations(String resourceId, TimeSlot timeSlot);

    /**
     * 회원의 특정 시간대와 충돌하는 예약 조회 (다른 리소스 포함)
     */
    List<Reservation> findMemberConflictingReservations(String memberId, TimeSlot timeSlot);

    // =========================
    // 상태별 조회
    // =========================

    /**
     * 모든 예약 조회
     */
    List<Reservation> findAll();

    /**
     * 상태별 예약 조회
     */
    List<Reservation> findByStatus(ReservationStatus status);

    /**
     * 요청 상태 예약 조회 (확정 대기중)
     */
    default List<Reservation> findPendingReservations() {
        return findByStatus(ReservationStatus.REQUESTED);
    }

    /**
     * 확정된 예약 조회
     */
    default List<Reservation> findConfirmedReservations() {
        return findByStatus(ReservationStatus.CONFIRMED);
    }

    /**
     * 취소된 예약 조회
     */
    default List<Reservation> findCancelledReservations() {
        return findByStatus(ReservationStatus.CANCELLED);
    }

    // =========================
    // 노쇼 및 특별 조건 조회
    // =========================

    /**
     * 노쇼 예약 조회 (확정 상태인데 시간이 지난 예약)
     */
    List<Reservation> findNoShowReservations(LocalDateTime cutoffTime);

    /**
     * 자동 취소 대상 예약 조회 (오래된 미확정 예약 등)
     */
    List<Reservation> findReservationsForAutoCancellation(LocalDateTime cutoffTime);

    /**
     * 특정 시간 이후 시작하는 예약 조회
     */
    List<Reservation> findReservationsAfter(LocalDateTime dateTime);

    /**
     * 특정 시간 이전 시작하는 예약 조회
     */
    List<Reservation> findReservationsBefore(LocalDateTime dateTime);

    // =========================
    // 통계 및 집계
    // =========================

    /**
     * 전체 예약 수
     */
    long countAll();

    /**
     * 상태별 예약 수
     */
    long countByStatus(ReservationStatus status);

    /**
     * 회원별 예약 수
     */
    long countByMemberId(String memberId);

    /**
     * 리소스별 예약 수
     */
    long countByResourceId(String resourceId);

    /**
     * 특정 기간의 예약 수
     */
    long countReservationsBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 특정 날짜의 예약 수
     */
    long countReservationsOnDate(LocalDate date);

    // =========================
    // 배치 처리용 메서드들
    // =========================

    /**
     * 여러 예약 일괄 저장
     */
    void saveAll(List<Reservation> reservations);

    /**
     * 페이징 처리된 예약 목록 조회
     */
    List<Reservation> findAll(int page, int size);

    /**
     * 상태별 페이징 처리된 예약 목록 조회
     */
    List<Reservation> findByStatus(ReservationStatus status, int page, int size);
}