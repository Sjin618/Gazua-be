package com.acc.gazua.domain.accommodation.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CheckDate {
    LocalDate checkIn;
    LocalDate checkOut;
}
