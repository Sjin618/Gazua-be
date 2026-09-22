package com.acc.gazua.domain.room.dto;

import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.roominventory.dto.AvailableRoomDto;
import com.acc.gazua.global.entity.CheckTime;

import java.time.LocalTime;

public record RoomGetResponse(
        //방 ID
        Long roomId,
        //방 이름
        String roomName,
        //방 대표 사진, 전체 사진은 사진 조회 API 에서 제공
        String thumbnailUrl,
        //방 사진 전체 개수
        Integer imageCount,
        //기준 인원
        Integer standardCapacity,
        //최대 인원
        Integer maxCapacity,
        //체크인 시간
        LocalTime checkInTime,
        //체크아웃 시간
        LocalTime checkOutTime,
        //요청한 인원을 수용할 수 있는지
        boolean capacityMatched,
        //요청한 기간에 예약할 수 있는지
        boolean available,
        //숙박 기간 전체 금액, 예약 불가면 null
        Long totalPrice,
        //1박 평균 금액, 예약 불가면 null
        Integer averageNightPrice
) {
    public static RoomGetResponse of(Room room, AvailableRoomDto availableRoom, Integer personal, long stayDays) {
        //재고 조회 결과에 없는 방은 기간 내내 예약이 불가능한 방
        boolean available = availableRoom != null;
        Long totalPrice = available ? availableRoom.getTotalPrice() : null;
        Integer averageNightPrice = available ? (int) Math.round(totalPrice / (double) stayDays) : null;

        CheckTime checkTime = room.getCheckTime();

        return new RoomGetResponse(
                room.getId(),
                room.getName(),
                room.getThumbnailUrl(),
                room.getImageCount() == null ? 0 : room.getImageCount(),
                room.getStandardCapacity(),
                room.getMaxCapacity(),
                checkTime == null ? null : checkTime.getCheckIn(),
                checkTime == null ? null : checkTime.getCheckOut(),
                room.getMaxCapacity() != null && room.getMaxCapacity() >= personal,
                available,
                totalPrice,
                averageNightPrice
        );
    }
}
