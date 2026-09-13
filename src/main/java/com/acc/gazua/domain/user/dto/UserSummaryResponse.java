package com.acc.gazua.domain.user.dto;

import com.acc.gazua.domain.user.entity.User;

public record UserSummaryResponse(
        String profileImage,
        String nickname
) {
    public static UserSummaryResponse from(User user, String defaultProfileImage) {
        return new UserSummaryResponse(
                user.getProfileImage() != null ? user.getProfileImage() : defaultProfileImage,
                user.getNickname()
        );
    }

}
