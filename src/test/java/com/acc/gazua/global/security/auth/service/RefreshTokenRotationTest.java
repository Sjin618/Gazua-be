package com.acc.gazua.global.security.auth.service;

import com.acc.gazua.domain.user.entity.Gender;
import com.acc.gazua.domain.user.entity.Role;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.auth.dto.LoginRequest;
import com.acc.gazua.global.security.auth.entity.RefreshToken;
import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.provider.dto.TokenDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@DisplayName("리프레시 토큰 회전 / 재사용 감지")
class RefreshTokenRotationTest {
    private static final String EMAIL = "rotation-test@example.com";
    private static final String RAW_PASSWORD = "password1234!";

    @MockitoBean
    Clock clock;

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Instant now;

    @BeforeEach
    void setUp(){
        now = Instant.now();
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
        given(clock.instant()).willAnswer(invocation -> now); // now 를 바꾸면 시계가 움직인다.

        refreshTokenRepository.deleteAll();
        if (userRepository.findByEmail(EMAIL).isEmpty()) {
            userRepository.save(User.builder()
                    .email(EMAIL)
                    .password(passwordEncoder.encode(RAW_PASSWORD))
                    .gender(Gender.MALE)
                    .nickname("회전테스터")
                    .role(Role.USER)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .name("홍길동")
                    .phoneNumber("01012345678")
                    .build());
        }
    }

    private void advance(Duration duration){
        now = now.plus(duration);
    }

    private TokenDto login(){
        return authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));
    }

    private long countFamilies() {
        return refreshTokenRepository.findAll().stream()
                .map(RefreshToken::getFamilyId).distinct().count();
    }

    @Test
    @DisplayName("회전된 토큰을 유예 시간 이후 재사용하면 체인 전체가 폐기된다.")
    void reuse_revokesWholeFamily() {
        String rt1 = login().getRefreshToken();
        String rt2 = authService.reissue(rt1).getRefreshToken();

        assertThat(refreshTokenRepository.count())
                .as("사용된 RT1의 흔적이 남아야 감지가 가능하다")
                .isEqualTo(2);

        advance(Duration.ofSeconds(30)); // 유예 구간(10초) 밖으로

        assertThatThrownBy(() -> authService.reissue(rt1))
                .isInstanceOf(JwtCustomException.class)
                .extracting(e -> ((JwtCustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_REUSE_DETECTED);

        assertThat(refreshTokenRepository.count())
                .as("탈취가 의심되면 체인 전체를 지운다.")
                .isZero();

        assertThatThrownBy(() -> authService.reissue(rt2))
                .as("정상 사용자가 둘고 있던 최신 토큰도 함께 무효화된다.")
                .isInstanceOf(JwtCustomException.class);
    }

    @Test
    @DisplayName("기기 수 상한을 넘으면 가장 오래된 세션부터 정리된다.")
    void deviceCap_evictsOldestFamily() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tokens.add(login().getRefreshToken());
            advance(Duration.ofSeconds(1)); // createdAt 을 벌려 정렬 순서를 확정한다.
        }
        assertThat(countFamilies()).isEqualTo(5);

        login(); // 6번째 기기

        assertThat(countFamilies()).as("상한이 유지되어야 한다.").isEqualTo(5);

        assertThatThrownBy(() -> authService.reissue(tokens.get(0)))
                .as("가장 오래된 기기가 정리된다.")
                .isInstanceOf(JwtCustomException.class);

        assertThatCode(() -> authService.reissue(tokens.get(1)))
                .as("나머지 기기는 살아있어야 한다.")
                .doesNotThrowAnyException();
    }
}