package com.acc.gazua.domain.review.dto;

import com.acc.gazua.global.validation.Star;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ReviewCreateRequest(
        @NotNull(message = "예약 번호는 필수입니다.")
        Long reservationId,
        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        @Size(max = 1000, message = "리뷰는 1000자를 넘을 수 없습니다.")
        String sentence,

        @NotNull(message = "별점은 필수입니다.")
        @Star
        BigDecimal star
) {

}
