package com.acc.gazua.domain.Payment.dto;

public record PaymentVerifyRequest(
        String impUid,
        String merchantUid
) {
}
