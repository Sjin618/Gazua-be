package com.acc.gazua.domain.user.controller;

import com.acc.gazua.domain.like.service.LikeService;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.service.UserService;
import com.acc.gazua.global.config.SecurityConfig;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.security.exception.JwtAccessDeniedHandler;
import com.acc.gazua.global.security.exception.JwtAuthenticationEntryPoint;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.provider.JwtProvider;
import com.acc.gazua.global.security.util.CookieUtils;
import com.acc.gazua.global.sse.SseEmitterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DELETE /api/users/withdraw 컨트롤러 슬라이스 테스트.
 *
 * 요청 본문이 없는 API 라 검증 대신 아래에 집중한다.
 *  - 인증 : 토큰이 없거나 잘못되면 막히는지, 에러 사유가 응답 코드로 제대로 전달되는지
 *  - 위임 : 토큰(principal)의 userId 가 서비스로 전달되는지
 *  - 응답 : refreshToken 삭제 쿠키의 속성
 *
 * JwtProvider 가 목이라 실제 토큰을 만들 수 없으므로, 인증은 spring-security-test 의 user() 로 주입한다.
 * Authorization 헤더가 없으면 JwtFilter 는 SecurityContext 를 건드리지 않으므로 주입한 인증이 그대로 쓰인다.
 * 토큰 오류 경로를 볼 때만 헤더를 보내고 jwtProvider 가 예외를 던지도록 스텁한다.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, CookieUtils.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.cors.allowed-origins=http://localhost:3000",
        "app.cookie.secure=true",
        "app.cookie.same-site=Lax"})
@DisplayName("회원탈퇴 API")
public class UserWithdrawControllerTest {
    private static final String URL = "/api/users/withdraw";

    // 1L 같은 기본값과 헷갈리지 않도록 눈에 띄는 값을 쓴다
    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    // UserController 가 생성자로 요구하는 나머지 협력 객체들
    @MockitoBean
    LikeService likeService;

    // SecurityConfig 가 JwtFilter 를 만들 때 필요
    @MockitoBean
    JwtProvider jwtProvider;


    @MockitoBean
    SseEmitterRegistry sseEmitterRegistry;

    private static CustomUserDetails loginUser() {
        return new CustomUserDetails(USER_ID, "ROLE_USER");
    }

    /**
     * refreshToken 을 지우는 쿠키가 내려갔는지 확인한다.
     * 로그인 때 심은 쿠키와 name / path / secure / sameSite 가 같아야 브라우저가 실제로 지운다.
     * andExpectAll 로 넘기면 하나가 실패해도 나머지까지 검사해서 틀린 속성을 한 번에 보여준다.
     */
    private static ResultMatcher[] refreshTokenCookieExpired() {
        return new ResultMatcher[]{
                cookie().exists("refreshToken"),
                cookie().value("refreshToken", ""),
                cookie().maxAge("refreshToken", 0),
                cookie().path("refreshToken", "/api/auth"),
                cookie().httpOnly("refreshToken", true),
                cookie().secure("refreshToken", true),
                cookie().sameSite("refreshToken", "Lax")};
    }

    @Nested
    @DisplayName("정상 요청")
    class Success {

        @Test
        @DisplayName("200 과 성공 메시지를 반환한다")
        void returns200() throws Exception {
            mockMvc.perform(delete(URL).with(user(loginUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("토큰의 userId 로 서비스를 호출한다")
        void delegatesWithPrincipalUserId() throws Exception {
            mockMvc.perform(delete(URL).with(user(loginUser())))
                    .andExpect(status().isOk());

            then(userService).should().withdraw(USER_ID);
        }

        @Test
        @DisplayName("요청에 다른 userId 를 실어 보내도 무시하고 토큰의 userId 를 쓴다")
        void ignoresUserIdFromRequest() throws Exception {
            // 남의 계정을 탈퇴시킬 수 없어야 한다
            mockMvc.perform(delete(URL)
                            .param("userId", "999")
                            .with(user(loginUser())))
                    .andExpect(status().isOk());

            then(userService).should().withdraw(USER_ID);
            then(userService).should(never()).withdraw(999L);
        }

        @Test
        @DisplayName("refreshToken 을 지우는 쿠키를 내려준다")
        void expiresRefreshTokenCookie() throws Exception {
            mockMvc.perform(delete(URL).with(user(loginUser())))
                    .andExpect(status().isOk())
                    .andExpectAll(refreshTokenCookieExpired());
        }
    }

    @Nested
    @DisplayName("인증")
    class Authentication {

        @Test
        @DisplayName("토큰이 없으면 401 을 반환하고 서비스를 호출하지 않는다")
        void noToken() throws Exception {
            mockMvc.perform(delete(URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.EMPTY_TOKEN.getCode()))
                    .andExpect(cookie().doesNotExist("refreshToken"));

            then(userService).should(never()).withdraw(anyLong());
        }

        @ParameterizedTest(name = "{0} 이면 해당 코드로 401 을 반환한다")
        @EnumSource(value = ErrorCode.class, names = {"INVALID_TOKEN", "EXPIRED_TOKEN", "UNSUPPORTED_TOKEN"})
        @DisplayName("토큰 검증에 실패하면 실패 사유가 응답 코드로 전달된다")
        void invalidToken(ErrorCode tokenError) throws Exception {
            // JwtFilter 가 사유를 request attribute 에 담고, EntryPoint 가 그 사유로 응답해야 한다.
            // 연결이 끊기면 모든 토큰 오류가 EMPTY_TOKEN 으로 뭉개진다.
            given(jwtProvider.getAuthentication("bad-token"))
                    .willThrow(new JwtCustomException(tokenError));

            mockMvc.perform(delete(URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(tokenError.getCode()));

            then(userService).should(never()).withdraw(anyLong());
        }
    }

    @Nested
    @DisplayName("비즈니스 예외")
    class Business {

        // 쿠키를 서비스 호출보다 먼저 내리므로, 서비스가 실패해도 브라우저의 refreshToken 은 지워져야 한다.
        // 에러 응답으로 바뀌는 과정에서 앞서 추가한 Set-Cookie 헤더가 사라지지 않는지까지 확인한다.

        @Test
        @DisplayName("이미 탈퇴한 회원이면 400 을 반환하고, 쿠키는 지운다")
        void deletedUser() throws Exception {
            willThrow(new UserException(ErrorCode.DELETED_USER))
                    .given(userService).withdraw(USER_ID);

            mockMvc.perform(delete(URL).with(user(loginUser())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DELETED_USER.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.DELETED_USER.getMessage()))
                    .andExpectAll(refreshTokenCookieExpired());
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 404 를 반환하고, 쿠키는 지운다")
        void userNotFound() throws Exception {
            willThrow(new UserException(ErrorCode.USER_NOT_FOUND))
                    .given(userService).withdraw(USER_ID);

            mockMvc.perform(delete(URL).with(user(loginUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                    .andExpectAll(refreshTokenCookieExpired());
        }
    }

    @Nested
    @DisplayName("HTTP 규약")
    class Protocol {

        @Test
        @DisplayName("인증된 상태에서 POST 로 호출하면 405 를 반환한다")
        void methodNotAllowed() throws Exception {
            // 인증 없이 호출하면 anyRequest().authenticated() 에 먼저 걸려 401 이 나오므로 인증을 넣는다
            mockMvc.perform(post(URL).with(user(loginUser())))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.getCode()));

            then(userService).should(never()).withdraw(anyLong());
        }
    }
}
