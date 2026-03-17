package com.acc.gazua.domain.review.repository;

import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review,Long> {

    @Query("SELECT COUNT(r) > 0 FROM review r WHERE r.accommodation.id = :accId and r.user.id = :userId")
    boolean existsByAccommodationIdAndUserId(@Param("accId") Long accId,@Param("userId") Long userId);

    //리뷰 평점 낮은 순
    @Query(value = """
            SELECT u.nickname,u.profile_Image as profileImage,r.star,r.sentence,r.review_id as reviewId
            FROM Review r JOIN User u on r.user_id = u.user_id
            WHERE ((:star IS NULL AND :id IS NULL)
            OR (r.star, r.review_id) > (:star, :id))
            AND r.accommodation_id = :accId
            ORDER BY r.star ASC, r.review_id ASC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<ReviewGetResponse> findByLowStarCursor(@Param("star") BigDecimal star,
                                                @Param("id") Long id,
                                                @Param("limit") int limit,
                                                @Param("accId") Long accId);

    //리뷰 평점 높은 순
    @Query(value = """
            SELECT u.nickname,u.profile_Image as profileImage,r.star,r.sentence,r.review_id as reviewId
            FROM Review r JOIN User u on r.user_id = u.user_id
            WHERE ((:star IS NULL AND :id IS NULL)
            OR (r.star, r.review_id) < (:star, :id))
            AND r.accommodation_id = :accId
            ORDER BY r.star DESC, r.review_id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<ReviewGetResponse> findByHighStarCursor(@Param("star") BigDecimal star,
                                                 @Param("id") Long id,
                                                 @Param("limit") int limit,
                                                 @Param("accId") Long accId);

    @Query(value = """
            SELECT u.nickname,u.profile_Image as profileImage,r.star,r.sentence,r.review_id as reviewId
            FROM Review r JOIN User u on r.user_id = u.user_id
            WHERE ((:id IS NULL)
            OR (:id > r.review_id))
            AND r.accommodation_id = :accId
            ORDER BY r.review_id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<ReviewGetResponse> findByLatestCursor(@Param("id") Long id,
                                               @Param("accId") Long accId,
                                               @Param("limit") int limit);
}
