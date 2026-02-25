package com.acc.gazua.global.security.auth.entity;

import com.acc.gazua.global.base.BaseTimeEntity;
import com.acc.gazua.global.security.provider.dto.RefreshTokenDto;
import com.acc.gazua.global.security.provider.dto.TokenDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@Entity
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String token;

    private LocalDateTime expiredAt;


    //refresh token을 갱신해줌
    public void updateToken(String token,LocalDateTime expiredAt){
        this.token = token;
        this.expiredAt = expiredAt;
    }

    //만료 기한이 현재 시간보다 이전이면 만료된 토큰
    public boolean isExpired(){
        return this.expiredAt.isBefore(LocalDateTime.now());
    }

}
