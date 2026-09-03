package com.acc.gazua.domain.accommodation.entity;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CheckDate {
    @NotNull LocalDate checkIn;
    @NotNull LocalDate checkOut;
}
