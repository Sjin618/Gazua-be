package com.acc.gazua.domain.like.service;

import com.acc.gazua.global.security.details.CustomUserDetails;

public interface LikeService {
    void createLike(Long accId, CustomUserDetails userDetails);

    void deleteLike(Long accId,CustomUserDetails userDetails);
}
