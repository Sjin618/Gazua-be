package com.acc.gazua.domain.like.service;

import com.acc.gazua.domain.like.dto.LikeAccommodationDto;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;

public interface LikeService {
    void createLike(Long accId, Long userId);

    void deleteLike(Long accId, Long userId);


    CursorPage<LikeAccommodationDto> getLikeAccommodation(Long userId, String cursor, int size);
}
