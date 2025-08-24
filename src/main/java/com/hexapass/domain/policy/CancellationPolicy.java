package com.hexapass.domain.policy;

import com.hexapass.domain.common.Money;

/**
 * 취소 정책 인터페이스
 * 예약 취소 시 적용되는 수수료와 조건을 정의
 */
public interface CancellationPolicy {

    /**
     * 취소 수수료 계산
     *
     * @param originalPrice 원래 결제 금액
     * @param context 취소 컨텍스트 정보
     * @return 취소 수수료
     */
    Money calculateCancellationFee(Money originalPrice, CancellationContext context);

    /**
     * 취소 가능한지 확인
     *
     * @param context 취소 컨텍스트 정보
     * @return 취소 가능하면 true, 불가능하면 false
     */
    boolean isCancellationAllowed(CancellationContext context);

    /**
     * 취소 불가능한 이유
     * 취소가 불가능한 경우 구체적인 사유를 제공
     *
     * @param context 취소 컨텍스트 정보
     * @return 취소 불가능한 이유 (취소 가능한 경우 null 반환)
     */
    String getCancellationDenialReason(CancellationContext context);

    /**
     * 정책 설명
     *
     * @return 이 취소 정책에 대한 설명
     */
    String getDescription();
}