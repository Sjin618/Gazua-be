package com.acc.gazua.domain.review.dto;

import java.math.BigDecimal;

public record ReviewUpdateRequest(
        String sentence,
        BigDecimal star
) {
}
