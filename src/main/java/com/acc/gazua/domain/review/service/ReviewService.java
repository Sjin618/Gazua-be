package com.acc.gazua.domain.review.service;

import com.acc.gazua.domain.review.dto.ReviewCreateRequest;
import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.dto.ReviewUpdateRequest;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;


public interface ReviewService {

    void createReview(CustomUserDetails userDetails, ReviewCreateRequest request, Long accId);

    void deleteReview(CustomUserDetails userDetails, Long reviewId,Long accId);

    void updateReview(CustomUserDetails userDetails, ReviewUpdateRequest request, Long reviewId,Long accId);

    CursorPage<ReviewGetResponse> getReviewList(String sortBy,
                                                String cursorValue,
                                                int size,
                                                Long accId);
}
