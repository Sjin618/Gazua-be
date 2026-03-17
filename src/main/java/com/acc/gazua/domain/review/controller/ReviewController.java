package com.acc.gazua.domain.review.controller;

import com.acc.gazua.domain.review.dto.ReviewCreateRequest;
import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.dto.ReviewUpdateRequest;
import com.acc.gazua.domain.review.service.ReviewService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accommodation/{accId}/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    //리뷰 생성
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReview(@RequestBody ReviewCreateRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable("accId") Long accId) {
        reviewService.createReview(userDetails,request,accId);

        return ResponseEntity.ok(ApiResponse.success("리뷰 생성에 성공했습니다."));
    }

    //리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @PathVariable("reviewId") Long reviewId,
                             @PathVariable("accId")Long accId) {
        reviewService.deleteReview(userDetails,reviewId,accId);

        return ResponseEntity.ok(ApiResponse.success("리뷰 삭제에 성공했습니다."));
    }


    //리뷰 수정
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateReview(@RequestBody ReviewUpdateRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable("reviewId") Long reviewId,
                                                          @PathVariable("accId") Long accId) {
        reviewService.updateReview(userDetails, request, reviewId, accId);

        return ResponseEntity.ok(ApiResponse.success("리뷰 수정에 성공했습니다."));
    }

    //숙소의 리뷰 조회
    //비로그인 사용자 접근 가능
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<ReviewGetResponse>>> getReviewList(@RequestParam String sortBy,
                              @RequestParam(required = false) String cursorValue,
                              @RequestParam(defaultValue = "10") int size,
                              @PathVariable("accId") Long accId) {
        CursorPage<ReviewGetResponse> reviewList = reviewService.getReviewList(sortBy, cursorValue, size, accId);

        return ResponseEntity.ok(ApiResponse.success(reviewList, "리뷰 조회에 성공했습니다."));
    }
}
