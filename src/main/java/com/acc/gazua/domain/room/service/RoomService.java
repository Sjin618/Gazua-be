package com.acc.gazua.domain.room.service;

import com.acc.gazua.domain.accommodation.entity.CheckDate;
import com.acc.gazua.domain.accommodation.exception.AccommodationException;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.room.dto.RoomGetResponse;
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.room.exception.RoomException;
import com.acc.gazua.domain.room.repository.RoomRepository;
import com.acc.gazua.domain.roomimage.repository.RoomImageRepository;
import com.acc.gazua.domain.roominventory.dto.AvailableRoomDto;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import com.acc.gazua.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final RoomInventoryRepository roomInventoryRepository;

    //인원 조건에 맞는 방이 먼저, 같은 그룹 안에서는 저렴한 순
    //가격이 없는 방(예약 불가)은 각 그룹의 맨 뒤로 보낸다
    private static final Comparator<RoomGetResponse> ROOM_ORDER =
            Comparator.comparing((RoomGetResponse room) -> room.capacityMatched()).reversed()
                    .thenComparing(RoomGetResponse::totalPrice,
                            Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    //방 목록 조회
    //기간 내 가격과 예약 가능 여부를 함께 내려준다
    public List<RoomGetResponse> getRooms(Long accId, CheckDate checkDate, Integer personal) {
        long stayDays = checkDate.stayDays();

        List<Room> rooms = roomRepository.findByAccommodationId(accId);
        if (rooms.isEmpty()) {
            //방이 없는 숙소와 존재하지 않는 숙소를 구분
            if (!accommodationRepository.existsById(accId)) {
                throw new AccommodationException(ErrorCode.ACCOMMODATION_NOT_FOUND);
            }
            return List.of();
        }

        //기간 내내 재고가 있는 방만 조회되므로, 결과에 없는 방은 예약 불가
        Map<Long, AvailableRoomDto> availableRooms = roomInventoryRepository
                .findAvailableRoomsWithTotalPrice(List.of(accId),
                        checkDate.checkIn(),
                        checkDate.checkOut(),
                        stayDays)
                .stream()
                .collect(Collectors.toMap(AvailableRoomDto::getRoomId, Function.identity()));

        return rooms.stream()
                .map(room -> RoomGetResponse.of(room, availableRooms.get(room.getId()), personal, stayDays))
                .sorted(ROOM_ORDER)
                .toList();
    }

    //방 사진 전체 조회
    //방 목록에서 대표 사진을 눌렀을 때 사용
    public List<String> getRoomImages(Long roomId) {
        List<String> images = roomImageRepository.findImageUrlByRoomId(roomId);

        //사진이 없는 방과 존재하지 않는 방을 구분
        if (images.isEmpty() && !roomRepository.existsById(roomId)) {
            throw new RoomException(ErrorCode.ROOM_NOT_FOUND);
        }

        return images;
    }
}
