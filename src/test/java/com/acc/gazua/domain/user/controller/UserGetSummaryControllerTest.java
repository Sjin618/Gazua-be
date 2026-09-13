package com.acc.gazua.domain.user.controller;

import com.acc.gazua.domain.like.service.LikeService;
import com.acc.gazua.domain.user.dto.UserSummaryResponse;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.service.UserService;
import com.acc.gazua.global.config.SecurityConfig;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.details.CustomUserDetails;
import com.acc.gazua.global.security.exception.JwtAccessDeniedHandler;
import com.acc.gazua.global.security.exception.JwtAuthenticationEntryPoint;
import com.acc.gazua.global.security.provider.JwtProvider;
import com.acc.gazua.global.security.util.CookieUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
@DisplayName("내 정보 간소 조회 API")
public class UserGetSummaryControllerTest {

    private static final String URL = "/api/users/me/summary";

    // 1L 같은 기본값과 헷갈리지 않도록 눈에 띄는 값을 쓴다
    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    LikeService likeService;

    @MockitoBean
    CookieUtils cookieUtils;

    // SecurityConfig 가 JwtFilter 를 만들 때 필요
    @MockitoBean
    JwtProvider jwtProvider;

    private static CustomUserDetails loginUser(){
        return new CustomUserDetails(USER_ID,"ROLE_USER");
    }

    @Nested
    @DisplayName("정상 요청")
    class Success{

        @Test
        @DisplayName("200 과 요약 정보를 반환한다")
        void returns200() throws Exception {
            given(userService.getMySummary(USER_ID))
                    .willReturn(new UserSummaryResponse("https://cdn.example.com/users/42.png", "길동이"));

            mockMvc.perform(get(URL).with(user(loginUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.profileImage").value("https://cdn.example.com/users/42.png"))
                    .andExpect(jsonPath("$.data.nickname").value("길동이"));
        }

        @Test
        @DisplayName("토큰의 userId 로 서비스를 호출한다")
        void delegatesWithPrincipalUserId() throws Exception {
            mockMvc.perform(get(URL).with(user(loginUser())))
                    .andExpect(status().isOk());

            then(userService).should().getMySummary(USER_ID);
        }

        @Test
        @DisplayName("요청에 다른 userId 를 실어 보내도 무시하고 토큰의 userId 를 쓴다")
        void ignoresUserIdFromRequest() throws Exception {
            // 남의 계정으로 정보를 조회할 수 없어야 한다
            mockMvc.perform(get(URL)
                            .param("userId", "999")
                            .with(user(loginUser())))
                    .andExpect(status().isOk());

            then(userService).should().getMySummary(USER_ID);
            then(userService).should(never()).getMySummary(999L);
        }
    }

    @Nested
    @DisplayName("인증")
    class Authentication{

        @Test
        @DisplayName("토큰이 없으면 401을 반환하고 서비스를 호출하지 않는다.")
        void noToken() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.EMPTY_TOKEN.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.EMPTY_TOKEN.getMessage()));

            then(userService).should(never()).getMySummary(anyLong());
        }
    }

    @Nested
    @DisplayName("비즈니스 예외")
    class Business{

        @Test
        @DisplayName("이미 탈퇴한 회원이면 400 을 반환한다")
        void deletedUser() throws Exception {
            willThrow(new UserException(ErrorCode.DELETED_USER))
                    .given(userService).getMySummary(USER_ID);

            mockMvc.perform(get(URL).with(user(loginUser())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.DELETED_USER.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.DELETED_USER.getMessage()));
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 404를 반환한다")
        void userNotFound() throws Exception {
            willThrow(new UserException(ErrorCode.USER_NOT_FOUND))
                    .given(userService).getMySummary(USER_ID);

            mockMvc.perform(get(URL).with(user(loginUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("HTTP 규약")
    class Protocol{

        @Test
        @DisplayName("인증된 상태에서 POST 로 호출하면 405를 반환한다")
        void methodNotAllowed() throws Exception {
            // 인증 없이 호출하면 anyRequest().authenticated() 에 먼저 걸려 401 이 나오므로 인증을 넣는다
            mockMvc.perform(post(URL).with(user(loginUser())))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.getCode()));

            then(userService).should(never()).getMySummary(anyLong());
        }
    }
}
