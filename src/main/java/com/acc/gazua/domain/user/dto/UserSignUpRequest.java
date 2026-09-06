package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserSignUpRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 아닙니다.")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, max = 24, message = "비밀번호는 8 ~ 24 글자만 허용됩니다.")
        String password,
        @NotBlank(message = "닉네임은 필수입니다.") @Size(min = 2, max = 20,message = "닉네임은 2 ~ 20 글자만 허용됩니다.")
        String nickname,
        @NotNull(message = "성별은 필수입니다.")
        Gender gender,
        @Pattern(regexp = "^\\d{11}$",message = "핸드폰 번호를 - 없이 입력해주세요")
        @NotBlank(message = "핸드폰 번호는 필수입니다.")
        String phoneNumber,
        @NotNull(message = "생년월일은 필수입니다.") @Past(message = "불가능한 생년월일입니다.")
        LocalDate birthDate,
        @NotBlank(message = "이름은 필수입니다.") @Size(min = 2, max = 20,message = "이름은 2 ~ 20 글자만 허용됩니다.")
        String name
) {
}
