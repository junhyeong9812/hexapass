package com.hexapass.domain.policy;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.type.ResourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약 컨텍스트 정보
 * 예약 정책 판단에 필요한 모든 정보를 담고 있는 객체
 */
public class ReservationContext {

    private final Member member;
    private final String resourceId;
    private final ResourceType resourceType;
    private final LocalDateTime reservationTime;
    private final int currentActiveReservations;
    private final int resourceCurrentOccupancy;
    private final int resourceCapacity;

    private ReservationContext(Member member, String resourceId, ResourceType resourceType,
                               LocalDateTime reservationTime, int currentActiveReservations,
                               int resourceCurrentOccupancy, int resourceCapacity) {
        this.member = validateNotNull(member, "회원");
        this.resourceId = validateNotBlank(resourceId, "리소스 ID");
        this.resourceType = validateNotNull(resourceType, "리소스 타입");
        this.reservationTime = validateNotNull(reservationTime, "예약 시간");
        this.currentActiveReservations = validateNonNegative(currentActiveReservations, "현재 활성 예약 수");
        this.resourceCurrentOccupancy = validateNonNegative(resourceCurrentOccupancy, "리소스 현재 이용자 수");
        this.resourceCapacity = validatePositive(resourceCapacity, "리소스 수용 인원");
    }

    /**
     * 예약 컨텍스트 생성
     */
    public static ReservationContext create(Member member, String resourceId, ResourceType resourceType,
                                            LocalDateTime reservationTime, int currentActiveReservations,
                                            int resourceCurrentOccupancy, int resourceCapacity) {
        return new ReservationContext(member, resourceId, resourceType, reservationTime,
                currentActiveReservations, resourceCurrentOccupancy, resourceCapacity);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public Member getMember() {
        return member;
    }

    public String getResourceId() {
        return resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public int getCurrentActiveReservations() {
        return currentActiveReservations;
    }

    public int getResourceCurrentOccupancy() {
        return resourceCurrentOccupancy;
    }

    public int getResourceCapacity() {
        return resourceCapacity;
    }

    // =========================
    // Helper 메서드들
    // =========================

    /**
     * 예약 날짜 반환
     */
    public LocalDate getReservationDate() {
        return reservationTime.toLocalDate();
    }

    /**
     * 오늘부터 예약일까지의 일수
     */
    public int getDaysFromToday() {
        return (int) LocalDate.now().until(getReservationDate()).getDays();
    }

    /**
     * 리소스가 가득 찼는지 확인
     */
    public boolean isResourceFull() {
        return resourceCurrentOccupancy >= resourceCapacity;
    }

    /**
     * 리소스 남은 수용 인원
     */
    public int getRemainingCapacity() {
        return Math.max(0, resourceCapacity - resourceCurrentOccupancy);
    }

    /**
     * 현재 시간 기준으로 예약 시간이 미래인지 확인
     */
    public boolean isReservationInFuture() {
        return reservationTime.isAfter(LocalDateTime.now());
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }

    private int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "은 0 이상이어야 합니다. 입력값: " + value);
        }
        return value;
    }

    private int validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "은 0보다 커야 합니다. 입력값: " + value);
        }
        return value;
    }
}