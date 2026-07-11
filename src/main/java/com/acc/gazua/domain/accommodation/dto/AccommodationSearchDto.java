package com.acc.gazua.domain.accommodation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AccommodationSearchDto {
    private Long id;
    private String name;
    private String thumbnailUrl;
    private BigDecimal averageStar;
    private Long reviewCount;
}
