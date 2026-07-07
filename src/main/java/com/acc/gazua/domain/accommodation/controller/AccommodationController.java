package com.acc.gazua.domain.accommodation.controller;

import com.acc.gazua.domain.accommodation.dto.AccommodationRegisterRequest;
import com.acc.gazua.domain.accommodation.service.AccommodationService;
import com.acc.gazua.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodation")
public class AccommodationController {
    private final AccommodationService accommodationService;

    //숙소 조회(페이징)
    //지역 필터링
    //인원 수, 기간 필터링
    public void getAccommodationList(){

    }

    //숙소 등록
    @PreAuthorize("admin")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerAccommodation(@RequestBody AccommodationRegisterRequest registerRequest){
        accommodationService.registerAccommodation(registerRequest);

        return ResponseEntity.ok(ApiResponse.success("숙소 등록에 성공하였습니다."));
    }

}
