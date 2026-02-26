package com.acc.gazua.domain.review.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {
    @Id @GeneratedValue
    @Column(name = "review_id")
    private Long id;

    @ManyToOne
    private User user; //리뷰 작성자

    @ManyToOne
    private Accommodation accommodation; //리뷰 작성된 숙박시설

    private String sentence; //리뷰 내용

    private Integer star; //평점
}
