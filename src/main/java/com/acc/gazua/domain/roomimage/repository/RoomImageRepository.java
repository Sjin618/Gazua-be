package com.acc.gazua.domain.roomimage.repository;

import com.acc.gazua.domain.roomimage.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

}
