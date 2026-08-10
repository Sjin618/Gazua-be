package com.acc.gazua.domain.roominventory.service;

import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.roominventory.entity.RoomInventory;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryJdbcRepository;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomInventoryService {
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomInventoryJdbcRepository roomInventoryJdbcRepository;


    //AccommodationRunner 에서 사용
    public void createInitialInventories(Room room, int days, int weekdayPrice, int weekendPrice) {
        LocalDate startDate = LocalDate.now();
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

}
