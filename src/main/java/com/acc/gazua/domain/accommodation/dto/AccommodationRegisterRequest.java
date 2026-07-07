package com.acc.gazua.domain.accommodation.dto;

import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import com.acc.gazua.domain.accommodation.entity.Address;
import lombok.Getter;

@Getter
public class AccommodationRegisterRequest {
    private Long hostId; //숙소 주인 ID
    private String name; //숙소 이름
    private Address address; //숙소 주소
    private AccommodationType type; //숙소 타입
}
