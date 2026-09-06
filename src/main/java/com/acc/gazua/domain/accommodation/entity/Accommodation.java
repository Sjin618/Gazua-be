package com.acc.gazua.domain.accommodation.entity;

import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.region.entity.Region;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodation_id")
    private Long id;

    @OneToOne
    private User host; //숙소 주인

    private String name; //숙소 이름

    private String address; // 숙소 주소

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region; //지역 코드

    @Enumerated(EnumType.STRING)
    private AccommodationType type;

    private String thumbnailUrl;

    @Lob
    private String description; //숙소 소개 글

    @Column(precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal averageStar = new BigDecimal("0.0"); //숙소 리뷰 평균 별점

    @Column(precision = 15, scale = 1)
    @Builder.Default
    private BigDecimal totalStar = new BigDecimal("0.0"); //모든 리뷰 별점 합

    @Builder.Default
    private Integer reviewCount = 0; //리뷰 수

    private Integer maxCapacity; // 숙소가 가진 방들 중 최대 인원 수

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private CancellationPolicyType cancellationPolicy = CancellationPolicyType.MODERATE; //숙소 환불 정책

    public void changeCancellationPolicy(CancellationPolicyType policy) {
        this.cancellationPolicy = policy;
    }
}
