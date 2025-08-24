package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * 리소스 수용 인원 확인 사양
 * 리소스에 여유 공간이 있는지 확인
 */
public class ResourceCapacitySpecification implements ReservationSpecification {

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return !context.isResourceFull();
    }

    @Override
    public String getDescription() {
        return "리소스 수용 인원 여유";
    }
}