package com.acc.gazua.domain.review.service;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.domain.review.dto.ReviewCreateRequest;
import com.acc.gazua.domain.review.dto.ReviewGetResponse;
import com.acc.gazua.domain.review.dto.ReviewUpdateRequest;
import com.acc.gazua.domain.review.entity.Review;
import com.acc.gazua.domain.review.exception.ReviewException;
import com.acc.gazua.domain.review.repository.ReviewRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.PageHandler;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final AccommodationRepository accommodationRepository;
    private final List<PageHandler<ReviewGetResponse>> pageHandlers;

    @Override
    public void createReview(CustomUserDetails userDetails, ReviewCreateRequest request, Long accId) {
        Long reservationId = request.reservationId();
        Long userId = userDetails.getUserId();

        Reservation reservation = reservationRepository.findByIdWithUser(reservationId)
                .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_FOUNT));

        //예약자와 리뷰 작성자가 동일한지 검증
        //체크아웃 완료된 예약에만 리뷰 가능
        reservation.validateReservation(userId);

        //이미 작성된 리뷰가 있는지 검증
        if(!reviewRepository.existsByAccommodationIdAndUserId(accId,userId)){
            throw new ReviewException(ErrorCode.ALREADY_CREATE_REVIEW);
        }

        User user = userRepository.getReferenceById(userId);
        Accommodation accommodation = accommodationRepository.getReferenceById(accId);

        Review review = Review.builder()
                .accommodation(accommodation)
                .user(user)
                .sentence(request.sentence())
                .star(request.star())
                .build();

        reviewRepository.save(review);

        //숙소의 평점 업데이트
        accommodationRepository.updateStats(accId,1L,request.star());
    }

    @Override
    public void deleteReview(CustomUserDetails userDetails, Long reviewId, Long accId) {
        Long userId = userDetails.getUserId();

        //User 검증,숙소 평점 수정을 하기 때문에 연관 객체 바로 가져오도록 수정해야 함
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        //리뷰를 삭제할 권한이 있는지 확인(삭제 요청 사용자 == 리뷰 사용자)
        review.checkAuthority(userId);

        //숙소의 평점 수정
        accommodationRepository.updateStats(accId, -1L,review.getStar().negate());

        //리뷰 삭제
        reviewRepository.delete(review);
    }

    @Override
    public void updateReview(CustomUserDetails userDetails, ReviewUpdateRequest request, Long reviewId,Long accId) {
        Long userId = userDetails.getUserId();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow();

        //리뷰를 수정할 권한이 있는지 확인(수정 요청 사용자 == 리뷰 사용자)
        review.checkAuthority(userId);

        //숙소의 평점 수정
        accommodationRepository.updateStats(accId,0L,request.star().subtract(review.getStar()));

        //리뷰의 내용,평점 변경
        review.changeReview(request.sentence(),request.star());
    }

    public CursorPage<ReviewGetResponse> getReviewList(String sortBy,
                                                          String cursorValue,
                                                          int size,
                                                          Long accId) {
        //정렬 기준이 없을 시 에러
        if(sortBy == null && sortBy.isBlank()){
            throw new IllegalArgumentException("정렬 기준 입력은 필수입니다.");
        }

        int limit = size + 1;

        //정렬 기준에 따라 데이터베이스 조회
        PageHandler<ReviewGetResponse> pageHandler = pageHandlers.stream()
                .filter(p -> p.supports(sortBy))
                .findAny()
                .orElseThrow();

        List<ReviewGetResponse> list = pageHandler.fetch(accId, cursorValue, limit);

        //리스트가 비어있을 시 리뷰가 더이상 없다는 뜻
        //빈 리스트 반환
        if(list.isEmpty()){
            return new CursorPage<>(list, null, false);
        }

        boolean hasNext = list.size() > size;
        if(hasNext){
            list.remove(size);
        }

        String newCursorValue = pageHandler.createCursor(list.get(list.size() - 1));

        //커서 페이징 DTO에 넣어서 반환
        return new CursorPage<>(list, newCursorValue, hasNext);
    }

}
