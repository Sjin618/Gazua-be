package com.acc.gazua.domain.accommodation.service;

import com.acc.gazua.domain.accommodation.dto.AccommodationGetResponse;
import com.acc.gazua.domain.accommodation.dto.AccommodationRegisterRequest;
import com.acc.gazua.domain.accommodation.dto.AccommodationSearchDto;
import com.acc.gazua.domain.accommodation.dto.AccommodationSearchRequest;
import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.entity.CheckDate;
import com.acc.gazua.domain.accommodation.exception.AccommodationException;
import com.acc.gazua.domain.accommodation.repository.AccommodationQueryRepository;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.accommodationimage.repository.AccommodationImageRepository;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.policy.service.CancellationPolicyResolver;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final AccommodationQueryRepository accommodationQueryRepository;
    private final UserRepository userRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final AccommodationImageRepository accommodationImageRepository;
    private final JsonCursorUtils jsonCursorUtils;
    private final CancellationPolicyResolver policyResolver;

    public CursorPage<AccommodationSearchDto> searchAccommodation(AccommodationSearchRequest request) {
        CheckDate checkDate = new CheckDate(request.checkIn(), request.checkOut());

        //조회 전에 날짜부터 검증
        long stayDays = checkDate.stayDays();

        CustomCursor customCursor = jsonCursorUtils.deserialize(request.cursor());
        Long cursorId = customCursor == null ? null: customCursor.cursorId();
        String cursorValue = customCursor == null ? null : customCursor.cursorValue();
        int size = request.size();

        List<AccommodationSearchDto> accs = accommodationQueryRepository.search(request.region(),
                request.personal(),
                request.sortType(),
                cursorValue,
                cursorId,
                size + 1);

        //더 이상 항목이 존재하지 않는다면 빈 리스트 반환
        if (accs.isEmpty()) {
            return CursorPage.empty();
        }

        //다음 요소가 있는지 확인
        boolean hasNext = accs.size() > size;
        if (hasNext) {
            accs.remove(accs.size() - 1);
        }

        //숙소에서 최저가 방 가격 가져오기
        List<Long> accIds = accs.stream()
                .map(AccommodationSearchDto::getId)
                .toList();

        List<AvailableRoomDto> rooms = roomInventoryRepository.findAvailableRoomsWithTotalPrice(accIds,
                checkDate.checkIn(),
                checkDate.checkOut(),
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

        String encodedCursor = hasNext ?
                jsonCursorUtils.serialize(new CustomCursor(request.sortType().cursorValueOf(lastItem), lastItem.getId())) : null;

        return new CursorPage<>(accs, encodedCursor, hasNext);
    }

    @Transactional
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

    //숙소 상세 조회
    public AccommodationGetResponse getAccommodation(Long accId){
        Accommodation accommodation = accommodationRepository.findById(accId)
                .orElseThrow(() -> new AccommodationException(ErrorCode.ACCOMMODATION_NOT_FOUND));

        List<String> images = accommodationImageRepository.findImageUrlByAccommodationId(accId);

        CancellationPolicyType cancellationPolicy = policyResolver.resolve(accommodation);

        return AccommodationGetResponse.from(accommodation, images, cancellationPolicy);
    }

}
