package com.acc.gazua.domain.accommodation.entity;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CheckDate {
    LocalDate checkIn;
    LocalDate checkOut;
}
