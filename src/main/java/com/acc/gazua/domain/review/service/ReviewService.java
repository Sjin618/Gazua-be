package com.acc.gazua.domain.review.service;

import com.acc.gazua.domain.review.dto.ReviewCreateRequest;
import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.dto.ReviewUpdateRequest;
import com.acc.gazua.global.dto.CursorPage;


public interface ReviewService {

    void createReview(Long userId, ReviewCreateRequest request, Long accId);

    void deleteReview(Long userId, Long reviewId,Long accId);

    void updateReview(Long userId, ReviewUpdateRequest request, Long reviewId,Long accId);

    CursorPage<ReviewGetResponse> getReviewList(String sortBy,
                                                String cursorValue,
                                                int size,
                                                Long accId);
}
