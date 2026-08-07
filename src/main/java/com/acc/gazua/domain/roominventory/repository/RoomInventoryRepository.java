package com.acc.gazua.domain.roominventory.repository;

import com.acc.gazua.domain.roominventory.dto.AvailableRoomDto;
import com.acc.gazua.domain.roominventory.entity.RoomInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {

    @Query("SELECT new com.acc.gazua.domain.roominventory.dto.AvailableRoomDto(r.accommodation.id,r.id,SUM(ri.price)) " +
            "FROM RoomInventory ri " +
            "JOIN ri.room r " +
            "WHERE r.accommodation.id IN :accIds " +
            "AND ri.date >= :checkIn AND ri.date < :checkOut " +
            "AND ri.stock > 0 " +
            "GROUP BY r.accommodation.id, r.id " +
            "HAVING COUNT(ri.id) > :stayDays")
    List<AvailableRoomDto> findAvailableRoomsWithTotalPrice(@Param("accIds") List<Long> accIds,
                                                            @Param("checkIn") LocalDate checkIn,
                                                            @Param("checkOut") LocalDate checkOut,
                                                            @Param("stayDays") long stayDays);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r " +
            "FROM RoomInventory r " +
            "WHERE r.room.id = :roomId " +
            "AND r.date >= :checkIn " +
            "AND r.date < :checkOut")
    List<RoomInventory> findInventoriesForUpdate(@Param("roomId") Long roomId,
                                                 @Param("checkIn") LocalDate checkIn,
                                                 @Param("checkOut") LocalDate checkOut);
}
