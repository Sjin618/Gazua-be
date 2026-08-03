package com.acc.gazua.domain.reservation.dto;


import com.acc.gazua.global.entity.CheckDateTIme;

import java.time.LocalDateTime;

public interface ReservationGetResponse {
    Long getId(); //예약 ID
    String getThumbnailUrl(); //방 썸네일 이미지
    String getAccommodationName(); //숙소 이름
    String getRoomName(); //방 이름
    LocalDateTime getCheckInDateTime();
    LocalDateTime getCheckOutDateTime();
}
