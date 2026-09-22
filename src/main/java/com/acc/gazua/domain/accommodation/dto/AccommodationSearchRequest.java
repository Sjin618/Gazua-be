package com.acc.gazua.domain.accommodation.dto;

import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record AccommodationSearchRequest(
        @NotBlank(message = "지역은 필수입니다.")
        String region,
        @NotNull(message = "체크인 날짜는 필수입니다.")
        @FutureOrPresent(message = "과거 날짜는 조회가 불가능합니다.")
        LocalDate checkIn,
        @NotNull(message = "체크아웃 날짜는 필수입니다.")
        LocalDate checkOut,
        @NotNull(message = "기준 인원은 필수입니다.") @Min(value = 1, message = "기준 인원은 최소 1명 이상이어야 합니다.")
        Integer personal,

        AccommodationSortType sortType,
        String cursor,
        List<AccommodationType> types,
        @Min(value = 1, message = "값이 1이상이어야 합니다.") @Max(value = 20, message = "값이 20 이하여야 합니다.")
        Integer size

) {
        public AccommodationSearchRequest{
                if(sortType == null) sortType = AccommodationSortType.AVERAGESTAR;
                if(size == null) size = 10;
        }
}
