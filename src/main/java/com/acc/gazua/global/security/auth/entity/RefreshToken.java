package com.acc.gazua.global.security.auth.entity;

import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Builder
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "refresh_token",
        indexes = {
                @Index(name = "idx_refresh_token_family", columnList = "familyId"),
                @Index(name = "idx_refresh_token_user", columnList = "userId")

        })
public class RefreshToken extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    //로그인 1회 = family 1개. 토큰이 회전해도 그대로 이어받는다.
    @Column(nullable = false, updatable = false, length = 36)
    private String familyId;

    @Column(nullable = false, updatable = false, unique = true, length = 64)
    private String tokenHash; //refresh token의 원문이 아닌 Hash 값

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    //회전에 사용된 시각. null 이면 아직 사용되지 않은 토큰
    private LocalDateTime usedAt;

    public boolean isUsed(){
        return usedAt != null;
    }

    //회전에 썼음을 표시. 삭제하지 않고 흔적을 남겨야 재사용을 감지할 수 있다.
    public void markUsed(Clock clock){
        this.usedAt = LocalDateTime.now(clock);
    }

    //만료 기한이 현재 시간보다 이전이면 만료된 토큰
    public boolean isExpired(Clock clock) {
        return this.expiredAt.isBefore(LocalDateTime.now(clock));
    }

    //사용 직후 유예 구간인지 - 동시 요청/네트워크 재시도를 탈취와 구분하기 위함
    public boolean isWithinLeeway(Clock clock, Duration leeway){
        return usedAt != null && usedAt.isAfter(LocalDateTime.now(clock).minus(leeway));
    }

}
