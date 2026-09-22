package com.acc.gazua.domain.accommodation.repository;

import com.acc.gazua.domain.accommodation.dto.AccommodationSearchDto;
import com.acc.gazua.domain.accommodation.dto.AccommodationSortType;
import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import com.acc.gazua.domain.accommodation.entity.QAccommodation;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccommodationQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;
    private final QAccommodation accommodation = QAccommodation.accommodation;

    public List<AccommodationSearchDto> search(String region,
                                               Integer personal,
                                               AccommodationSortType sortType,
                                               List<AccommodationType> types,
                                               String cursorValue,
                                               Long cursorId,
                                               int size) {
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
                        eqType(types),
                        cursorCondition(sortType, cursorValue, cursorId)
                )
                .orderBy(sortOrder(sortType), accommodation.id.desc())
                .limit(size)
                .fetch();
    }

    private BooleanExpression eqRegion(String region){
        return region != null ? accommodation.region.regionCode.eq(region) : null;
    }

    private BooleanExpression goePersonal(Integer personal){
        return personal != null ? accommodation.maxCapacity.goe(personal) : null;
    }

    private BooleanExpression eqType(List<AccommodationType> types) {
        return types == null || types.isEmpty() ? null : accommodation.type.in(types);
    }

    private OrderSpecifier<?> sortOrder(AccommodationSortType sortType){
        return switch (sortType) {
            case REVIEW -> accommodation.reviewCount.desc();
            case AVERAGESTAR -> accommodation.averageStar.desc();
        };
    }

    private BooleanExpression cursorCondition(AccommodationSortType sortType, String cursorValue, Long cursorId) {
        if (cursorValue == null || cursorId == null) {
            return null;
        }
        try {
            return switch (sortType) {
                case REVIEW -> descCursor(accommodation.reviewCount, Integer.parseInt(cursorValue), cursorId);
                case AVERAGESTAR -> descCursor(accommodation.averageStar, new BigDecimal(cursorValue), cursorId);
            };
        }catch (NumberFormatException e){
            throw new BusinessException(ErrorCode.INVALID_CURSOR, e);
        }
    }

    private <T extends Number & Comparable<?>> BooleanExpression descCursor(NumberPath<T> path, T value, Long cursorId) {
        return path.lt(value)
                .or(path.eq(value).and(accommodation.id.lt(cursorId)));
    }

}
