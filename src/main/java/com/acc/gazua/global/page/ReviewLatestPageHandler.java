package com.acc.gazua.global.page;

import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewLatestPageHandler implements PageHandler<ReviewGetResponse> {
    private final ReviewRepository reviewRepository;

    @Override
    public boolean supports(String sortBy) {
        return "latest".equals(sortBy);
    }

    @Override
    public List<ReviewGetResponse> fetch(Object searchId, String cursor, int limit) {
        Long reviewId = null;

        if(cursor != null && !cursor.isBlank()){
            try{
                reviewId = Long.parseLong(cursor);
            }catch(Exception e){
                throw new IllegalArgumentException("유효하지 않은 커서 타입입니다.");
            }
        }

        Long accId = Long.parseLong(searchId.toString());

        return reviewRepository.findByLatestCursor(reviewId, accId, limit);
    }

    @Override
    public String createCursor(ReviewGetResponse lastItem) {
        return String.valueOf(lastItem.getReviewId());
    }
}
