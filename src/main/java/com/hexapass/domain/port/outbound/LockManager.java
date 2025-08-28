package com.hexapass.domain.port.outbound;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 분산 락 관리자 포트 (Outbound Port)
 * 동시성 제어를 위한 분산 락 시스템과의 인터페이스
 */
public interface LockManager {

    /**
     * 락 획득 시도
     * @param lockKey 락 키
     * @param ttl 락 유효시간
     * @return 락 획득 성공 여부
     */
    CompletableFuture<LockResult> acquireLock(String lockKey, Duration ttl);

    /**
     * 락 획득 시도 (대기 시간 포함)
     * @param lockKey 락 키
     * @param ttl 락 유효시간
     * @param waitTime 대기 시간
     * @return 락 획득 성공 여부
     */
    CompletableFuture<LockResult> acquireLock(String lockKey, Duration ttl, Duration waitTime);

    /**
     * 락 해제
     * @param lockKey 락 키
     * @param lockToken 락 토큰 (락 획득 시 받은 토큰)
     * @return 해제 성공 여부
     */
    CompletableFuture<Boolean> releaseLock(String lockKey, String lockToken);

    /**
     * 락 갱신 (TTL 연장)
     * @param lockKey 락 키
     * @param lockToken 락 토큰
     * @param additionalTtl 추가할 유효시간
     * @return 갱신 성공 여부
     */
    CompletableFuture<Boolean> renewLock(String lockKey, String lockToken, Duration additionalTtl);

    /**
     * 락 상태 확인
     * @param lockKey 락 키
     * @return 락 상태
     */
    CompletableFuture<LockStatus> getLockStatus(String lockKey);

    /**
     * 락 강제 해제 (관리자용)
     * @param lockKey 락 키
     * @return 해제 성공 여부
     */
    CompletableFuture<Boolean> forceReleaseLock(String lockKey);

    /**
     * 예약 시간대 충돌 방지용 락
     * @param resourceId 리소스 ID
     * @param startTime 예약 시작 시간
     * @param endTime 예약 종료 시간
     * @return 락 결과
     */
    default CompletableFuture<LockResult> acquireReservationLock(
            String resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        String lockKey = generateReservationLockKey(resourceId, startTime, endTime);
        return acquireLock(lockKey, Duration.ofMinutes(5)); // 5분 TTL
    }

    /**
     * 예약 락 해제
     */
    default CompletableFuture<Boolean> releaseReservationLock(
            String resourceId, LocalDateTime startTime, LocalDateTime endTime, String lockToken) {
        String lockKey = generateReservationLockKey(resourceId, startTime, endTime);
        return releaseLock(lockKey, lockToken);
    }

    /**
     * 회원별 동시 예약 방지용 락
     * @param memberId 회원 ID
     * @return 락 결과
     */
    default CompletableFuture<LockResult> acquireMemberReservationLock(String memberId) {
        String lockKey = "member_reservation:" + memberId;
        return acquireLock(lockKey, Duration.ofMinutes(3)); // 3분 TTL
    }

    /**
     * 회원 예약 락 해제
     */
    default CompletableFuture<Boolean> releaseMemberReservationLock(String memberId, String lockToken) {
        String lockKey = "member_reservation:" + memberId;
        return releaseLock(lockKey, lockToken);
    }

    /**
     * 결제 중복 방지용 락
     * @param orderId 주문 ID
     * @return 락 결과
     */
    default CompletableFuture<LockResult> acquirePaymentLock(String orderId) {
        String lockKey = "payment:" + orderId;
        return acquireLock(lockKey, Duration.ofMinutes(10)); // 10분 TTL
    }

    /**
     * 결제 락 해제
     */
    default CompletableFuture<Boolean> releasePaymentLock(String orderId, String lockToken) {
        String lockKey = "payment:" + orderId;
        return releaseLock(lockKey, lockToken);
    }

    /**
     * 멤버십 갱신 중복 방지용 락
     * @param memberId 회원 ID
     * @return 락 결과
     */
    default CompletableFuture<LockResult> acquireMembershipRenewalLock(String memberId) {
        String lockKey = "membership_renewal:" + memberId;
        return acquireLock(lockKey, Duration.ofMinutes(5)); // 5분 TTL
    }

    /**
     * 멤버십 갱신 락 해제
     */
    default CompletableFuture<Boolean> releaseMembershipRenewalLock(String memberId, String lockToken) {
        String lockKey = "membership_renewal:" + memberId;
        return releaseLock(lockKey, lockToken);
    }

    /**
     * 락 결과
     */
    class LockResult {
        private final boolean acquired;
        private final String lockToken;
        private final String lockKey;
        private final LocalDateTime acquiredAt;
        private final LocalDateTime expiresAt;
        private final String errorMessage;

        private LockResult(boolean acquired, String lockToken, String lockKey,
                           LocalDateTime acquiredAt, LocalDateTime expiresAt, String errorMessage) {
            this.acquired = acquired;
            this.lockToken = lockToken;
            this.lockKey = lockKey;
            this.acquiredAt = acquiredAt;
            this.expiresAt = expiresAt;
            this.errorMessage = errorMessage;
        }

        public static LockResult success(String lockToken, String lockKey, Duration ttl) {
            LocalDateTime now = LocalDateTime.now();
            return new LockResult(true, lockToken, lockKey, now, now.plus(ttl), null);
        }

        public static LockResult failure(String lockKey, String errorMessage) {
            return new LockResult(false, null, lockKey, null, null, errorMessage);
        }

        public static LockResult alreadyLocked(String lockKey) {
            return failure(lockKey, "이미 다른 프로세스에서 락을 보유하고 있습니다");
        }

        public static LockResult timeout(String lockKey) {
            return failure(lockKey, "락 획득 대기 시간이 초과되었습니다");
        }

        // Getters
        public boolean isAcquired() { return acquired; }
        public String getLockToken() { return lockToken; }
        public String getLockKey() { return lockKey; }
        public LocalDateTime getAcquiredAt() { return acquiredAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getErrorMessage() { return errorMessage; }

        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        public Duration getRemainingTtl() {
            if (expiresAt == null) {
                return Duration.ZERO;
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(expiresAt)) {
                return Duration.ZERO;
            }
            return Duration.between(now, expiresAt);
        }
    }

    /**
     * 락 상태
     */
    class LockStatus {
        private final String lockKey;
        private final boolean locked;
        private final String currentHolder;
        private final LocalDateTime lockedAt;
        private final LocalDateTime expiresAt;
        private final int waitingCount; // 대기 중인 프로세스 수

        public LockStatus(String lockKey, boolean locked, String currentHolder,
                          LocalDateTime lockedAt, LocalDateTime expiresAt, int waitingCount) {
            this.lockKey = lockKey;
            this.locked = locked;
            this.currentHolder = currentHolder;
            this.lockedAt = lockedAt;
            this.expiresAt = expiresAt;
            this.waitingCount = waitingCount;
        }

        // Getters
        public String getLockKey() { return lockKey; }
        public boolean isLocked() { return locked; }
        public String getCurrentHolder() { return currentHolder; }
        public LocalDateTime getLockedAt() { return lockedAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public int getWaitingCount() { return waitingCount; }

        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        public Duration getRemainingTtl() {
            if (expiresAt == null) {
                return Duration.ZERO;
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(expiresAt)) {
                return Duration.ZERO;
            }
            return Duration.between(now, expiresAt);
        }
    }

    /**
     * 락 타입
     */
    enum LockType {
        RESERVATION("예약"),
        PAYMENT("결제"),
        MEMBERSHIP("멤버십"),
        RESOURCE("리소스"),
        MEMBER("회원"),
        SYSTEM("시스템");

        private final String displayName;

        LockType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 락 우선순위
     */
    enum LockPriority {
        HIGH(1),
        NORMAL(2),
        LOW(3);

        private final int level;

        LockPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 예약 락 키 생성 헬퍼 메서드
     */
    private String generateReservationLockKey(String resourceId,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        return String.format("reservation:%s:%s:%s",
                resourceId,
                startTime.toString(),
                endTime.toString());
    }

    /**
     * 배치 락 획득 (여러 락을 한번에 처리)
     * @param lockKeys 락 키 목록
     * @param ttl 락 유효시간
     * @return 각 락별 결과
     */
    CompletableFuture<java.util.Map<String, LockResult>> acquireMultipleLocks(
            java.util.List<String> lockKeys, Duration ttl);

    /**
     * 배치 락 해제
     * @param lockTokens 락 키와 토큰의 매핑
     * @return 각 락별 해제 성공 여부
     */
    CompletableFuture<java.util.Map<String, Boolean>> releaseMultipleLocks(
            java.util.Map<String, String> lockTokens);

    /**
     * 락 통계 정보 조회
     * @param lockKeyPattern 락 키 패턴 (와일드카드 지원)
     * @return 락 통계
     */
    CompletableFuture<LockStatistics> getLockStatistics(String lockKeyPattern);

    /**
     * 락 통계 정보
     */
    class LockStatistics {
        private final long totalLocks;
        private final long activeLocks;
        private final long expiredLocks;
        private final long averageHoldTime;
        private final java.util.Map<String, Integer> locksByType;
        private final LocalDateTime generatedAt;

        public LockStatistics(long totalLocks, long activeLocks, long expiredLocks,
                              long averageHoldTime, java.util.Map<String, Integer> locksByType) {
            this.totalLocks = totalLocks;
            this.activeLocks = activeLocks;
            this.expiredLocks = expiredLocks;
            this.averageHoldTime = averageHoldTime;
            this.locksByType = locksByType != null ? locksByType : java.util.Map.of();
            this.generatedAt = LocalDateTime.now();
        }

        // Getters
        public long getTotalLocks() { return totalLocks; }
        public long getActiveLocks() { return activeLocks; }
        public long getExpiredLocks() { return expiredLocks; }
        public long getAverageHoldTime() { return averageHoldTime; }
        public java.util.Map<String, Integer> getLocksByType() { return locksByType; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }

        public double getActiveRatio() {
            return totalLocks > 0 ? (double) activeLocks / totalLocks : 0.0;
        }
    }

    /**
     * 자동 락 해제 기능을 가진 AutoCloseable 락
     * try-with-resources 구문에서 사용 가능
     */
    interface AutoReleaseLock extends AutoCloseable {
        String getLockToken();
        String getLockKey();
        boolean isAcquired();
        LocalDateTime getAcquiredAt();
        LocalDateTime getExpiresAt();

        @Override
        void close(); // Exception을 던지지 않도록 오버라이드
    }

    /**
     * AutoReleaseLock 획득
     * try-with-resources 구문에서 사용하여 자동으로 락이 해제되도록 함
     *
     * try (AutoReleaseLock lock = lockManager.acquireAutoReleaseLock(lockKey, ttl).join()) {
     *     if (lock.isAcquired()) {
     *         // 임계 영역 코드 실행
     *     }
     * } // 자동으로 락 해제됨
     */
    CompletableFuture<AutoReleaseLock> acquireAutoReleaseLock(String lockKey, Duration ttl);
}