package com.acc.gazua.global.page;

import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.repository.ReviewRepository;
import com.acc.gazua.global.dto.CustomCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewLatestPageHandler implements PageHandler<ReviewGetResponse> {
    private final ReviewRepository reviewRepository;
    private final JsonCursorUtils jsonCursorUtils;

    @Override
    public boolean supports(String sortBy) {
        return "latest".equals(sortBy);
    }

    @Override
    public List<ReviewGetResponse> fetch(Long accId, String cursor, int limit) {
        Long reviewId = null;

        if(cursor != null && !cursor.isBlank()){
            reviewId = jsonCursorUtils.deserialize(cursor).cursorId();
        }
        return reviewRepository.findByLatestCursor(reviewId, accId, limit);
    }

    @Override
    public String createCursor(ReviewGetResponse lastItem) {
        return jsonCursorUtils.serialize(new CustomCursor(null,lastItem.getReviewId()));
    }

}
