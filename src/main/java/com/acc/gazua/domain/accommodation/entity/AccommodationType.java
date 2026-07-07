package com.acc.gazua.domain.accommodation.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AccommodationType {
    MOTEL("모텔"),
    HOTEL("호텔"),
    PENSION("펜션"),
    CAMPING("캠핑");

    private final String name;

    AccommodationType(String name){
        this.name = name;
    }

    @JsonValue
    public String getName(){
        return this.name;
    }
}
