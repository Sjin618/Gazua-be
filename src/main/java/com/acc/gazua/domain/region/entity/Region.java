package com.acc.gazua.domain.region.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class Region {

    @EmbeddedId
    private AreaCode areaCode;

    private String sido;
    private String sigungu;
}
