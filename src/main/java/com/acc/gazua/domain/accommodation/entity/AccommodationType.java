package com.acc.gazua.domain.accommodation.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccommodationType {
    MOTEL("모텔"),
    HOTEL("호텔"),
    PENSION("펜션"),
    CAMPING("캠핑"),
    GUEST_HOUSE("게스트하우스"),
    HANOK("한옥"),
    ;

    private final String name;

    AccommodationType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return this.name; // 응답 : "호텔"
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AccommodationType from(String value) {
        return AccommodationType.valueOf(value); //요청 : "HOTEL"만 허용
    }
}
