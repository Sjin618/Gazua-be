package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserUpdateRequest(
        @Size(min = 2, max = 20,message = "이름은 2 ~ 20 글자만 허용됩니다.")
        String name,
        @Size(min = 2, max = 20,message = "닉네임은 2 ~ 20 글자만 허용됩니다.")
        String nickname,
        @Pattern(regexp = "^\\d{11}$",message = "핸드폰 번호를 - 없이 입력해주세요")
        String phoneNumber,
        Gender gender,
        @Past(message = "불가능한 생년월일입니다.")
        LocalDate birthDate
) {
    public UserUpdateRequest{
        name = name == null ? null : name.trim();
        nickname = nickname == null ? null : nickname.trim();
        phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }
}
