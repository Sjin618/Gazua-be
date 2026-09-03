package com.acc.gazua.domain.review.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {
    @Id @GeneratedValue
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; //리뷰 작성자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation; //한 예약 당 하나의 예약

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation; //리뷰 작성된 숙박시설

    @Column(name = "sentence", length = 1000, nullable = false)
    private String sentence; //리뷰 내용

    @Column(precision = 2, scale = 1, nullable = false)
    private BigDecimal star; //평점

    //요청자 권한 체크
    public void checkAuthority(Long requestUserId){
        if(!user.getId().equals(requestUserId)) throw new AccessDeniedException("리뷰에 관한 권한이 존재하지 않습니다.");
    }

    //리뷰의 내용,평점 변경
    public void changeReview(String sentence,BigDecimal star){
        this.sentence = sentence;
        this.star = star;
    }


}
