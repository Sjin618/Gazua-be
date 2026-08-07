package com.acc.gazua.global.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Embeddable
public class CheckDateTime {
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
}
