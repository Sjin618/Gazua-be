package com.acc.gazua.domain.payment.dto;

public record PaymentVerifyRequest(
        String impUid,
        String merchantUid
) {
}
