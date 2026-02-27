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
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
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
        userRepository.save(user);
    }

    public void withdraw(CustomUserDetails userDetails){
        Long userId = userDetails.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        user.delete();
        //유저와 관련된 모든 refresh token DB에서 삭제
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getMySummary(CustomUserDetails userDetails){
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        return UserSummaryResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getMyDetail(CustomUserDetails userDetails){
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        return UserDetailResponse.from(user);
    }

    public void updateMe(UserUpdateRequest userUpdateRequest,CustomUserDetails userDetails){

    }

}
