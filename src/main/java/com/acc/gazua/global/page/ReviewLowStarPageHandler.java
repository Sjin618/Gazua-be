package com.acc.gazua.global.page;

import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewLowStarPageHandler implements PageHandler<ReviewGetResponse>{
    private final ReviewRepository reviewRepository;

    @Override
    public boolean supports(String sortBy) {
        return "lowStar".equals(sortBy);
    }

    @Override
    public List<ReviewGetResponse> fetch(Object searchId, String cursor, int limit) {
        BigDecimal star = null;
        Long reviewId = null;

        if(cursor != null && !cursor.isBlank()){
            try{
                String[] parts = cursor.split("_");
                if (parts.length >= 2) {
                    star = new BigDecimal(parts[0]);
                    reviewId = Long.parseLong(parts[1]);
                }
            }catch(Exception e){
                throw new IllegalArgumentException("유효하지 않은 커서 타입입니다.");
            }
        }

        Long accId = Long.parseLong(searchId.toString());
        return reviewRepository.findByLowStarCursor(star, reviewId, limit, accId);
    }

    @Override
    public String createCursor(ReviewGetResponse lastItem) {
        return lastItem.getStar() + "_" + lastItem.getReviewId();
    }
}
