package com.acc.gazua.domain.User.service;


import com.acc.gazua.domain.User.dto.UserSignUpRequest;
import com.acc.gazua.domain.User.entity.Role;
import com.acc.gazua.domain.User.entity.User;
import com.acc.gazua.domain.User.exception.UserException;
import com.acc.gazua.domain.User.repository.UserRepository;
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
public class UserService {
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
                .image(null) //기본 이미지로 수정
                .gender(request.gender())
                .nickname(request.nickname())
                .role(Role.USER)
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

}
