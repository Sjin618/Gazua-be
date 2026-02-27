package com.acc.gazua.domain.user.controller;


import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.service.UserServiceImpl;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.security.details.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserServiceImpl userServiceImpl;

    //회원 가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody UserSignUpRequest request) {
        userServiceImpl.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    //회원 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails user){
        userServiceImpl.withdraw(user);

        ResponseCookie cookie = ResponseCookie.from("refreshToken","")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie",cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }

    //내 정보 간소 조회
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<Void>> getMySummary(){

        return ResponseEntity.ok(ApiResponse.success("d"));
    }

    //내 정보 상세 조회
    @GetMapping("/me/detail")
    public ResponseEntity<ApiResponse<Void>> getMyDetail(){

        return ResponseEntity.ok(ApiResponse.success("d"));
    }

    //내 정보 수정
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(){

        return ResponseEntity.ok(ApiResponse.success("정보 수정이 완료되었습니다."));
    }
}
