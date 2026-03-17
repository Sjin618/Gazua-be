package com.acc.gazua.domain.accommodation.entity;

import com.acc.gazua.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


//필드 추가 필요
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Accommodation {

    @Id @GeneratedValue
    @Column(name = "accommodation_id")
    private Long id;

    @OneToOne
    private User host; //숙소 주인

    private String name; //숙소 이름

    @Lob
    private String description; //숙소 소개 글

    @Column(precision = 2, scale = 1)
    private BigDecimal averageStar; //숙소 리뷰 평균 별점

    @Column(precision = 15,scale = 1)
    private BigDecimal totalStar; //모든 리뷰 별점 합

    private Long reviewCount; //리뷰 수
}
