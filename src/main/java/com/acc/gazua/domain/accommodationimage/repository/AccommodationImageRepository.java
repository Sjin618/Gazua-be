package com.acc.gazua.domain.accommodationimage.repository;

import com.acc.gazua.domain.accommodationimage.entity.AccommodationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccommodationImageRepository extends JpaRepository<AccommodationImage,Long> {

    @Query("SELECT a.imageUrl FROM AccommodationImage a WHERE a.accommodation.id = :accId ORDER BY a.id ASC")
    List<String> findImageUrlByAccommodationId(@Param("accId") Long accId);
}
