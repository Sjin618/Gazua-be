package com.acc.gazua.domain.accommodation.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {
    private String sido; // 시/도 (서울시, 강원도)
    private String sigungu; // 시/군/구 (가평군, 강남구)
    private String roadName; // 도로명 주소
}
