package com.acc.gazua.domain.like.dto;

import com.acc.gazua.domain.accommodation.entity.AccommodationType;

import java.math.BigDecimal;

public interface LikeAccommodationDto {

    Long getId();
    String getName();
    BigDecimal getAverageStar();
    Long getReviewCount();
    AccommodationType getType();
    String getThumbnailUrl();
}
