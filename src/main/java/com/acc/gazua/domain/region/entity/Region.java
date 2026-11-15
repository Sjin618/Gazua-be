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

    private static final String SIDO_SUFFIX = "00000000"; //시군구 + 읍면동 + 리
    private static final String SIGUNGU_SUFFIX = "00000"; //읍면동 + 리

    public static String sigunguCodeOf(String sidoCode, String sigunguCode) {
        return sidoCode + sigunguCode + SIGUNGU_SUFFIX; // "27" + "170" -> "2717000000"
    }

    public static String sidoCodeOf(String sidoCode) {
        return sidoCode + SIDO_SUFFIX; // "27" -> "2700000000"
    }

    // 다음 우편번호 bcode(10자리 법정동) -> 시군구 코드
    public static String sigunguCodeFromLegalDong(String bcode) {
        return bcode.substring(0,5) + SIGUNGU_SUFFIX; // "2717010900" -> "2717000000"
    }
}
