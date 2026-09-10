package com.acc.gazua.global.security.filter;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.exception.JwtErrorHandler;
import com.acc.gazua.global.security.provider.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    public static final String TOKEN_ERROR = "jwtTokenError";
    private final JwtProvider jwtProvider;

    //요청에서 jwt 토큰을 가지고 옴
    //jwt 토큰의 만료일,유효성 검증 (JwtProvider)
    //jwt 토큰에서 userID를 파싱해서 Authentication 반환 (CustomUserDetails,JwtProvider)
    //반환된 Authentication은 SecurityContextHolder에 저장
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveToken(request);

        if(StringUtils.hasText(accessToken)){
            try{
                Authentication authentication = jwtProvider.getAuthentication(accessToken);
                SecurityContextHolder
                        .getContextHolderStrategy()
                        .getContext()
                        .setAuthentication(authentication);
            }catch(JwtCustomException e){
                // 여기서 응답을 끊지 않는다.
                // 보호된 경로면 AuthorizationFilter 가 막고 EntryPoint 가 401 을 낸다.
                SecurityContextHolder.getContextHolderStrategy().clearContext();
                request.setAttribute(TOKEN_ERROR,e.getErrorCode()); //사유만 전달
            }catch(Exception e){
                log.error("토큰 처리 중 예상치 못한 오류", e);
                JwtErrorHandler.sendErrorResponse(response,ErrorCode.INTERNAL_SERVER_ERROR);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
