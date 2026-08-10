package com.acc.gazua.domain.region.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Region {

    @EmbeddedId
    private AreaCode areaCode;

    private String regionName;
    private String sigunguName;
}
