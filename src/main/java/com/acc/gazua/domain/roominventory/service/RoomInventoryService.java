package com.acc.gazua.domain.roominventory.service;

import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.roominventory.entity.RoomInventory;
import com.acc.gazua.domain.roominventory.exception.RoomInventoryException;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryJdbcRepository;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.entity.CheckDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomInventoryService {
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomInventoryJdbcRepository roomInventoryJdbcRepository;
    private final Clock clock;


    //AccommodationRunner 에서 사용
    public void createInitialInventories(Room room, int days, int weekdayPrice, int weekendPrice) {
        LocalDate startDate = LocalDate.now(clock);
        LocalDate endDate = startDate.plusDays(days);

        List<RoomInventory> inventories = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    DayOfWeek dayOfWeek = date.getDayOfWeek();

                    boolean isWeekend = (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY);

                    int dailyPrice = isWeekend ? weekendPrice : weekdayPrice;

                    return RoomInventory.builder()
                            .room(room)
                            .date(date)
                            .totalCount(room.getAmount())
                            .availableCount(room.getAmount())
                            .price(dailyPrice)
                            .build();
                })
                .toList();

        roomInventoryJdbcRepository.bulkInsert(inventories);
    }

    public void increaseRoomInventoryCount(CheckDateTime checkDateTime, Room room){
        List<RoomInventory> inventories = roomInventoryRepository.findInventoriesForUpdate(room.getId(),
                checkDateTime.getCheckInDateTime().toLocalDate(),
                checkDateTime.getCheckOutDateTime().toLocalDate());
        inventories.forEach(inventory -> inventory.increaseAvailableCount(1));
    }

    public int checkRoomInventoryAvailability(long nightDays, Room room, LocalDate checkIn, LocalDate checkOut) {
        List<RoomInventory> inventories = roomInventoryRepository.findInventoriesForUpdate(room.getId(), checkIn, checkOut);

        //가져온 재고의 수가 일수와 맞지 않으면 불가능
        if (inventories.size() != nightDays) throw new ReservationException(ErrorCode.RESERVATION_NOT_AVAILABLE);

        //가져온 재고가 1 이상 있지 않으면 예약 불가능
        for (RoomInventory inventory : inventories) {
            if (inventory.getAvailableCount() < 1) throw new RoomInventoryException(ErrorCode.OUT_OF_STOCK);
        }

        return calculatePriceAndDecreaseStock(inventories);
    }

    private int calculatePriceAndDecreaseStock(List<RoomInventory> inventories){
        int price = 0;
        for (RoomInventory inventory : inventories) {
            inventory.decreaseAvailableCount(1);
            price += inventory.getPrice();
        }
        return price;
    }
}
