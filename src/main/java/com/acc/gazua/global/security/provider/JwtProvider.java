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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${JWT_SECRET}")
    private String salt;

    @Value("${JWT_EXPIRATION_ACCESS}")
    private long accessExpirationTime;

    @Value("${JWT_EXPIRATION_REFRESH}")
    private long refreshExpirationTime;

    private SecretKey secretKey;

    public static final String GRANT_TYPE = "Bearer";

    @PostConstruct
    protected void init(){
        // 설정된 salt 값을 바탕으로 HMAC-SHA 알고리즘에 적합한 SecretKey 생성
        this.secretKey = Keys.hmacShaKeyFor(salt.getBytes(StandardCharsets.UTF_8));
    }

    public TokenDto createTokenDto(Long userId,String role){
        String accessToken = createAccessToken(userId, role);
        RefreshTokenDto refreshToken = createRefreshToken(userId);
        return new TokenDto(GRANT_TYPE, accessToken, refreshToken.refreshToken(), refreshExpirationTime, refreshToken.expiredAt());
    }

    public String createAccessToken(Long userId,String role){
        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .add("role", role)
                .add("type","access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationTime))
                .build();
        return Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
    }

    public RefreshTokenDto createRefreshToken(Long userId){
        Date expiredDate = new Date(System.currentTimeMillis() + refreshExpirationTime);
        Claims claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .add("type","refresh")
                .issuedAt(new Date())
                .expiration(expiredDate)
                .build();
        String refreshToken = Jwts.builder()
                .claims(claims)
                .signWith(secretKey)
                .compact();
        return new RefreshTokenDto(refreshToken, expiredDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
    }
    

    public Claims parseClaims(String token){
        try{
            return Jwts.parser()
                    .verifyWith(secretKey)
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

        //권한 객체 생성
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        CustomUserDetails customUserDetails = new CustomUserDetails(userId, role);

        return new UsernamePasswordAuthenticationToken(customUserDetails, "", authorities);
    }
}
