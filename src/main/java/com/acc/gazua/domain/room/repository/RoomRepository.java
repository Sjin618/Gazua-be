package com.acc.gazua.domain.room.repository;

import com.acc.gazua.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room,Long> {

    @Query("SELECT r FROM Room r JOIN FETCH r.accommodation a WHERE r.id = :roomId")
    Optional<Room> findByIdWithAccommodation(@Param("roomId") Long roomId);
}
