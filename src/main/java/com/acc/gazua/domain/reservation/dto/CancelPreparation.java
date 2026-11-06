package com.acc.gazua.domain.reservation.dto;

import com.acc.gazua.domain.policy.dto.RefundCalculation;

public record CancelPreparation(
        Long refundId,
        RefundCalculation calc
) {
}
