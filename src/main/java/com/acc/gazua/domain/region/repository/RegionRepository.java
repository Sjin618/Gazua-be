package com.acc.gazua.domain.region.repository;

import com.acc.gazua.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    @Query("SELECT r FROM Region r WHERE r.regionCode = :regionCode")
    Optional<Region> findByRegionCode(@Param(("regionCode")) String regionCode);
}
