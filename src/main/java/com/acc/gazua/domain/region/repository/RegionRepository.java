package com.acc.gazua.domain.region.repository;

import com.acc.gazua.domain.region.entity.AreaCode;
import com.acc.gazua.domain.region.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends JpaRepository<Region, AreaCode> {



}
