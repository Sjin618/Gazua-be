package com.acc.gazua.domain.user.service;

import com.acc.gazua.domain.user.dto.UserDetailResponse;
import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.dto.UserSummaryResponse;
import com.acc.gazua.domain.user.dto.UserUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    //회원가입
    void signup(UserSignUpRequest userSignUpRequest);

    //회원탈퇴
    void withdraw(Long userId);

    //내 정보 간소 조회
    UserSummaryResponse getMySummary(Long userId);
    //내 정보 상세 조회
    UserDetailResponse getMyDetail(Long userId);

    //내 정보 수정
    void updateMe(UserUpdateRequest userUpdateRequest, Long userId);

    void updateMyProfileImage(MultipartFile newImage, Long userId);

    void deleteMyProfileImage(Long userId);
}
