package com.acc.gazua.domain.user.service;


import com.acc.gazua.domain.user.dto.UserDetailResponse;
import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.dto.UserSummaryResponse;
import com.acc.gazua.domain.user.dto.UserUpdateRequest;
import com.acc.gazua.domain.user.entity.Role;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Override
    public void signup(UserSignUpRequest request){
        //이메일 중복 체크
        if(userRepository.existsByEmail(request.email())){
            throw new UserException(ErrorCode.DUPLICATE_EMAIL);
        }
        //패스워드 인코딩
        String encodedPassword = passwordEncoder.encode(request.password());
        //회원 정보 설정
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .profileImage(null) //기본 이미지로 수정
                .gender(request.gender())
                .nickname(request.nickname())
                .role(Role.USER)
                .birthDate(request.birthDate())
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .build();

        //회원 저장
        //회원가입 중복 실행 시 에러 메시지 일관되게 반환
        //나중에 user에 unique 제약이 추가되면, 에러를 필드에 따라 상세하게 분리해야 함
        try {
            userRepository.save(user);
        }catch(DataIntegrityViolationException e){
            log.warn("회원가입 중복실행 감지");
            throw new UserException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Override
    public void withdraw(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if(user.isDeleted()){
            throw new UserException(ErrorCode.DELETED_USER);
        }

        user.delete(LocalDateTime.now(clock));
        //유저와 관련된 모든 refresh token DB에서 삭제
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getMySummary(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        return UserSummaryResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getMyDetail(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        return UserDetailResponse.from(user);
    }

    @Override
    public void updateMe(UserUpdateRequest userUpdateRequest,Long userId){

    }

}
