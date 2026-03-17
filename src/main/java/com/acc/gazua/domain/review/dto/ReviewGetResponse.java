package com.acc.gazua.domain.review.dto;

import java.math.BigDecimal;

public interface ReviewGetResponse{
    String getNickName();
    String getProfileImage();
    BigDecimal getStar();
    String getSentence();
    Long getReviewId();
}
