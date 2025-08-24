# Reservation.java - 상세 주석 및 설명

## 클래스 개요
`Reservation`은 특정 회원이 특정 시간에 특정 리소스를 이용하기 위한 예약을 나타내는 **엔티티(Entity)**입니다.
`reservationId`를 기준으로 동일성을 판단하며, 예약의 전체 생명주기와 상태 변경 이력을 관리합니다.

## 왜 이런 클래스가 필요한가?
1. **예약 생명주기 관리**: 요청→확정→사용→완료의 전체 흐름 추적
2. **충돌 감지**: 같은 리소스의 시간 겹침 확인
3. **상태 변경 추적**: 언제, 왜 상태가 변경되었는지 이력 관리
4. **비즈니스 규칙 적용**: 노쇼 판정, 취소 가능 여부 등

## 상세 주석이 추가된 코드

```java
package com.hexapass.domain.model;

import com.hexapass.domain.common.TimeSlot; // 값 객체 - 시간대
import com.hexapass.domain.type.ReservationStatus; // 열거형 - 예약 상태

import java.time.LocalDateTime; // 날짜+시간
import java.util.ArrayList; // 가변 리스트 (상태 이력용)
import java.util.List; // 리스트 인터페이스
import java.util.Objects; // equals, hashCode 유틸리티

/**
 * 예약을 나타내는 엔티티
 * 특정 회원이 특정 시간에 특정 리소스를 이용하기 위한 예약
 * reservationId를 기준으로 동일성 판단
 * 
 * 엔티티 특징:
 * 1. 식별자: reservationId로 구분
 * 2. 생명주기: 요청→확정→사용→완료/취소
 * 3. 상태 변경: 비즈니스 규칙에 따른 상태 전환
 * 4. 이력 관리: 모든 상태 변경을 추적
 */
public class Reservation {

    // === 불변 필드들 (생성 후 변경 불가) ===
    private final String reservationId;    // 예약 고유 식별자
    private final String memberId;         // 예약한 회원 ID (FK)
    private final String resourceId;       // 예약된 리소스 ID (FK)
    private final TimeSlot timeSlot;       // 예약 시간대 (값 객체)
    private final LocalDateTime createdAt; // 예약 생성 시각

    // === 상태 관련 가변 필드들 ===
    private ReservationStatus status;      // 현재 예약 상태
    private LocalDateTime confirmedAt;     // 확정 시각 (null 가능)
    private LocalDateTime startedAt;       // 사용 시작 시각 (null 가능)
    private LocalDateTime completedAt;     // 완료 시각 (null 가능)
    private LocalDateTime cancelledAt;     // 취소 시각 (null 가능)
    private String cancellationReason;    // 취소 사유 (취소 시에만 사용)
    
    // === 추가 정보 ===
    private final List<StatusChangeHistory> statusHistory; // 상태 변경 이력
    private String notes;                  // 예약 관련 메모 (가변)

    /**
     * 상태 변경 이력을 기록하는 내부 클래스 (Value Object)
     * 
     * static nested class 사용 이유:
     * 1. Reservation과 밀접한 관련이 있지만 독립적인 개념
     * 2. 외부 클래스 인스턴스에 접근할 필요 없음 (static)
     * 3. 캡슐화: Reservation 내부에서만 사용되는 개념
     */
    public static class StatusChangeHistory {
        private final ReservationStatus fromStatus; // 이전 상태 (null이면 최초 생성)
        private final ReservationStatus toStatus;   // 변경된 상태
        private final LocalDateTime changedAt;      // 변경 시각
        private final String reason;                // 변경 사유

        /**
         * 상태 변경 이력 생성자
         * 
         * 불변 객체로 설계하여 이력의 무결성 보장
         */
        public StatusChangeHistory(ReservationStatus fromStatus, ReservationStatus toStatus,
                                   LocalDateTime changedAt, String reason) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.changedAt = changedAt;
            this.reason = reason;
        }

        // Getter methods - 모든 필드가 final이므로 단순 반환
        public ReservationStatus getFromStatus() { return fromStatus; }
        public ReservationStatus getToStatus() { return toStatus; }
        public LocalDateTime getChangedAt() { return changedAt; }
        public String getReason() { return reason; }

        /**
         * 사용자 친화적인 문자열 표현
         * 
         * 관리자가 이력을 볼 때 이해하기 쉬운 형태로 표시
         */
        @Override
        public String toString() {
            return String.format("%s -> %s (%s) %s",
                    fromStatus, toStatus, changedAt, reason != null ? "[" + reason + "]" : "");
        }
    }

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     * 
     * 예약 생성 시 기본적인 유효성 검사와 초기 상태 설정
     */
    private Reservation(String reservationId, String memberId, String resourceId, TimeSlot timeSlot) {
        // 기본 유효성 검증
        this.reservationId = validateNotBlank(reservationId, "예약 ID");
        this.memberId = validateNotBlank(memberId, "회원 ID");
        this.resourceId = validateNotBlank(resourceId, "리소스 ID");
        this.timeSlot = validateNotNull(timeSlot, "예약 시간대");
        
        this.createdAt = LocalDateTime.now();
        this.status = ReservationStatus.REQUESTED; // 기본값: 요청 상태
        
        // 상태 이력을 저장할 가변 리스트 초기화
        // ArrayList: 순서 보장, 중복 허용, 인덱스 기반 접근
        this.statusHistory = new ArrayList<>();

        // 비즈니스 규칙 검증
        validateReservationTime();

        // 초기 상태 이력 기록 (fromStatus = null은 최초 생성을 의미)
        addStatusHistory(null, ReservationStatus.REQUESTED, "예약 생성");
    }

    /**
     * 예약 생성 팩토리 메서드
     * 
     * 기본적인 예약 생성
     */
    public static Reservation create(String reservationId, String memberId,
                                     String resourceId, TimeSlot timeSlot) {
        return new Reservation(reservationId, memberId, resourceId, timeSlot);
    }

    /**
     * 메모가 포함된 예약 생성
     * 
     * 특별한 요청사항이나 참고사항이 있는 예약 생성
     */
    public static Reservation createWithNotes(String reservationId, String memberId,
                                              String resourceId, TimeSlot timeSlot, String notes) {
        Reservation reservation = new Reservation(reservationId, memberId, resourceId, timeSlot);
        reservation.notes = notes; // 생성 후 메모 설정
        return reservation;
    }

    // =========================
    // 예약 상태 변경 메서드들
    // =========================

    /**
     * 예약 확정
     * 
     * 요청 상태에서 확정 상태로 전환
     * 관리자가 예약을 승인하거나 자동 확정 시 호출
     */
    public void confirm() {
        validateStatusTransition(ReservationStatus.CONFIRMED); // 전환 가능성 확인

        ReservationStatus oldStatus = this.status; // 이력 기록용 이전 상태 저장
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now(); // 확정 시각 기록

        // 상태 변경 이력 추가
        addStatusHistory(oldStatus, ReservationStatus.CONFIRMED, "예약 확정");
    }

    /**
     * 사용 시작
     * 
     * 확정된 예약을 실제로 사용하기 시작할 때 호출
     * QR 코드 스캔, 출입 인증 등에서 트리거
     */
    public void startUsing() {
        validateStatusTransition(ReservationStatus.IN_USE);

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.IN_USE;
        this.startedAt = LocalDateTime.now(); // 사용 시작 시각 기록

        addStatusHistory(oldStatus, ReservationStatus.IN_USE, "사용 시작");
    }

    /**
     * 사용 완료
     * 
     * 예약된 시간이 끝나거나 사용자가 퇴실할 때 호출
     * 정상적인 예약 완료를 의미
     */
    public void complete() {
        validateStatusTransition(ReservationStatus.COMPLETED);

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now(); // 완료 시각 기록

        addStatusHistory(oldStatus, ReservationStatus.COMPLETED, "사용 완료");
    }

    /**
     * 예약 취소
     * 
     * 사용자가 직접 취소하거나 관리자가 취소할 때 호출
     * 
     * @param reason 취소 사유 (필수)
     */
    public void cancel(String reason) {
        // 취소 가능 상태인지 확인 (enum의 비즈니스 로직 활용)
        if (!status.isCancellable()) {
            throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다: " + status);
        }

        validateNotBlank(reason, "취소 사유"); // 취소 사유는 필수

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason; // 취소 사유 기록

        addStatusHistory(oldStatus, ReservationStatus.CANCELLED, "예약 취소: " + reason);
    }

    /**
     * 자동 취소 (시스템에 의한 취소)
     * 
     * 시간 초과, 시스템 점검 등으로 인한 자동 취소
     * 
     * @param systemReason 시스템 취소 사유
     */
    public void autoCancel(String systemReason) {
        cancel("시스템 자동 취소: " + systemReason); // cancel 메서드 재사용
    }

    // =========================
    // 예약 정보 확인 메서드들
    // =========================

    /**
     * 활성 예약인지 확인 (취소되지 않고 아직 완료되지 않은 상태)
     * 
     * 리소스 충돌 검사, 예약 현황 조회 등에서 사용
     * enum의 비즈니스 로직 메서드 활용
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * 최종 상태인지 확인
     * 
     * 더 이상 상태 변경이 일어나지 않는 상태
     * 통계, 정산 등에서 활용
     */
    public boolean isFinal() {
        return status.isFinal();
    }

    /**
     * 취소 가능한 상태인지 확인
     * 
     * UI에서 취소 버튼 표시 여부 결정 등에 사용
     */
    public boolean isCancellable() {
        return status.isCancellable();
    }

    /**
     * 다른 예약과 시간 충돌하는지 확인
     * 
     * 같은 리소스에서 시간이 겹치는 경우를 충돌로 판정
     * 
     * @param other 비교할 다른 예약
     * @return 충돌하면 true
     */
    public boolean conflictsWith(Reservation other) {
        if (other == null) {
            return false;
        }

        // 같은 리소스이고 시간이 겹치는 경우 충돌
        return this.resourceId.equals(other.resourceId) &&
                this.timeSlot.overlaps(other.timeSlot);
    }

    /**
     * 지정된 시간대와 충돌하는지 확인
     * 
     * 새로운 예약 요청이 기존 예약과 충돌하는지 확인할 때 사용
     * 
     * @param otherTimeSlot 비교할 시간대
     * @return 충돌하면 true
     */
    public boolean conflictsWith(TimeSlot otherTimeSlot) {
        return this.timeSlot.overlaps(otherTimeSlot);
    }

    /**
     * 노쇼(No-show) 여부 확인 - 예약 시간이 지났는데 사용하지 않은 경우
     * 
     * 노쇼 패널티, 통계 등에 활용
     * 
     * @return 노쇼면 true
     */
    public boolean isNoShow() {
        if (status != ReservationStatus.CONFIRMED) {
            return false; // 확정된 예약만 노쇼 판정 가능
        }

        // TimeSlot.isPast(): 시간대가 현재 시각보다 이전인지 확인
        return timeSlot.isPast();
    }

    /**
     * 예약 시간까지 남은 시간 (분 단위)
     * 
     * 알림 발송, UI 표시 등에 활용
     * 
     * @return 남은 시간 (분), 이미 지났으면 0
     */
    public long getMinutesUntilReservation() {
        LocalDateTime now = LocalDateTime.now();
        if (timeSlot.getStartTime().isBefore(now)) {
            return 0; // 이미 지난 시간
        }

        // Duration.between(): 두 시간 사이의 간격 계산
        return java.time.Duration.between(now, timeSlot.getStartTime()).toMinutes();
    }

    /**
     * 예약 변경 가능 시간인지 확인 (예약 시작 시간 1시간 전까지)
     * 
     * 비즈니스 규칙: 예약 시작 1시간 전까지만 수정 가능
     * 
     * @return 수정 가능하면 true
     */
    public boolean isModifiable() {
        if (isFinal()) {
            return false; // 최종 상태는 수정 불가
        }

        return getMinutesUntilReservation() > 60; // 1시간(60분) 전까지만 수정 가능
    }

    // =========================
    // 예약 정보 조회 메서드들
    // =========================

    /**
     * 예약 소요 시간 (분 단위)
     * 
     * 시간 기반 요금 계산, 통계 등에 활용
     */
    public long getDurationMinutes() {
        return timeSlot.getDurationMinutes(); // TimeSlot 값 객체의 메서드 활용
    }

    /**
     * 예약 정보 요약
     * 
     * 관리자 화면, 로그, 알림 메시지 등에서 사용할 요약 정보
     * 
     * @return 예약 요약 문자열
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        // 기본 정보
        summary.append(String.format("예약 %s - 회원 %s, 리소스 %s",
                reservationId, memberId, resourceId));
        
        // 시간과 상태 정보
        summary.append(String.format(" | %s | %s", timeSlot, status.getDisplayName()));

        // 취소된 경우 취소 사유 표시
        if (status == ReservationStatus.CANCELLED && cancellationReason != null) {
            summary.append(" [").append(cancellationReason).append("]");
        }

        // 메모가 있으면 표시
        if (notes != null && !notes.trim().isEmpty()) {
            summary.append(" | 메모: ").append(notes);
        }

        return summary.toString();
    }

    /**
     * 상태 변경 이력 조회
     * 
     * List.copyOf(): 방어적 복사로 외부에서 수정 불가능한 불변 리스트 반환
     * 
     * @return 상태 변경 이력의 불변 복사본
     */
    public List<StatusChangeHistory> getStatusHistory() {
        return List.copyOf(statusHistory);
    }

    /**
     * 최근 상태 변경 정보
     * 
     * 마지막 상태 변경 시점과 사유를 확인할 때 사용
     * 
     * @return 최근 상태 변경 이력, 없으면 null
     */
    public StatusChangeHistory getLatestStatusChange() {
        if (statusHistory.isEmpty()) {
            return null;
        }
        // ArrayList는 인덱스 기반 접근이 O(1)
        return statusHistory.get(statusHistory.size() - 1); // 마지막 요소
    }

    // =========================
    // 예약 메타데이터 관리
    // =========================

    /**
     * 메모 설정
     * 
     * 기존 메모를 완전히 대체
     * 
     * @param notes 새로운 메모 (null이면 메모 제거)
     */
    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }

    /**
     * 메모 추가
     * 
     * 기존 메모에 새로운 내용을 추가 (구분자로 연결)
     * 
     * @param additionalNotes 추가할 메모
     */
    public void addNotes(String additionalNotes) {
        if (additionalNotes == null || additionalNotes.trim().isEmpty()) {
            return; // 빈 내용은 추가하지 않음
        }

        if (this.notes == null || this.notes.isEmpty()) {
            this.notes = additionalNotes.trim(); // 첫 번째 메모
        } else {
            this.notes = this.notes + " | " + additionalNotes.trim(); // 구분자로 연결
        }
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    /**
     * equals 메서드 오버라이드
     * 
     * 엔티티: 식별자(reservationId) 기준으로만 동일성 판단
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Reservation that = (Reservation) obj;
        return Objects.equals(reservationId, that.reservationId);
    }

    /**
     * hashCode 메서드 오버라이드
     */
    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    /**
     * toString 메서드 오버라이드
     * 
     * 디버깅용 간결한 표현
     */
    @Override
    public String toString() {
        return String.format("Reservation{id='%s', member='%s', resource='%s', timeSlot=%s, status=%s}",
                reservationId, memberId, resourceId, timeSlot, status);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getReservationId() {
        return reservationId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getResourceId() {
        return resourceId;
    }

    /**
     * 예약 시간대 반환
     * 
     * TimeSlot은 불변 객체이므로 방어적 복사 불필요
     */
    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    /**
     * 확정 시각 반환
     * 
     * 확정되지 않은 예약은 null 반환
     */
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    /**
     * 취소 사유 반환
     * 
     * 취소되지 않은 예약은 null 반환
     */
    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getNotes() {
        return notes;
    }

    // =========================
    // 헬퍼 메서드들 (private)
    // =========================

    /**
     * 상태 전환 가능성 검증
     * 
     * enum에 정의된 상태 전환 규칙 활용
     * 
     * @param newStatus 전환하려는 새로운 상태
     */
    private void validateStatusTransition(ReservationStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("예약 상태를 %s에서 %s로 변경할 수 없습니다",
                            status.getDisplayName(), newStatus.getDisplayName()));
        }
    }

    /**
     * 상태 변경 이력 추가
     * 
     * 모든 상태 변경 시 호출하여 이력 추적
     * 
     * @param fromStatus 이전 상태 (최초 생성 시 null)
     * @param toStatus 새로운 상태
     * @param reason 변경 사유
     */
    private void addStatusHistory(ReservationStatus fromStatus, ReservationStatus toStatus, String reason) {
        StatusChangeHistory history = new StatusChangeHistory(fromStatus, toStatus, LocalDateTime.now(), reason);
        statusHistory.add(history); // ArrayList에 추가
    }

    /**
     * 예약 시간 유효성 검증
     * 
     * 비즈니스 규칙:
     * 1. 과거 시간으로는 예약 불가
     * 2. 너무 먼 미래(1년 후)로는 예약 불가
     */
    private void validateReservationTime() {
        LocalDateTime now = LocalDateTime.now();
        
        // 과거 시간 예약 방지
        if (timeSlot.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException(
                    String.format("과거 시간으로는 예약할 수 없습니다. 예약 시간: %s, 현재 시간: %s",
                            timeSlot.getStartTime(), now));
        }

        // 너무 먼 미래 예약 방지 (최대 1년 후)
        if (timeSlot.getStartTime().isAfter(now.plusDays(365))) {
            throw new IllegalArgumentException("예약은 최대 1년 후까지만 가능합니다");
        }
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }
}
```

## 주요 설계 원칙 및 패턴

### 1. 상태 관리와 이력 추적

#### 상태 전환 + 메타데이터 기록
```java
reservation.confirm();
// 1. 상태 변경: REQUESTED → CONFIRMED
// 2. 확정 시각 기록: confirmedAt = now()
// 3. 상태 이력 추가: StatusChangeHistory 객체 생성
```

#### 완전한 감사 추적(Audit Trail)
```java
List<StatusChangeHistory> history = reservation.getStatusHistory();
// 예약 생성부터 현재까지의 모든 상태 변경을 추적
// 언제, 무엇이, 왜 변경되었는지 완전 기록
```

### 2. 내부 클래스 활용

#### StatusChangeHistory의 설계 선택
- **static nested class**: 외부 인스턴스 참조 없이 독립적
- **불변 객체**: 이력의 무결성 보장
- **Value Object**: 값으로 구분되는 개념

```java
// 사용 예시
StatusChangeHistory latest = reservation.getLatestStatusChange();
System.out.println(latest); // "REQUESTED -> CONFIRMED (2024-01-15T10:30) [예약 확정]"
```

### 3. 비즈니스 로직의 응집도

#### 충돌 감지 로직
```java
// 같은 리소스, 겹치는 시간 = 충돌
boolean conflict = reservation1.conflictsWith(reservation2);

// 새로운 시간대와의 충돌 확인
boolean timeConflict = reservation.conflictsWith(newTimeSlot);
```

#### 노쇼 판정 로직
```java
// 확정된 예약이면서 시간이 지난 경우
boolean noShow = reservation.isNoShow();
```

### 4. 시간 기반 비즈니스 규칙

#### 수정 가능 시간 제한
```java
// 1시간 전까지만 수정 가능
boolean canModify = reservation.isModifiable();
```

#### 남은 시간 계산
```java
long minutes = reservation.getMinutesUntilReservation();
if (minutes <= 30) {
    sendReminderNotification(); // 30분 전 알림
}
```

### 5. 메타데이터 관리

#### 유연한 메모 시스템
```java
reservation.setNotes("특별 요청: 창가 자리 선호");
reservation.addNotes("관리자 확인: 창가 자리 배정 완료");
// 결과: "특별 요청: 창가 자리 선호 | 관리자 확인: 창가 자리 배정 완료"
```

### 6. 방어적 프로그래밍

#### 상태 전환 검증
```java
public void cancel(String reason) {
    if (!status.isCancellable()) {
        throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다: " + status);
    }
    // 검증 통과 후에만 실행
}
```

#### 시간 유효성 검증
```java
private void validateReservationTime() {
    // 과거 시간 예약 방지
    // 너무 먼 미래 예약 방지
}
```

### 7. 실제 사용 예시

#### 예약 생성부터 완료까지
```java
// 1. 예약 생성
Reservation reservation = Reservation.create("R001", "M001", "GYM001", timeSlot);

// 2. 관리자 확정
reservation.confirm();

// 3. 사용 시작 (QR 코드 스캔)
reservation.startUsing();

// 4. 사용 완료
reservation.complete();

// 5. 전체 이력 확인
List<StatusChangeHistory> history = reservation.getStatusHistory();
// 4개의 상태 변경 이력이 기록됨
```

#### 예약 충돌 검사
```java
List<Reservation> existingReservations = getActiveReservations("GYM001");
TimeSlot newTimeSlot = TimeSlot.of(requestedStart, requestedEnd);

boolean hasConflict = existingReservations.stream()
    .anyMatch(existing -> existing.conflictsWith(newTimeSlot));
```

### 8. 확장성 고려사항

#### 상태별 특화 로직
```java
// 향후 상태별 전략 패턴 적용 가능
public interface ReservationStateHandler {
    void onEnterState(Reservation reservation);
    void onExitState(Reservation reservation);
}
```

#### 이벤트 발행
```java
// 상태 변경 시 도메인 이벤트 발행 가능
private void addStatusHistory(ReservationStatus fromStatus, ReservationStatus toStatus, String reason) {
    StatusChangeHistory history = new StatusChangeHistory(fromStatus, toStatus, LocalDateTime.now(), reason);
    statusHistory.add(history);
    
    // 도메인 이벤트 발행
    DomainEvents.publish(new ReservationStatusChangedEvent(this, fromStatus, toStatus));
}
```

이러한 설계로 Reservation은 단순한 데이터 저장소가 아닌, 예약의 전체 생명주기와 비즈니스 규칙을 완전히 캡슐화한 풍부한 도메인 객체가 되었습니다.