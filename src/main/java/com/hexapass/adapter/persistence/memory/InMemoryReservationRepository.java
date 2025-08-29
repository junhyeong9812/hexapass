package com.hexapass.adapter.persistence.memory;

import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.model.Reservation;
import com.hexapass.domain.port.outbound.ReservationRepository;
import com.hexapass.domain.type.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 예약 저장소의 인메모리 구현체
 */
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.getReservationId(), reservation);
    }

    @Override
    public void delete(String reservationId) {
        reservations.remove(reservationId);
    }

    @Override
    public boolean existsById(String reservationId) {
        return reservations.containsKey(reservationId);
    }

    @Override
    public List<Reservation> findByMemberId(String memberId) {
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findActiveReservationsByMember(String memberId) {
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .filter(Reservation::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByMemberIdAndStatus(String memberId, ReservationStatus status) {
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .filter(reservation -> status.equals(reservation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByMemberIdAndDateRange(String memberId, LocalDate startDate, LocalDate endDate) {
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .filter(reservation -> {
                    LocalDate reservationDate = reservation.getTimeSlot().getStartTime().toLocalDate();
                    return !reservationDate.isBefore(startDate) && !reservationDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findFutureReservationsByMember(String memberId) {
        LocalDateTime now = LocalDateTime.now();
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .filter(reservation -> reservation.getTimeSlot().getStartTime().isAfter(now))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByResourceId(String resourceId) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByResourceIdAndDate(String resourceId, LocalDate date) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .filter(reservation -> {
                    LocalDate reservationDate = reservation.getTimeSlot().getStartTime().toLocalDate();
                    return date.equals(reservationDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByResourceIdAndDateRange(String resourceId, LocalDate startDate, LocalDate endDate) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .filter(reservation -> {
                    LocalDate reservationDate = reservation.getTimeSlot().getStartTime().toLocalDate();
                    return !reservationDate.isBefore(startDate) && !reservationDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findActiveReservationsByResource(String resourceId) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .filter(Reservation::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findConflictingReservations(String resourceId, TimeSlot timeSlot) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .filter(reservation -> reservation.conflictsWith(timeSlot))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findConflictingActiveReservations(String resourceId, TimeSlot timeSlot) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .filter(Reservation::isActive)
                .filter(reservation -> reservation.conflictsWith(timeSlot))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findMemberConflictingReservations(String memberId, TimeSlot timeSlot) {
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .filter(Reservation::isActive)
                .filter(reservation -> reservation.conflictsWith(timeSlot))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }

    @Override
    public List<Reservation> findByStatus(ReservationStatus status) {
        return reservations.values().stream()
                .filter(reservation -> status.equals(reservation.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findNoShowReservations(LocalDateTime cutoffTime) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CONFIRMED)
                .filter(reservation -> reservation.getTimeSlot().getEndTime().isBefore(cutoffTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findReservationsForAutoCancellation(LocalDateTime cutoffTime) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.REQUESTED)
                .filter(reservation -> reservation.getCreatedAt().isBefore(cutoffTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findReservationsAfter(LocalDateTime dateTime) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getTimeSlot().getStartTime().isAfter(dateTime))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findReservationsBefore(LocalDateTime dateTime) {
        return reservations.values().stream()
                .filter(reservation -> reservation.getTimeSlot().getStartTime().isBefore(dateTime))
                .collect(Collectors.toList());
    }

    @Override
    public long countAll() {
        return reservations.size();
    }

    @Override
    public long countByStatus(ReservationStatus status) {
        return reservations.values().stream()
                .filter(reservation -> status.equals(reservation.getStatus()))
                .count();
    }

    @Override
    public long countByMemberId(String memberId) {
        return reservations.values().stream()
                .filter(reservation -> memberId.equals(reservation.getMemberId()))
                .count();
    }

    @Override
    public long countByResourceId(String resourceId) {
        return reservations.values().stream()
                .filter(reservation -> resourceId.equals(reservation.getResourceId()))
                .count();
    }

    @Override
    public long countReservationsBetween(LocalDate startDate, LocalDate endDate) {
        return reservations.values().stream()
                .filter(reservation -> {
                    LocalDate reservationDate = reservation.getTimeSlot().getStartTime().toLocalDate();
                    return !reservationDate.isBefore(startDate) && !reservationDate.isAfter(endDate);
                })
                .count();
    }

    @Override
    public long countReservationsOnDate(LocalDate date) {
        return reservations.values().stream()
                .filter(reservation -> {
                    LocalDate reservationDate = reservation.getTimeSlot().getStartTime().toLocalDate();
                    return date.equals(reservationDate);
                })
                .count();
    }

    @Override
    public void saveAll(List<Reservation> reservationList) {
        for (Reservation reservation : reservationList) {
            save(reservation);
        }
    }

    @Override
    public List<Reservation> findAll(int page, int size) {
        List<Reservation> allReservations = findAll();
        int start = page * size;
        int end = Math.min(start + size, allReservations.size());

        if (start >= allReservations.size()) {
            return new ArrayList<>();
        }

        return allReservations.subList(start, end);
    }

    @Override
    public List<Reservation> findByStatus(ReservationStatus status, int page, int size) {
        List<Reservation> statusReservations = findByStatus(status);
        int start = page * size;
        int end = Math.min(start + size, statusReservations.size());

        if (start >= statusReservations.size()) {
            return new ArrayList<>();
        }

        return statusReservations.subList(start, end);
    }

    // 개발/테스트용 유틸리티 메서드들
    public void clear() {
        reservations.clear();
    }

    public int size() {
        return reservations.size();
    }

    public boolean isEmpty() {
        return reservations.isEmpty();
    }
}