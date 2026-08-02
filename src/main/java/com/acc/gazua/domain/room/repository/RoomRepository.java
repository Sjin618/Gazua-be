package com.acc.gazua.domain.room.repository;

import com.acc.gazua.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room,Long> {
}
