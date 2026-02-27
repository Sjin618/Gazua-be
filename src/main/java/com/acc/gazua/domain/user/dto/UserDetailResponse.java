package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.Gender;
import com.acc.gazua.domain.user.entity.User;

import java.time.LocalDate;

public record UserDetailResponse(
        String nickname,
        String profileImage,
        String name,
        String phoneNumber,
        Gender gender,
        LocalDate birthDate
) {
    public static UserDetailResponse from(User user){
        return new UserDetailResponse(
                user.getNickname(),
                user.getProfileImage(),
                user.getName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getBirthDate()
        );
    }
}
