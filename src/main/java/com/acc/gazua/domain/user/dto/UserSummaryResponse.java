package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.User;

public record UserSummaryResponse(
        String profileImage,
        String name
) {
    public static UserSummaryResponse from(User user){
        return new UserSummaryResponse(
                user.getProfileImage(),
                user.getName()
        );
    }
}
