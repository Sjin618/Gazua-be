package com.acc.gazua.domain.accommodation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AccommodationSearchDto {
    private Long id;
    private String name;
    private String thumbnailUrl;
    private BigDecimal averageStar;
    private Integer reviewCount;
    private Integer minPrice;
    private boolean available;

    public AccommodationSearchDto(Long id, String name, String thumbnailUrl, BigDecimal averageStar, Integer reviewCount) {
        this.id = id;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.averageStar = averageStar;
        this.reviewCount = reviewCount;
    }
}
