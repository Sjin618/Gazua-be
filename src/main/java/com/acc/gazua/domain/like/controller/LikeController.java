package com.acc.gazua.domain.like.controller;

import com.acc.gazua.domain.like.service.LikeService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodation/{accId}/like")
public class LikeController {
    private final LikeService likeService;

    //좋아요 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createLike(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @PathVariable("accId")Long accId){
        likeService.createLike(accId, userDetails);

        return ResponseEntity.ok(ApiResponse.success("좋아요 생성을 성공하였습니다."));
    }

    //좋아요 제거
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteLike(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @PathVariable("accId")Long accId){
        likeService.deleteLike(accId,userDetails);

        return ResponseEntity.ok(ApiResponse.success("좋아요 취소를 성공하였습니다."));
    }

}
