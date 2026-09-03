package com.acc.gazua.domain.like.dto;

import com.acc.gazua.domain.accommodation.entity.AccommodationType;

import java.math.BigDecimal;

public interface LikeAccommodationDto {

    Long getLikeId();
    Long getAccommodationId();
    String getName();
    BigDecimal getAverageStar();
    Long getReviewCount();
    AccommodationType getType();
    String getThumbnailUrl();
}
