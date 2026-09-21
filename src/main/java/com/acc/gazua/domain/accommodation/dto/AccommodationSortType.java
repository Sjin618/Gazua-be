package com.acc.gazua.domain.accommodation.dto;

import java.util.function.Function;

public enum AccommodationSortType {
    REVIEW(dto -> dto.getReviewCount().toString()),
    AVERAGESTAR(dto -> dto.getAverageStar().toString());

    private final Function<AccommodationSearchDto,String> cursorValueExtractor;

    AccommodationSortType(Function<AccommodationSearchDto, String> cursorValueExtractor) {
        this.cursorValueExtractor = cursorValueExtractor;
    }

    public String cursorValueOf(AccommodationSearchDto dto){
        return cursorValueExtractor.apply(dto);
    }
}
