package com.acc.gazua.domain.review.dto;

import java.math.BigDecimal;

public record ReviewCreateRequest(
        Long reservationId,
        String sentence,
        BigDecimal star
) {

}
