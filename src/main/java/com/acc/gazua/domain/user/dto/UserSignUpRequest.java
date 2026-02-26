package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserSignUpRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String nickname,
        @NotNull Gender gender
) {
}
