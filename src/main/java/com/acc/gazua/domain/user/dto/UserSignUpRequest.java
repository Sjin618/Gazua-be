package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

public record UserSignUpRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String nickname,
        @NotNull Gender gender,
        @Pattern(regexp = "^\\d{11}$") String phoneNumber,
        @NotNull LocalDate birthDate,
        @NotBlank String name
) {
}
