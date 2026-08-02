package com.acc.gazua.domain.region.entity;

import jakarta.persistence.Embeddable;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
public class AreaCode implements Serializable {
    private Integer areaCode;
    private Integer sigunguCode;
}
