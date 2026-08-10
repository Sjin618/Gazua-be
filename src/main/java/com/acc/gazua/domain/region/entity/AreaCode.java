package com.acc.gazua.domain.region.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class AreaCode implements Serializable {
    private Integer regionCode;
    private Integer sigunguCode;
}
