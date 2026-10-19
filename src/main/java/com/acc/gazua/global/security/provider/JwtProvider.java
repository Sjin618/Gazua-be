package com.acc.gazua.global.security.provider;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.provider.dto.RefreshTokenDto;
import com.acc.gazua.global.security.provider.dto.TokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    private final String salt;

    private final long accessExpirationTime;

    private final long refreshExpirationTime;

    private final Clock clock;

    private SecretKey secretKey;

    private static final int MIN_SECRET_BYTES = 32;

    public static final String GRANT_TYPE = "Bearer";

    public JwtProvider(Clock clock,
                       @Value("${JWT_SECRET}") String salt,
                       @Value("${JWT_EXPIRATION_ACCESS}") long accessExpirationTime,
                       @Value("${JWT_EXPIRATION_REFRESH}") long refreshExpirationTime) {

        this.salt = salt;
        this.accessExpirationTime = accessExpirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
        this.clock = clock;
    }


    //HS256 최소 키 길이 (256bit)

    @PostConstruct
    protected void init(){
        byte[] keyBytes = salt.getBytes(StandardCharsets.UTF_8);

        if(keyBytes.length < MIN_SECRET_BYTES){
            throw new IllegalStateException("JWT_SECRET 은 최소 %d바이트여야 합니다. 현재 %d바이트입니다. 'openssl rand -base64 48' 로 재발급하세요."
                    .formatted(MIN_SECRET_BYTES, keyBytes.length));
        }

        // 설정된 salt 값을 바탕으로 HMAC-SHA 알고리즘에 적합한 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenDto createTokenDto(Long userId,String role){
        String accessToken = createAccessToken(userId, role);
        RefreshTokenDto refreshToken = createRefreshToken(userId);
        return new TokenDto(GRANT_TYPE, accessToken, refreshToken.refreshToken(), refreshExpirationTime, refreshToken.expiredAt());
    }

    public String createAccessToken(Long userId,String role){
        Instant now = clock.instant();
        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .add("role", role)
                .add("type","access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationTime)))
                .build();
        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public RefreshTokenDto createRefreshToken(Long userId){
        Instant now = clock.instant();
        Instant expiresAt = now.plusMillis(refreshExpirationTime);
        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .add("type","refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .build();
        String refreshToken = Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
        return new RefreshTokenDto(refreshToken, LocalDateTime.ofInstant(expiresAt,clock.getZone()));
    }
    

    public Claims parseClaims(String token){
        try{
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }catch (SecurityException | MalformedJwtException e){
            throw new JwtCustomException(ErrorCode.INVALID_TOKEN);
        }catch (ExpiredJwtException e){
            throw new JwtCustomException(ErrorCode.EXPIRED_TOKEN);
        }catch (UnsupportedJwtException e){
            throw new JwtCustomException(ErrorCode.UNSUPPORTED_TOKEN);
        }catch (IllegalArgumentException e){
            throw new JwtCustomException(ErrorCode.EMPTY_TOKEN);
        }

    }

    public Authentication getAuthentication(String token) {
        //토큰 복호화 및 Claims 추출
        Claims claims = parseClaims(token);

        //type이 access 인 토큰만 통과 가능
        if(!"access".equals(claims.get("type", String.class))) {
            throw new JwtCustomException(ErrorCode.INVALID_TOKEN);
        }

        //Claims 에서 데이터 추출
        Long userId = Long.parseLong(claims.getSubject());
        log.debug("인증 완료 userId={}",userId);
        String role = claims.get("role", String.class);

        //역할이 없으면 유효하지 않은 토큰
        if(role == null){
            throw new JwtCustomException(ErrorCode.INVALID_TOKEN);
        }

        //권한 객체 생성
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        CustomUserDetails customUserDetails = new CustomUserDetails(userId, role);

        return new UsernamePasswordAuthenticationToken(customUserDetails, "", authorities);
    }
}
