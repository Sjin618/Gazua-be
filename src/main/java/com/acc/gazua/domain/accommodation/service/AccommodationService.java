package com.acc.gazua.domain.accommodation.service;

import com.acc.gazua.domain.accommodation.dto.AccommodationRegisterRequest;
import com.acc.gazua.domain.accommodation.dto.AccommodationSearchDto;
import com.acc.gazua.domain.accommodation.dto.AccommodationSearchRequest;
import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.entity.CheckDate;
import com.acc.gazua.domain.accommodation.repository.AccommodationQueryRepository;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.roominventory.dto.AvailableRoomDto;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.JsonCursorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationQueryRepository accommodationQueryRepository;
    private final UserRepository userRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final JsonCursorUtils jsonCursorUtils;

    public CursorPage<AccommodationSearchDto> searchAccommodation(Integer region,
                                                                  Integer personal,
                                                                  CheckDate checkDate,
                                                                  String sortType,
                                                                  String cursor,
                                                                  int size) {
        CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
        int limit = size + 1;
        Long cursorId = customCursor == null ? Long.MAX_VALUE : customCursor.cursorId();

        List<AccommodationSearchDto> accs = null;
        if ("REVIEW".equals(sortType)) {
            Integer cursorReviewCount = customCursor == null ? null : Integer.parseInt(customCursor.cursorValue());
            accs = accommodationQueryRepository.searchAccommodationSortByReviewDesc(region,
                    personal,
                    cursorReviewCount,
                    cursorId,
                    limit);
        } else if ("AVERAGESTAR".equals(sortType)) {
            BigDecimal cursorAverageStar = customCursor == null ? null : new BigDecimal(customCursor.cursorValue());
            accs = accommodationQueryRepository.searchAccommodationSortByAverageStarDesc(region,
                    personal,
                    cursorAverageStar,
                    cursorId,
                    limit);
        } else {
            throw new IllegalArgumentException("잘못된 형식의 정렬입니다.");
        }

        //더 이상 항목이 존재하지 않는다면 빈 리스트 반환
        if (accs.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), null, false);
        }

        //다음 요소가 있는지 확인
        boolean hasNext = accs.size() > size;
        if (hasNext) {
            accs.remove(accs.size() - 1);
        }

        //숙소에서 최저가 방 가격 가져오기
        long stayDays = ChronoUnit.DAYS.between(checkDate.getCheckIn(), checkDate.getCheckOut());
        if (stayDays <= 0) {
            throw new IllegalArgumentException("체크아웃 날짜는 체크인 날짜보다 최소 1일 이상 커야합니다.");
        }

        List<Long> accIds = accs.stream()
                .map(AccommodationSearchDto::getId)
                .toList();

        List<AvailableRoomDto> rooms = roomInventoryRepository.findAvailableRoomsWithTotalPrice(accIds,
                checkDate.getCheckIn(),
                checkDate.getCheckOut(),
                stayDays);

        //첫 번째 쿼리에서 가져온 숙소와 두 번째 쿼리에서 가져온 최저가 가격 매칭시키기
        //첫 번째 쿼리에서 가져온 순서대로 매칭시키기 위해 Map 사용
        Map<Long, AvailableRoomDto> minPriceRooms = rooms.stream()
                .collect(Collectors.toMap(AvailableRoomDto::getAccId,
                        Function.identity(),
                        (room1, room2) -> room1.getTotalPrice() < room2.getTotalPrice() ? room1 : room2));

        for (AccommodationSearchDto acc : accs) {
            if (minPriceRooms.containsKey(acc.getId())) {
                AvailableRoomDto availableRoomDto = minPriceRooms.get(acc.getId());
                int averageMinPrice = availableRoomDto.getTotalPrice().intValue() / (int) stayDays;

                acc.setAvailable(true);
                acc.setMinPrice(averageMinPrice);
            } else {
                acc.setMinPrice(null);
                acc.setAvailable(false);
            }
        }

        //cursor 를 만들어서 반환
        AccommodationSearchDto lastItem = accs.get(accs.size() - 1);

        String encodedCursor = "";
        if ("REVIEW".equals(sortType)) {
            encodedCursor = jsonCursorUtils.serialize(new CustomCursor(lastItem.getReviewCount().toString(), lastItem.getId()));
        } else if ("AVERAGESTA".equals(sortType)) {
            encodedCursor = jsonCursorUtils.serialize(new CustomCursor(lastItem.getAverageStar().toString(), lastItem.getId()));
        }

        return new CursorPage<>(accs, encodedCursor, hasNext);
    }

    public void registerAccommodation(AccommodationRegisterRequest registerRequest){
        User host = userRepository.findById(registerRequest.getHostId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Accommodation accommodation = Accommodation.builder()
                .host(host)
                .name(registerRequest.getName())
                .type(registerRequest.getType())
                .address(registerRequest.getAddress())
                .build();

        accommodationRepository.save(accommodation);
    }
}
