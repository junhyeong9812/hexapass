package com.hexapass.domain.port.outbound;

import com.hexapass.domain.common.Money;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 결제 게이트웨이 포트 (Outbound Port)
 * 외부 결제 시스템과의 인터페이스
 */
public interface PaymentGateway {

    /**
     * 결제 요청
     */
    CompletableFuture<PaymentResult> requestPayment(PaymentRequest request);

    /**
     * 결제 취소/환불
     */
    CompletableFuture<PaymentResult> cancelPayment(
            String paymentId,
            Money refundAmount,
            String reason
    );

    /**
     * 부분 환불
     */
    CompletableFuture<PaymentResult> partialRefund(
            String paymentId,
            Money refundAmount,
            String reason
    );

    /**
     * 결제 상태 조회
     */
    CompletableFuture<PaymentStatus> getPaymentStatus(String paymentId);

    /**
     * 정기 결제 등록
     */
    CompletableFuture<SubscriptionResult> createSubscription(SubscriptionRequest request);

    /**
     * 정기 결제 취소
     */
    CompletableFuture<Boolean> cancelSubscription(String subscriptionId);

    /**
     * 정기 결제 상태 조회
     */
    CompletableFuture<SubscriptionStatus> getSubscriptionStatus(String subscriptionId);

    /**
     * 결제 수단 등록
     */
    CompletableFuture<PaymentMethodResult> registerPaymentMethod(
            String memberId,
            PaymentMethodInfo paymentMethodInfo
    );

    /**
     * 결제 수단 삭제
     */
    CompletableFuture<Boolean> deletePaymentMethod(String paymentMethodId);

    /**
     * 회원의 등록된 결제 수단 목록 조회
     */
    CompletableFuture<java.util.List<PaymentMethod>> getPaymentMethods(String memberId);

    /**
     * 결제 이력 조회
     */
    java.util.List<PaymentHistory> getPaymentHistory(
            String memberId,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * 결제 수단 유효성 검증
     */
    CompletableFuture<Boolean> validatePaymentMethod(PaymentMethodInfo paymentMethodInfo);

    /**
     * 결제 요청 객체
     */
    class PaymentRequest {
        private final String orderId;
        private final String memberId;
        private final Money amount;
        private final String paymentMethodId;
        private final String description;
        private final Map<String, String> metadata;
        private final String returnUrl;
        private final String cancelUrl;

        public PaymentRequest(String orderId, String memberId, Money amount,
                              String paymentMethodId, String description,
                              Map<String, String> metadata, String returnUrl, String cancelUrl) {
            this.orderId = orderId;
            this.memberId = memberId;
            this.amount = amount;
            this.paymentMethodId = paymentMethodId;
            this.description = description;
            this.metadata = metadata != null ? metadata : Map.of();
            this.returnUrl = returnUrl;
            this.cancelUrl = cancelUrl;
        }

        // Getters
        public String getOrderId() { return orderId; }
        public String getMemberId() { return memberId; }
        public Money getAmount() { return amount; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public String getDescription() { return description; }
        public Map<String, String> getMetadata() { return metadata; }
        public String getReturnUrl() { return returnUrl; }
        public String getCancelUrl() { return cancelUrl; }
    }

    /**
     * 정기 결제 요청 객체
     */
    class SubscriptionRequest {
        private final String memberId;
        private final String planId;
        private final Money amount;
        private final String paymentMethodId;
        private final SubscriptionPeriod period;
        private final LocalDateTime startDate;
        private final String description;

        public SubscriptionRequest(String memberId, String planId, Money amount,
                                   String paymentMethodId, SubscriptionPeriod period,
                                   LocalDateTime startDate, String description) {
            this.memberId = memberId;
            this.planId = planId;
            this.amount = amount;
            this.paymentMethodId = paymentMethodId;
            this.period = period;
            this.startDate = startDate;
            this.description = description;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getPlanId() { return planId; }
        public Money getAmount() { return amount; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public SubscriptionPeriod getPeriod() { return period; }
        public LocalDateTime getStartDate() { return startDate; }
        public String getDescription() { return description; }
    }

    /**
     * 결제 결과
     */
    class PaymentResult {
        private final boolean success;
        private final String paymentId;
        private final String transactionId;
        private final PaymentStatus status;
        private final String errorCode;
        private final String errorMessage;
        private final LocalDateTime processedAt;
        private final Map<String, String> additionalInfo;

        public PaymentResult(boolean success, String paymentId, String transactionId,
                             PaymentStatus status, String errorCode, String errorMessage,
                             LocalDateTime processedAt, Map<String, String> additionalInfo) {
            this.success = success;
            this.paymentId = paymentId;
            this.transactionId = transactionId;
            this.status = status;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.processedAt = processedAt;
            this.additionalInfo = additionalInfo != null ? additionalInfo : Map.of();
        }

        public static PaymentResult success(String paymentId, String transactionId) {
            return new PaymentResult(true, paymentId, transactionId, PaymentStatus.COMPLETED,
                    null, null, LocalDateTime.now(), null);
        }

        public static PaymentResult failure(String errorCode, String errorMessage) {
            return new PaymentResult(false, null, null, PaymentStatus.FAILED,
                    errorCode, errorMessage, LocalDateTime.now(), null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getPaymentId() { return paymentId; }
        public String getTransactionId() { return transactionId; }
        public PaymentStatus getStatus() { return status; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public Map<String, String> getAdditionalInfo() { return additionalInfo; }
    }

    /**
     * 정기 결제 결과
     */
    class SubscriptionResult {
        private final boolean success;
        private final String subscriptionId;
        private final String errorMessage;
        private final LocalDateTime nextBillingDate;

        public SubscriptionResult(boolean success, String subscriptionId,
                                  String errorMessage, LocalDateTime nextBillingDate) {
            this.success = success;
            this.subscriptionId = subscriptionId;
            this.errorMessage = errorMessage;
            this.nextBillingDate = nextBillingDate;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getSubscriptionId() { return subscriptionId; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getNextBillingDate() { return nextBillingDate; }
    }

    /**
     * 결제 수단 등록 결과
     */
    class PaymentMethodResult {
        private final boolean success;
        private final String paymentMethodId;
        private final String errorMessage;

        public PaymentMethodResult(boolean success, String paymentMethodId, String errorMessage) {
            this.success = success;
            this.paymentMethodId = paymentMethodId;
            this.errorMessage = errorMessage;
        }

        public static PaymentMethodResult success(String paymentMethodId) {
            return new PaymentMethodResult(true, paymentMethodId, null);
        }

        public static PaymentMethodResult failure(String errorMessage) {
            return new PaymentMethodResult(false, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 결제 상태
     */
    enum PaymentStatus {
        PENDING("대기중"),
        PROCESSING("처리중"),
        COMPLETED("완료"),
        FAILED("실패"),
        CANCELLED("취소됨"),
        REFUNDED("환불됨"),
        PARTIALLY_REFUNDED("부분환불됨");

        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isCompleted() {
            return this == COMPLETED;
        }

        public boolean isFinal() {
            return this == COMPLETED || this == FAILED ||
                    this == CANCELLED || this == REFUNDED;
        }
    }

    /**
     * 정기 결제 상태
     */
    enum SubscriptionStatus {
        ACTIVE("활성"),
        INACTIVE("비활성"),
        CANCELLED("취소됨"),
        SUSPENDED("정지됨"),
        EXPIRED("만료됨");

        private final String displayName;

        SubscriptionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isActive() {
            return this == ACTIVE;
        }
    }

    /**
     * 정기 결제 주기
     */
    enum SubscriptionPeriod {
        WEEKLY("주간", 7),
        MONTHLY("월간", 30),
        QUARTERLY("분기", 90),
        YEARLY("연간", 365);

        private final String displayName;
        private final int days;

        SubscriptionPeriod(String displayName, int days) {
            this.displayName = displayName;
            this.days = days;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getDays() {
            return days;
        }
    }

    /**
     * 결제 수단 타입
     */
    enum PaymentMethodType {
        CREDIT_CARD("신용카드"),
        DEBIT_CARD("체크카드"),
        BANK_TRANSFER("계좌이체"),
        VIRTUAL_ACCOUNT("가상계좌"),
        MOBILE_PAYMENT("간편결제"),
        CRYPTOCURRENCY("암호화폐");

        private final String displayName;

        PaymentMethodType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 결제 수단 정보
     */
    class PaymentMethodInfo {
        private final PaymentMethodType type;
        private final String cardNumber;
        private final String expiryDate;
        private final String holderName;
        private final String bankCode;
        private final String accountNumber;
        private final Map<String, String> additionalInfo;

        public PaymentMethodInfo(PaymentMethodType type, String cardNumber, String expiryDate,
                                 String holderName, String bankCode, String accountNumber,
                                 Map<String, String> additionalInfo) {
            this.type = type;
            this.cardNumber = cardNumber;
            this.expiryDate = expiryDate;
            this.holderName = holderName;
            this.bankCode = bankCode;
            this.accountNumber = accountNumber;
            this.additionalInfo = additionalInfo != null ? additionalInfo : Map.of();
        }

        // Getters
        public PaymentMethodType getType() { return type; }
        public String getCardNumber() { return cardNumber; }
        public String getExpiryDate() { return expiryDate; }
        public String getHolderName() { return holderName; }
        public String getBankCode() { return bankCode; }
        public String getAccountNumber() { return accountNumber; }
        public Map<String, String> getAdditionalInfo() { return additionalInfo; }

        public String getMaskedCardNumber() {
            if (cardNumber != null && cardNumber.length() >= 4) {
                return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
            }
            return "****";
        }
    }

    /**
     * 등록된 결제 수단
     */
    class PaymentMethod {
        private final String paymentMethodId;
        private final String memberId;
        private final PaymentMethodType type;
        private final String maskedInfo;
        private final String displayName;
        private final boolean isDefault;
        private final LocalDateTime registeredAt;
        private final LocalDateTime lastUsedAt;

        public PaymentMethod(String paymentMethodId, String memberId, PaymentMethodType type,
                             String maskedInfo, String displayName, boolean isDefault,
                             LocalDateTime registeredAt, LocalDateTime lastUsedAt) {
            this.paymentMethodId = paymentMethodId;
            this.memberId = memberId;
            this.type = type;
            this.maskedInfo = maskedInfo;
            this.displayName = displayName;
            this.isDefault = isDefault;
            this.registeredAt = registeredAt;
            this.lastUsedAt = lastUsedAt;
        }

        // Getters
        public String getPaymentMethodId() { return paymentMethodId; }
        public String getMemberId() { return memberId; }
        public PaymentMethodType getType() { return type; }
        public String getMaskedInfo() { return maskedInfo; }
        public String getDisplayName() { return displayName; }
        public boolean isDefault() { return isDefault; }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    }

    /**
     * 결제 이력
     */
    class PaymentHistory {
        private final String paymentId;
        private final String memberId;
        private final String orderId;
        private final Money amount;
        private final PaymentStatus status;
        private final PaymentMethodType paymentMethodType;
        private final String description;
        private final LocalDateTime processedAt;
        private final String failureReason;

        public PaymentHistory(String paymentId, String memberId, String orderId, Money amount,
                              PaymentStatus status, PaymentMethodType paymentMethodType,
                              String description, LocalDateTime processedAt, String failureReason) {
            this.paymentId = paymentId;
            this.memberId = memberId;
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
            this.paymentMethodType = paymentMethodType;
            this.description = description;
            this.processedAt = processedAt;
            this.failureReason = failureReason;
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getMemberId() { return memberId; }
        public String getOrderId() { return orderId; }
        public Money getAmount() { return amount; }
        public PaymentStatus getStatus() { return status; }
        public PaymentMethodType getPaymentMethodType() { return paymentMethodType; }
        public String getDescription() { return description; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public String getFailureReason() { return failureReason; }
    }
}