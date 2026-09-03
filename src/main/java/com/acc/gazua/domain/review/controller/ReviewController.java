package com.acc.gazua.domain.review.controller;

import com.acc.gazua.domain.review.dto.ReviewCreateRequest;
import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.dto.ReviewUpdateRequest;
import com.acc.gazua.domain.review.service.ReviewService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    public ResponseEntity<ApiResponse<Void>> createReview(@Valid @RequestBody ReviewCreateRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails user,
                                                          @PathVariable("accId") Long accId) {
        reviewService.createReview(user.getUserId(),request,accId);

        return ResponseEntity.ok(ApiResponse.success("리뷰 생성에 성공했습니다."));
    }

    //리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@AuthenticationPrincipal CustomUserDetails user,
                             @PathVariable("reviewId") Long reviewId,
                             @PathVariable("accId")Long accId) {
        reviewService.deleteReview(user.getUserId(),reviewId,accId);

        return ResponseEntity.ok(ApiResponse.success("리뷰 삭제에 성공했습니다."));
    }


    //리뷰 수정
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateReview(@Valid @RequestBody ReviewUpdateRequest request,
                                                          @AuthenticationPrincipal CustomUserDetails user,
                                                          @PathVariable("reviewId") Long reviewId,
                                                          @PathVariable("accId") Long accId) {
        reviewService.updateReview(user.getUserId(), request, reviewId, accId);

        return ResponseEntity.ok(ApiResponse.success("리뷰 수정에 성공했습니다."));
    }

    //숙소의 리뷰 조회
    //비로그인 사용자 접근 가능
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<ReviewGetResponse>>> getReviewList(@RequestParam String sortBy,
                                                                                    @RequestParam(required = false) String cursor, //base64 인코딩 필요
                                                                                    @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.") @Max(value = 20, message = "조회 개수는 20 이하이어야 합니다.") @RequestParam(defaultValue = "10") int size,
                                                                                    @PathVariable("accId") Long accId) {

        CursorPage<ReviewGetResponse> reviewList = reviewService.getReviewList(sortBy, cursor, size, accId);

        return ResponseEntity.ok(ApiResponse.success(reviewList, "리뷰 조회에 성공했습니다."));
    }
}
