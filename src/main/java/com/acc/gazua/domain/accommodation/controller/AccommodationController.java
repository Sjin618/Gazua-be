package com.acc.gazua.domain.accommodation.controller;

import com.acc.gazua.domain.accommodation.dto.AccommodationRegisterRequest;
import com.acc.gazua.domain.accommodation.dto.AccommodationSearchDto;
import com.acc.gazua.domain.accommodation.dto.AccommodationSearchRequest;
import com.acc.gazua.domain.accommodation.entity.CheckDate;
import com.acc.gazua.domain.accommodation.service.AccommodationService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accommodation")
public class AccommodationController {
    private final AccommodationService accommodationService;

    //숙소 조회(페이징)
    //지역, 인원 수, 기간 필터링 3개는 필수로 들어와야 함
    //평점, 리뷰 수, 가격 정렬 3개 중에 선택해서 들어옴
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<AccommodationSearchDto>>> getAccommodationList(@RequestParam Integer region,
                                                                                                @RequestParam Integer personal,
                                                                                                @RequestParam CheckDate checkDate,
                                                                                                @RequestParam(required = false) String sortType,
                                                                                                @RequestParam(required = false) String cursor,
                                                                                                @RequestParam(defaultValue = "10") int size) {

        CursorPage<AccommodationSearchDto> accommodationPage = accommodationService.searchAccommodation(region,
                personal,
                checkDate,
                sortType,
                cursor,
                size);
        return ResponseEntity.ok(ApiResponse.success(accommodationPage, "숙소 조회 성공"));
    }

    //숙소 등록
    @PreAuthorize("admin")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerAccommodation(@RequestBody AccommodationRegisterRequest registerRequest){
        accommodationService.registerAccommodation(registerRequest);

        return ResponseEntity.ok(ApiResponse.success("숙소 등록에 성공하였습니다."));
    }

}
