package com.acc.gazua.global.security.auth.service;


import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.auth.dto.LoginRequest;
import com.acc.gazua.global.security.auth.entity.RefreshToken;
import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.provider.JwtProvider;
import com.acc.gazua.global.security.provider.dto.TokenDto;
import com.acc.gazua.global.security.util.TokenHasher;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceRDBImpl implements AuthService{
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    //동시에 로그인 상태를 유지할 수 있는 기기 수
    private static final int MAX_DEVICES = 5;

    //사용 직후 이 시간 안에 같은 토큰이 또 오면 탈취가 아니라 동시 요청/재시도로 본다.
    private static final Duration REUSE_LEEWAY = Duration.ofSeconds(10);

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

        //기기 수 상한을 넘으면 가장 오래된 세션부터 정리
        evictOldestDevices(user.getId());

        TokenDto tokenDto = jwtProvider.createTokenDto(user.getId(), user.getRole().getKey());

        RefreshToken refreshToken = RefreshToken.builder()
                .expiredAt(tokenDto.getExpiredAt())
                .userId(user.getId())
                .tokenHash(TokenHasher.hash(tokenDto.getRefreshToken()))
                .familyId(UUID.randomUUID().toString())
                .build();

        refreshTokenRepository.save(refreshToken);

        return tokenDto;
    }

    //유저 로그아웃 - 해당 기기의 체인만 폐기한다.
    public void logout(String refreshToken){
        //RDB에서 refresh token 제거
        refreshTokenRepository.findByTokenHash(TokenHasher.hash(refreshToken))
                .ifPresent(token -> refreshTokenRepository.deleteByFamilyId(token.getFamilyId()));

    }

    /**
     * access token 재발급.
     *
     * 재사용을 감지하면 체인을 폐기한 뒤 예외를 던지는데, 기본 설정으로는 예외가 롤백을 일으켜
     * 폐기까지 취소된다. 그래서 JwtCustomException 은 롤백 대상에서 제외한다.
     * (REQUIRES_NEW 로 분리하면 위에서 잡은 비관적 락과 서로를 기다리다 교착 상태가 된다)
     */
    @Transactional(noRollbackFor = JwtCustomException.class)
    public TokenDto reissue(String refreshToken){
        //토큰이 비어있다면 에러
        if(refreshToken == null){
            throw new JwtCustomException(ErrorCode.EMPTY_TOKEN);
        }
        //refresh token 검증
        Claims claims = jwtProvider.parseClaims(refreshToken);
        if(!"refresh".equals(claims.get("type",String.class))){
            throw new JwtCustomException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = Long.parseLong(claims.getSubject());

        RefreshToken stored = refreshTokenRepository.findByTokenHashForUpdate(TokenHasher.hash(refreshToken))
                .orElseThrow(() -> new JwtCustomException(ErrorCode.INVALID_TOKEN));

        //DB에 저장된 토큰 userId 와 지금 받은 refresh Token userId가 동일한지 확인
        if (!stored.getUserId().equals(userId)) {
            throw new JwtCustomException(ErrorCode.INVALID_TOKEN);
        }

        // === 재사용 감지 ===
        if(stored.isUsed()){
            if (stored.isWithinLeeway(clock, REUSE_LEEWAY)) {
                //동시 요청이나 네트워크 재시도로 보고 통과시킨다.
                log.warn("유예 구간 내 중복 재발급 - userId={}, familyId={}", userId, stored.getFamilyId());
            }else{
                log.error("리프레시 토큰 재사용 감지 - userId={}, familyId={}", userId, stored.getFamilyId());
                refreshTokenRepository.deleteByFamilyId(stored.getFamilyId());
                throw new JwtCustomException(ErrorCode.TOKEN_REUSE_DETECTED);
            }
        }

        //refresh Token 의 유효기간이 지났는지 확인
        if(stored.isExpired(clock)){
            throw new JwtCustomException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        //탈퇴한 회원인지 확인
        if(user.isDeleted()){
            throw new UserException(ErrorCode.DELETED_USER);
        }

        //이전 토큰은 지우지 않고 사용됨으로 표시 - 이 흔적이 다음 재사용 감지의 근거가 된다.
        stored.markUsed(clock);

        //refresh token 재발급
        //access token 재발급
        TokenDto tokenDto = jwtProvider.createTokenDto(user.getId(), user.getRole().getKey());

        //같은 체인을 이어받는 새 토큰
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .familyId(stored.getFamilyId())
                .tokenHash(TokenHasher.hash(tokenDto.getRefreshToken()))
                .expiredAt(tokenDto.getExpiredAt())
                .build());

        return tokenDto;
    }

    //기기 수 상한 초과 시 오래된 체인부터 제거(이번 로그인 자리를 미리 비워둔다)
    private void evictOldestDevices(Long userId){
        List<String> activeFamilies = refreshTokenRepository.findActiveFamilyIds(userId, LocalDateTime.now(clock));

        //초과한 기기 수
        int excess = activeFamilies.size() - (MAX_DEVICES - 1);
        if (excess <= 0) {
            return;
        }

        //createdAt 오름차순으로 조회했기 때문에, 가장 오래된 기기부터 List 에 담김
        List<String> targets = activeFamilies.stream().limit(excess).toList();
        log.info("기기 수 상한 초과로 오래된 세션 정리 - userId={}, 제거={}건", userId, targets.size());
        refreshTokenRepository.deleteByFamilyIdIn(targets);
    }
}
