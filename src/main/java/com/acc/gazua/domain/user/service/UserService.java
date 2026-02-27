package com.acc.gazua.domain.user.service;

import com.acc.gazua.domain.user.dto.UserDetailResponse;
import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.dto.UserSummaryResponse;
import com.acc.gazua.domain.user.dto.UserUpdateRequest;
import com.acc.gazua.global.security.details.CustomUserDetails;

public interface UserService {
    //회원가입
    void signup(UserSignUpRequest userSignUpRequest);

    //회원탈퇴
    void withdraw(CustomUserDetails userDetails);

    //내 정보 간소 조회
    UserSummaryResponse getMySummary(CustomUserDetails userDetails);
    //내 정보 상세 조회
    UserDetailResponse getMyDetail(CustomUserDetails userDetails);

    //내 정보 수정
    void updateMe(UserUpdateRequest userUpdateRequest,CustomUserDetails userDetails);
}
