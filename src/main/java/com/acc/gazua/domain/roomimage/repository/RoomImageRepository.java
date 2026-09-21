package com.acc.gazua.domain.roomimage.repository;

import com.acc.gazua.domain.roomimage.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    @Query("SELECT i.imageUrl FROM RoomImage i WHERE i.room.id = :roomId ORDER BY i.id ASC")
    List<String> findImageUrlByRoomId(@Param("roomId") Long roomId);
}
