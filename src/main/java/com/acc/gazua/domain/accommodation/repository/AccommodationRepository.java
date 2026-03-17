package com.acc.gazua.domain.accommodation.repository;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation,Long> {

    @Modifying
    @Query("UPDATE Accommodation a " +
            "SET a.reviewCount = a.reviewCount + :deltaCount, " +
            "    a.totalStar = a.totalStart + :deltaStar, " +
            "    a.averageStar = CASE WHEN (a.reviewCount + :deltaCount > 0)" +
            "                         THEN (a.totalStar + :deltaStar) / (a.reviewCount + :deltaCount)" +
            "                         ELSE 0 END " +
            "WHERE a.id = :accId AND a.reviewCount + :deltaCount >= 0")
    void updateStats(@Param("accId") Long accId, @Param("deltaCount") Long deltaCount, @Param("deltaStar") BigDecimal deltaStar);
}
