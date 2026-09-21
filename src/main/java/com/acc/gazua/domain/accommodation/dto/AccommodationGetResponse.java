package com.acc.gazua.domain.accommodation.dto;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;

import java.math.BigDecimal;
import java.util.List;

public record AccommodationGetResponse(
        //숙소 ID
        Long accommodationId,
        //숙소 이름
        String accommodationName,
        //숙소 사진들
        List<String> accommodationImages,
        //숙소 평균 별점
        BigDecimal averageStar,
        //숙소 리뷰 개수
        Integer reviewCount,
        //숙소 환불 규정
        CancellationPolicyResponse cancellationPolicy,
        //숙소 주소
        String accommodationAddress,
        //숙소 유형
        AccommodationType type,
        //숙소 소개글
        String description
) {
    public static AccommodationGetResponse from(Accommodation accommodation, List<String> images, CancellationPolicyType policy) {
        return new AccommodationGetResponse(
                accommodation.getId(),
                accommodation.getName(),
                images,
                accommodation.getAverageStar(),
                accommodation.getReviewCount(),
                CancellationPolicyResponse.from(policy),
                accommodation.getAddress(),
                accommodation.getType(),
                accommodation.getDescription()
        );
    }

}
