package com.hexapass.domain.policy;

/**
 * 예약 정책 인터페이스
 * 사양(Specification)들을 조합하여 예약 가능성을 판단
 */
public interface ReservationPolicy {

    /**
     * 예약 가능한지 확인
     *
     * @param context 예약 컨텍스트
     * @return 예약 가능하면 true, 불가능하면 false
     */
    boolean canReserve(ReservationContext context);

    /**
     * 예약 불가능한 이유 반환
     * 예약이 불가능한 경우 구체적인 사유를 제공
     *
     * @param context 예약 컨텍스트
     * @return 예약 불가능한 이유 (예약 가능한 경우 "예약 가능" 반환)
     */
    String getViolationReason(ReservationContext context);

    /**
     * 정책 설명
     *
     * @return 이 정책에 대한 설명
     */
    String getDescription();
}