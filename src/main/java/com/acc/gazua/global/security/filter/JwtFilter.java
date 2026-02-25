package com.acc.gazua.global.security.filter;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.exception.JwtCustomException;
import com.acc.gazua.global.security.exception.JwtErrorHandler;
import com.acc.gazua.global.security.provider.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private JwtProvider jwtProvider;

    //요청에서 jwt 토큰을 가지고 옴
    //jwt 토큰의 만료일,유효성 검증 (JwtProvider)
    //jwt 토큰에서 userID를 파싱해서 Authentication 반환 (CustomUserDetails,JwtProvider)
    //반환된 Authentication은 SecurityContextHolder에 저장
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try{
            //token 추출
            String accessToken = resolveToken(request);
            //token 검증
            if (StringUtils.hasText(accessToken) && jwtProvider.validateToken(accessToken)) {
                Authentication authentication = jwtProvider.getAuthentication(accessToken);
                SecurityContextHolder
                        .getContextHolderStrategy()
                        .getContext()
                        .setAuthentication(authentication);
            }
            log.info("필터 통과 accessToken: {}",accessToken);
            filterChain.doFilter(request, response);
        }catch(JwtCustomException e){
            JwtErrorHandler.sendErrorResponse(response, e.getErrorCode());
        }catch(Exception e){
            JwtErrorHandler.sendErrorResponse(response,ErrorCode.INTERNAL_SERVER_ERROR);
        }

    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }
}
