package com.acc.gazua.domain.accommodation.dto;

import lombok.Getter;

@Getter
public class AccommodationSearchRequest {
    private String cursor;
    private Integer size;
}
