package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.Gender;

import java.time.LocalDate;

public record UserUpdateRequest(
        String name,
        String nickname,
        String phoneNumber,
        Gender gender,
        LocalDate birthDate
) {
}
