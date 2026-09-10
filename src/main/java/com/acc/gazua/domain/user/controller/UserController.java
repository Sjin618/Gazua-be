package com.acc.gazua.domain.user.controller;


import com.acc.gazua.domain.like.dto.LikeAccommodationDto;
import com.acc.gazua.domain.like.service.LikeService;
import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.service.UserService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.util.CookieUtils;
import com.acc.gazua.global.security.details.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final LikeService likeService;
    private final CookieUtils cookieUtils;

    //회원 가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody UserSignUpRequest request) {
        userService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    //회원 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(HttpServletResponse response,
            @AuthenticationPrincipal CustomUserDetails user){
        userService.withdraw(user.getUserId());

        ResponseCookie cookie = cookieUtils.makeCookie("refreshToken","",0);

        response.addHeader("Set-Cookie",cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }

    //좋아요 한 숙소 조회
    @GetMapping("/me/likes")
    public ResponseEntity<ApiResponse<CursorPage<LikeAccommodationDto>>> getLikesAccommodation(@AuthenticationPrincipal CustomUserDetails user,
                                                                                               @RequestParam(required = false) String cursor,
                                                                                               @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(ApiResponse.success(likeService.getLikeAccommodation(user.getUserId(), cursor, size),
                "좋아요 숙소 조회에 성공하였습니다."));
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
