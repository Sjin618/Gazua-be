package com.acc.gazua.data;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import com.acc.gazua.domain.region.entity.AreaCode;
import com.acc.gazua.domain.region.entity.Region;
import com.acc.gazua.domain.region.repository.RegionRepository;
import com.acc.gazua.domain.roominventory.service.RoomInventoryService;
import com.acc.gazua.global.entity.CheckTime;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.room.repository.RoomRepository;
import com.acc.gazua.domain.roomimage.entity.RoomImage;
import com.acc.gazua.domain.roomimage.repository.RoomImageRepository;
import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.entity.Role;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AccommodationRunner implements CommandLineRunner {

    private final SearchStayList searchStayList;
    private final SearchRoom searchRoom;
    private final SearchStay searchStay;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomImageRepository roomImageRepository;
    private final RoomInventoryService roomInventoryService;
    private final RegionRepository regionRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("데이터 요청 시작");
        List<TourAccListResponseDto.Item> items = searchStayList.searchAccommodation(1);
        for (TourAccListResponseDto.Item acc : items) {
            TourAccResponseDto.Item description = searchStay.searchStayDescription(acc.getContentid());
            List<TourRoomResponseDto.Item> rooms = searchRoom.searchRoom(acc.getContenttypeid(), acc.getContentid());
            if(rooms == null) continue;
            if(rooms.get(0).getRoomtitle() == null || rooms.get(0).getRoomtitle().isBlank()) continue;
            Accommodation savedAccommodation = createAndSaveAccommodation(acc, rooms, description);
            createAndSaveRooms(rooms,savedAccommodation);
        }


    }

    public Accommodation createAndSaveAccommodation(TourAccListResponseDto.Item acc, List<TourRoomResponseDto.Item> rooms,TourAccResponseDto.Item description){
        OptionalInt maxCapacity = rooms.stream()
                .map(TourRoomResponseDto.Item::getRoommaxcount)
                .filter(count -> count != null && !count.isBlank())
                .mapToInt(Integer::parseInt)
                .max();

        UserSignUpRequest userRequest = UserRandomGenerator.createRandomUserHost();
        User host = User.builder()
                .email(userRequest.email())
                .password(userRequest.password())
                .name(userRequest.name())
                .phoneNumber(userRequest.phoneNumber())
                .birthDate(userRequest.birthDate())
                .gender(userRequest.gender())
                .profileImage(null)
                .role(Role.HOST)
                .nickname(userRequest.nickname())
                .build();

        Region region = regionRepository.findById(new AreaCode(Integer.parseInt(description.getLDongRegnCd()), Integer.parseInt(description.getLDongSignguCd())))
                .orElseGet(() -> new Region(new AreaCode(11,110),"서울특별시","종로구"));

        Accommodation accommodation = Accommodation.builder()
                .host(host)
                .name(acc.getTitle())
                .type(divideAccommodationType(acc.getCat3(), acc.getTitle()))
                .address(acc.getAddr1())
                .description(description.getOverview())
                .region(region)
                .thumbnailUrl(acc.getFirstimage())
                .maxCapacity(maxCapacity.orElse(5))
                .build();
        userRepository.save(host);
        return accommodationRepository.save(accommodation);
    }

    private void createAndSaveRooms(List<TourRoomResponseDto.Item> rooms,Accommodation accommodation){
        for (TourRoomResponseDto.Item room : rooms) {
            String baseCountString = room.getRoombasecount();
            String maxCountString = room.getRoommaxcount();
            String roomCountString = room.getRoomcount();

            Integer baseCount = baseCountString != null && !baseCountString.isBlank() ? Integer.parseInt(baseCountString) : ThreadLocalRandom.current().nextInt(2,6);
            Integer maxCount = maxCountString != null && !maxCountString.isBlank() ? Integer.parseInt(maxCountString) : baseCount + 1;
            Integer roomCount = roomCountString != null && !roomCountString.isBlank() && !"0".equals(roomCountString) ? Integer.parseInt(roomCountString) : ThreadLocalRandom.current().nextInt(3,6);

            int checkInHour = ThreadLocalRandom.current().nextInt(14,16);
            int checkOutHour = ThreadLocalRandom.current().nextInt(10,13);

            LocalTime checkIn = LocalTime.of(checkInHour,0);
            LocalTime checkOut = LocalTime.of(checkOutHour,0);

            Room saveRoom = Room.builder()
                    .accommodation(accommodation)
                    .name(room.getRoomtitle())
                    .checkTime(new CheckTime(checkIn,checkOut))
                    .standardCapacity(baseCount)
                    .maxCapacity(maxCount)
                    .amount(roomCount)
                    .build();

            List<RoomImage> images = Stream.of(room.getRoomimg1(),
                    room.getRoomimg2(),
                    room.getRoomimg3(),
                    room.getRoomimg4(),
                    room.getRoomimg5())
                    .filter(Objects::nonNull)
                    .filter(img -> !img.isBlank())
                    .map(img -> RoomImage.builder()
                            .imageUrl(img)
                            .room(saveRoom)
                            .build())
                    .toList();

            if(!images.isEmpty()) {
                saveRoom.changeThumbnail(images.get(0).getImageUrl());
                saveRoom.setImageCount(images.size());
            }

            roomRepository.save(saveRoom);
            roomImageRepository.saveAll(images);


            String weekdayMinFee = room.getRoomoffseasonminfee1();
            String weekendMinFee = room.getRoomoffseasonminfee2();
            int weekdayPrice = weekdayMinFee != null && !weekdayMinFee.isBlank() ? Integer.parseInt(weekdayMinFee) : ThreadLocalRandom.current().nextInt(50000, 1000000);
            int weekendPrice = weekendMinFee != null && !weekendMinFee.isBlank() ? Integer.parseInt(weekendMinFee) : weekdayPrice + 50000;

            createRoomInventories(saveRoom,180,weekdayPrice,weekendPrice);
        }
    }

    private void createRoomInventories(Room room, int days, int weekdayPrice, int weekendPrice) {
        roomInventoryService.createInitialInventories(room,days,weekdayPrice,weekendPrice);
    }

//
//    B02010100,관광호텔
//            B02010200,콘도미니엄 -> 호텔 리조트
//    B02010300,유스호스텔 -> 모텔
//            B02010700,펜션
//    B02010900,"모텔, 여관 (일반숙박시설)"
//    B02011100,한옥스테이 -> 한옥
//            B02011500,베니키아 -> 호텔
//    B02011600,굿스테이 -> 모텔
//    B02011700,홈스테이 / 게스트하우스
    private AccommodationType divideAccommodationType(String cat3,String title){
        if("B02010100".equals(cat3) || "B02010200".equals(cat3) || "B02011500".equals(cat3) || title.contains("호텔")) return AccommodationType.HOTEL;
        if("B02010300".equals(cat3) || "B02010900".equals(cat3) || "B02011600".equals(cat3) || title.contains("모텔")) return AccommodationType.MOTEL;
        if("B02010700".equals(cat3) || title.contains("펜션")) return AccommodationType.PENSION;
        if("B02011100".equals(cat3) || title.contains("한옥")) return AccommodationType.HANOK;
        if("B02011700".equals(cat3) || title.contains("게스트")) return AccommodationType.GUEST_HOUSE;
        return AccommodationType.CAMPING;
    }
}
