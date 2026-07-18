package com.acc.gazua.domain.accommodation.entity;

import com.acc.gazua.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


//필드 추가 필요
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Accommodation {

    @Id @GeneratedValue
    @Column(name = "accommodation_id")
    private Long id;

    @OneToOne
    private User host; //숙소 주인

    private String name; //숙소 이름

    @Embedded private Address address; // 숙소 주소
    @Embedded private CheckTime checkTime; //체크인,체크아웃 시간

    @Enumerated(EnumType.STRING) private AccommodationType type;

    private String thumbnailUrl;

    @Lob
    private String description; //숙소 소개 글

    @Column(precision = 2, scale = 1)
    private BigDecimal averageStar; //숙소 리뷰 평균 별점

    @Column(precision = 15,scale = 1)
    private BigDecimal totalStar; //모든 리뷰 별점 합

    private Integer reviewCount; //리뷰 수

    private Integer maxCapacity; // 숙소가 가진 방들 중 최대 인원 수
}
