package com.acc.gazua.global.security.auth.service;


import com.acc.gazua.domain.User.entity.User;
import com.acc.gazua.domain.User.exception.UserException;
import com.acc.gazua.domain.User.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.auth.dto.LoginRequest;
import com.acc.gazua.global.security.auth.entity.RefreshToken;
import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.provider.JwtProvider;
import com.acc.gazua.global.security.provider.dto.RefreshTokenDto;
import com.acc.gazua.global.security.provider.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceRDBImpl implements AuthService{
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    //유저 로그인
    public TokenDto login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new UserException(ErrorCode.LOGIN_FAILED));

        if(!passwordEncoder.matches(loginRequest.password(), user.getPassword())){
            throw new UserException(ErrorCode.LOGIN_FAILED);
        }

        if(user.isDeleted()){
            throw new UserException(ErrorCode.DELETED_USER);
        }

        TokenDto tokenDto = jwtProvider.createTokenDto(user.getId(), user.getRole().getKey());

        RefreshToken refreshToken = RefreshToken.builder()
                .expiredAt(tokenDto.getExpiredAt())
                .userId(user.getId())
                .token(tokenDto.getRefreshToken())
                .build();

        refreshTokenRepository.save(refreshToken);

        return tokenDto;
    }

    //유저 로그아웃
    public void logout(String refreshToken){
        //RDB에서 refresh token 제거
        refreshTokenRepository.deleteByCurrentToken(refreshToken);
    }

    //access token 재발급
    public TokenDto reissue(String refreshToken){
        //토큰이 비어있다면 에러
        if(refreshToken == null){
            throw new JwtCustomException(ErrorCode.EMPTY_TOKEN);
        }
        //refresh token 검증
        jwtProvider.validateToken(refreshToken);

        RefreshToken oldToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new JwtCustomException(ErrorCode.INVALID_TOKEN));

        if(oldToken.isExpired()){
            throw new JwtCustomException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        //refresh token 재발급
        //access token 재발급
        TokenDto tokenDto = jwtProvider.createTokenDto(user.getId(), user.getRole().getKey());

        //refresh token 업데이트
        oldToken.updateToken(tokenDto.getRefreshToken(), tokenDto.getExpiredAt());

        return tokenDto;
    }
}
