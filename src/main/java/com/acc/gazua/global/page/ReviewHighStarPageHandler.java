package com.acc.gazua.global.page;

import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.repository.ReviewRepository;
import com.acc.gazua.global.dto.CustomCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewHighStarPageHandler implements PageHandler<ReviewGetResponse>{
    private final ReviewRepository reviewRepository;
    private final JsonCursorUtils jsonCursorUtils;
    @Override
    public boolean supports(String sortBy) {
        return "highStar".equals(sortBy);
    }

    @Override
    public List<ReviewGetResponse> fetch(Long accId, String cursor, int limit) {
        BigDecimal star = null;
        Long reviewId = null;

        if(cursor != null && !cursor.isBlank()){
            CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
            star = new BigDecimal(customCursor.cursorValue());
            reviewId = customCursor.cursorId();
        }

        return reviewRepository.findByHighStarCursor(star, reviewId, limit, accId);
    }

    @Override
    public String createCursor(ReviewGetResponse lastItem) {
        return jsonCursorUtils.serialize(new CustomCursor(lastItem.getStar().toString(),lastItem.getReviewId()));
    }
}
