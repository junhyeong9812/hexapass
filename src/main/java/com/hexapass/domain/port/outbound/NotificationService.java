package com.hexapass.domain.port.outbound;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.Reservation;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 알림 서비스 포트 (Outbound Port)
 * 회원에게 다양한 채널로 알림을 발송하는 외부 서비스와의 인터페이스
 */
public interface NotificationService {

    /**
     * 예약 확정 알림 발송
     */
    CompletableFuture<NotificationResult> sendReservationConfirmed(
            Member member,
            Reservation reservation
    );

    /**
     * 예약 취소 알림 발송
     */
    CompletableFuture<NotificationResult> sendReservationCancelled(
            Member member,
            Reservation reservation,
            String cancellationReason
    );

    /**
     * 예약 시작 알림 발송 (예약 시간 N분 전)
     */
    CompletableFuture<NotificationResult> sendReservationReminder(
            Member member,
            Reservation reservation,
            int minutesBefore
    );

    /**
     * 멤버십 만료 경고 알림 발송
     */
    CompletableFuture<NotificationResult> sendMembershipExpiryWarning(
            Member member,
            int daysRemaining
    );

    /**
     * 멤버십 갱신 완료 알림 발송
     */
    CompletableFuture<NotificationResult> sendMembershipRenewed(
            Member member,
            LocalDateTime newExpiryDate
    );

    /**
     * 결제 완료 알림 발송
     */
    CompletableFuture<NotificationResult> sendPaymentCompleted(
            Member member,
            String paymentId,
            String amount
    );

    /**
     * 결제 실패 알림 발송
     */
    CompletableFuture<NotificationResult> sendPaymentFailed(
            Member member,
            String paymentId,
            String failureReason
    );

    /**
     * 커스텀 알림 발송
     */
    CompletableFuture<NotificationResult> sendCustomNotification(
            Member member,
            String title,
            String message,
            NotificationType type,
            Map<String, String> additionalData
    );

    /**
     * 대량 알림 발송 (공지사항 등)
     */
    CompletableFuture<BulkNotificationResult> sendBulkNotification(
            java.util.List<Member> members,
            String title,
            String message,
            NotificationType type
    );

    /**
     * 알림 발송 예약 (지정된 시간에 발송)
     */
    CompletableFuture<NotificationResult> scheduleNotification(
            Member member,
            String title,
            String message,
            LocalDateTime scheduledTime,
            NotificationType type
    );

    /**
     * 예약된 알림 취소
     */
    CompletableFuture<Boolean> cancelScheduledNotification(String notificationId);

    /**
     * 회원의 알림 설정 조회
     */
    NotificationPreferences getNotificationPreferences(String memberId);

    /**
     * 회원의 알림 설정 업데이트
     */
    CompletableFuture<Boolean> updateNotificationPreferences(
            String memberId,
            NotificationPreferences preferences
    );

    /**
     * 알림 발송 이력 조회
     */
    java.util.List<NotificationHistory> getNotificationHistory(
            String memberId,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * 알림 타입
     */
    enum NotificationType {
        INFO("정보"),
        WARNING("경고"),
        SUCCESS("성공"),
        ERROR("오류"),
        REMINDER("알림"),
        PROMOTION("프로모션");

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 알림 채널
     */
    enum NotificationChannel {
        EMAIL("이메일"),
        SMS("문자메시지"),
        PUSH("푸시알림"),
        IN_APP("앱내알림");

        private final String displayName;

        NotificationChannel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 알림 발송 결과
     */
    class NotificationResult {
        private final boolean success;
        private final String notificationId;
        private final String errorMessage;
        private final LocalDateTime sentAt;
        private final NotificationChannel channel;

        public NotificationResult(boolean success, String notificationId,
                                  String errorMessage, LocalDateTime sentAt,
                                  NotificationChannel channel) {
            this.success = success;
            this.notificationId = notificationId;
            this.errorMessage = errorMessage;
            this.sentAt = sentAt;
            this.channel = channel;
        }

        public static NotificationResult success(String notificationId,
                                                 NotificationChannel channel) {
            return new NotificationResult(true, notificationId, null,
                    LocalDateTime.now(), channel);
        }

        public static NotificationResult failure(String errorMessage,
                                                 NotificationChannel channel) {
            return new NotificationResult(false, null, errorMessage,
                    LocalDateTime.now(), channel);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getNotificationId() { return notificationId; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getSentAt() { return sentAt; }
        public NotificationChannel getChannel() { return channel; }
    }

    /**
     * 대량 알림 발송 결과
     */
    class BulkNotificationResult {
        private final int totalCount;
        private final int successCount;
        private final int failureCount;
        private final java.util.List<String> successIds;
        private final java.util.List<String> failureReasons;
        private final LocalDateTime completedAt;

        public BulkNotificationResult(int totalCount, int successCount, int failureCount,
                                      java.util.List<String> successIds,
                                      java.util.List<String> failureReasons) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.successIds = successIds != null ? successIds : java.util.List.of();
            this.failureReasons = failureReasons != null ? failureReasons : java.util.List.of();
            this.completedAt = LocalDateTime.now();
        }

        // Getters
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public java.util.List<String> getSuccessIds() { return successIds; }
        public java.util.List<String> getFailureReasons() { return failureReasons; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public double getSuccessRate() {
            return totalCount > 0 ? (double) successCount / totalCount : 0.0;
        }
    }

    /**
     * 알림 설정
     */
    class NotificationPreferences {
        private final boolean emailEnabled;
        private final boolean smsEnabled;
        private final boolean pushEnabled;
        private final boolean inAppEnabled;
        private final java.util.Set<NotificationType> enabledTypes;
        private final String timezone;

        public NotificationPreferences(boolean emailEnabled, boolean smsEnabled,
                                       boolean pushEnabled, boolean inAppEnabled,
                                       java.util.Set<NotificationType> enabledTypes,
                                       String timezone) {
            this.emailEnabled = emailEnabled;
            this.smsEnabled = smsEnabled;
            this.pushEnabled = pushEnabled;
            this.inAppEnabled = inAppEnabled;
            this.enabledTypes = enabledTypes != null ? enabledTypes : java.util.Set.of();
            this.timezone = timezone != null ? timezone : "Asia/Seoul";
        }

        // Getters
        public boolean isEmailEnabled() { return emailEnabled; }
        public boolean isSmsEnabled() { return smsEnabled; }
        public boolean isPushEnabled() { return pushEnabled; }
        public boolean isInAppEnabled() { return inAppEnabled; }
        public java.util.Set<NotificationType> getEnabledTypes() { return enabledTypes; }
        public String getTimezone() { return timezone; }

        public boolean isChannelEnabled(NotificationChannel channel) {
            return switch (channel) {
                case EMAIL -> emailEnabled;
                case SMS -> smsEnabled;
                case PUSH -> pushEnabled;
                case IN_APP -> inAppEnabled;
            };
        }

        public boolean isTypeEnabled(NotificationType type) {
            return enabledTypes.contains(type);
        }
    }

    /**
     * 알림 이력
     */
    class NotificationHistory {
        private final String notificationId;
        private final String memberId;
        private final String title;
        private final String message;
        private final NotificationType type;
        private final NotificationChannel channel;
        private final boolean delivered;
        private final LocalDateTime sentAt;
        private final LocalDateTime deliveredAt;
        private final String errorMessage;

        public NotificationHistory(String notificationId, String memberId, String title,
                                   String message, NotificationType type, NotificationChannel channel,
                                   boolean delivered, LocalDateTime sentAt, LocalDateTime deliveredAt,
                                   String errorMessage) {
            this.notificationId = notificationId;
            this.memberId = memberId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.channel = channel;
            this.delivered = delivered;
            this.sentAt = sentAt;
            this.deliveredAt = deliveredAt;
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getNotificationId() { return notificationId; }
        public String getMemberId() { return memberId; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public NotificationType getType() { return type; }
        public NotificationChannel getChannel() { return channel; }
        public boolean isDelivered() { return delivered; }
        public LocalDateTime getSentAt() { return sentAt; }
        public LocalDateTime getDeliveredAt() { return deliveredAt; }
        public String getErrorMessage() { return errorMessage; }
    }
}