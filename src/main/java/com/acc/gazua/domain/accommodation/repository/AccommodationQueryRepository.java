package com.acc.gazua.domain.accommodation.repository;

import com.acc.gazua.domain.accommodation.dto.AccommodationSearchDto;
import com.acc.gazua.domain.accommodation.entity.QAccommodation;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccommodationQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QAccommodation accommodation = QAccommodation.accommodation;

    public List<AccommodationSearchDto> searchAccommodationSortByReviewDesc(String region,
                                                    Integer personal,
                                                    Integer cursorReviewCount,
                                                    Long cursorId,
                                                    int size){
        return jpaQueryFactory.select(Projections.constructor(AccommodationSearchDto.class,
                        accommodation.id,
                        accommodation.name,
                        accommodation.thumbnailUrl,
                        accommodation.averageStar,
                        accommodation.reviewCount))
                .from(accommodation)
                .where(
                        eqRegion(region),
                        goePersonal(personal),
                        reviewCountCursorCondition(cursorReviewCount, cursorId)
                )
                .orderBy(accommodation.reviewCount.desc(), accommodation.id.desc())
                .limit(size)
                .fetch();
    }

    public List<AccommodationSearchDto> searchAccommodationSortByAverageStarDesc(String region,
                                                                                 Integer personal,
                                                                                 BigDecimal cursorStar,
                                                                                 Long cursorId,
                                                                                 int size){
        return jpaQueryFactory.select(Projections.constructor(AccommodationSearchDto.class,
                        accommodation.id,
                        accommodation.name,
                        accommodation.thumbnailUrl,
                        accommodation.averageStar,
                        accommodation.reviewCount))
                .from(accommodation)
                .where(
                        eqRegion(region),
                        goePersonal(personal),
                        averageStarCursorCondition(cursorStar, cursorId)
                )
                .orderBy(accommodation.averageStar.desc(), accommodation.id.desc())
                .limit(size)
                .fetch();
    }

    private BooleanExpression eqRegion(String region){
        return region != null ? accommodation.region.regionCode.eq(region) : null;
    }

    private BooleanExpression goePersonal(Integer personal){
        return personal != null ? accommodation.maxCapacity.goe(personal) : null;
    }

    private BooleanExpression reviewCountCursorCondition(Integer cursorReviewCount,Long cursorId){
        if(cursorReviewCount == null || cursorId == null){
            return null;
        }
        return accommodation.reviewCount.lt(cursorReviewCount)
                .or(
                        accommodation.reviewCount.eq(cursorReviewCount)
                        .and(accommodation.id.lt(cursorId))
                );
    }

    private BooleanExpression averageStarCursorCondition(BigDecimal cursorStar,Long cursorId){
        if(cursorStar == null || cursorId == null){
            return null;
        }

        return accommodation.averageStar.lt(cursorStar)
                .or(
                        accommodation.averageStar.eq(cursorStar)
                        .and(accommodation.id.lt(cursorId)));
    }
}
