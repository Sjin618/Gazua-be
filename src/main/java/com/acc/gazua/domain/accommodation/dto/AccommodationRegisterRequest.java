package com.acc.gazua.domain.accommodation.dto;

import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccommodationRegisterRequest(
        @NotNull(message = "호스트는 필수입니다.")
        Long hostId,

        @NotBlank(message = "숙소 이름은 필수입니다.")
        @Size(max = 100, message = "숙소 이름은 100자 이하여야 합니다.")
        String name, //숙소 이름

        @NotBlank(message = "숙소 주소는 필수입니다.")
        String address, //숙소 주소

        @NotNull(message = "숙소 유형은 필수입니다.")
        AccommodationType type, //숙소 타입

        @Pattern(regexp = "\\d{5}", message = "올바른 우편 번호 형식이 아닙니다.") String zipcode, //숙소 우편 번호

        @Size(max = 2000, message = "숙소 소개는 2000자 이하여야 합니다.") String description, //숙소 소개글

        @NotBlank(message = "숙소 전화번호는 필수입니다.") String contactNumber,

        @NotBlank(message = "지역 코드가 존재하지 않습니다.")
        @Pattern(regexp = "\\d{10}", message = "올바른 지역 코드 형식이 아닙니다.")
        String regionCode, // 우편 번호 서비스의 bcode, 지역 코드


        //환불 정책
        CancellationPolicyType cancellationPolicy // null -> 타입 별 기본 값
) {
}
