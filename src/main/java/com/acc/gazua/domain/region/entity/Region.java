package com.acc.gazua.domain.region.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Region {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", length = 10, nullable = false, unique = true)
    private String regionCode; //전체 지역 코드 (시도 + 시군구 + 읍면동 + 리 코드)

    @Column(name = "sido_code", length = 2, nullable = false)
    private String sidoCode; //시도 코드

    @Column(name = "sigungu_code", length = 3,nullable = false)
    private String sigunguCode; // 시군구 코드

    @Column(name = "umd_code", length = 3, nullable = false)
    private String umdCode; //읍면동 코드

    @Column(name = "ri_code", length = 2, nullable = false)
    private String riCode; //리 코드

    private String regionName;
}
