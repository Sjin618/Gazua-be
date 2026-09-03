package com.acc.gazua.domain.review.service;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.exception.AccommodationException;
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
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.exception.BusinessException;
import com.acc.gazua.global.page.PageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService{
    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final AccommodationRepository accommodationRepository;
    private final List<PageHandler<ReviewGetResponse>> pageHandlers;

    @Override
    public void createReview(Long userId, ReviewCreateRequest request, Long accId) {
        Long reservationId = request.reservationId();

        Reservation reservation = reservationRepository.findByIdWithUserAndRoom(reservationId)
                .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_FOUND));

        //예약자와 리뷰 작성자가 동일한지 검증
        //체크아웃 완료된 예약에만 리뷰 가능
        reservation.validateReviewAuthority(userId);

        Long reservedAccId = reservation.getRoom().getAccommodation().getId();

        //review에 있는 숙소와 클라이언트에서 준 숙소의 ID가 일치하는지 확인
        if(!reservedAccId.equals(accId)){
            log.warn("리뷰 - 숙소 ID 불일치 - expected={}, actual={}", reservedAccId, accId);
            throw new ReviewException(ErrorCode.NOT_REVIEWABLE);
        }

        //이미 작성된 리뷰가 있는지 검증
        if(reviewRepository.existsByReservationId(reservationId)){
            throw new ReviewException(ErrorCode.ALREADY_CREATE_REVIEW);
        }

        Room room = reservation.getRoom();
        Accommodation accommodation = room.getAccommodation();

        Review review = Review.builder()
                .accommodation(accommodation)
                .reservation(reservation)
                .user(reservation.getUser())
                .sentence(request.sentence())
                .star(request.star())
                .build();

        reviewRepository.save(review);

        //숙소의 평점 업데이트
        if(accommodationRepository.updateStats(accommodation.getId(),1L,request.star()) < 1){
            throw new AccommodationException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }
    }

    @Override
    public void deleteReview(Long userId, Long reviewId, Long accId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        //review에 있는 숙소와 클라이언트에서 준 숙소의 ID가 일치하는지 확인
        if(!review.getAccommodation().getId().equals(accId)){
            throw new ReviewException(ErrorCode.REVIEW_NOT_FOUND);
        }

        //리뷰를 삭제할 권한이 있는지 확인(삭제 요청 사용자 == 리뷰 사용자)
        review.checkAuthority(userId);

        //숙소의 평점 수정
        if(accommodationRepository.updateStats(review.getAccommodation().getId(), -1L,review.getStar().negate()) < 1){
            throw new AccommodationException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        //리뷰 삭제
        reviewRepository.delete(review);
    }

    @Override
    public void updateReview(Long userId, ReviewUpdateRequest request, Long reviewId, Long accId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        //review에 있는 숙소와 클라이언트에서 준 숙소의 ID가 일치하는지 확인
        if(!review.getAccommodation().getId().equals(accId)){
            throw new ReviewException(ErrorCode.REVIEW_NOT_FOUND);
        }

        //리뷰를 수정할 권한이 있는지 확인(수정 요청 사용자 == 리뷰 사용자)
        review.checkAuthority(userId);

        //숙소의 평점 수정
        if (accommodationRepository.updateStats(review.getAccommodation().getId(), 0L, request.star().subtract(review.getStar())) < 1) {
            throw new AccommodationException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        //리뷰의 내용,평점 변경
        review.changeReview(request.sentence(), request.star());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewGetResponse> getReviewList(String sortBy,
                                                          String cursor,
                                                          int size,
                                                          Long accId) {
        int limit = size + 1;

        //정렬 기준에 따라 데이터베이스 조회
        PageHandler<ReviewGetResponse> pageHandler = pageHandlers.stream()
                .filter(p -> p.supports(sortBy))
                .findAny()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_SORT_TYPE));

        List<ReviewGetResponse> list = pageHandler.fetch(accId, cursor, limit);

        //리스트가 비어있을 시 리뷰가 더이상 없다는 뜻
        //빈 리스트 반환
        if(list.isEmpty()){
            return CursorPage.empty();
        }

        boolean hasNext = list.size() > size;
        if(hasNext){
            list.remove(size);
        }

        String newCursorValue = hasNext ? pageHandler.createCursor(list.get(list.size() - 1)) : null;

        //커서 페이징 DTO에 넣어서 반환
        return new CursorPage<>(list, newCursorValue, hasNext);
    }

}
