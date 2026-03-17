package com.acc.gazua.domain.review.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; //리뷰 작성자

    @ManyToOne
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation; //리뷰 작성된 숙박시설

    private String sentence; //리뷰 내용

    private BigDecimal star; //평점

    //요청자 권한 체크
    public void checkAuthority(Long requestUser){
        if(!user.equalsId(requestUser)) throw new AccessDeniedException("권한이 존재하지 않습니다.");
    }

    //리뷰의 내용,평점 변경
    public void changeReview(String sentence,BigDecimal star){
        this.sentence = sentence;
        this.star = star;
    }


}
