package com.acc.gazua.domain.reservation.controller;

import com.acc.gazua.domain.reservation.dto.ReservationConfirmRequest;
import com.acc.gazua.domain.reservation.dto.ReservationGetResponse;
import com.acc.gazua.domain.reservation.dto.ReservationInitRequest;
import com.acc.gazua.domain.reservation.dto.ReservationInitResponse;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import com.acc.gazua.domain.reservation.service.ReservationFacade;
import com.acc.gazua.domain.reservation.service.ReservationService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation")
public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationFacade reservationFacade; //결제 관련 처리
    //가예약 만들기 or 가져오기
    //어떤 방에 대한 예약인지
    //어떤 사용자가 예약하는지
    //언제 체크인 하고 체크아웃 하는지
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<ReservationInitResponse>> getAndCreateReservation(@RequestBody ReservationInitRequest request,
                                                                                        @AuthenticationPrincipal CustomUserDetails user){

        return ResponseEntity.ok(ApiResponse.success(reservationService.getAndCreateReservation(request,user.getUserId()),"가예약 생성 및 조회 성공")
        );
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(@RequestBody ReservationConfirmRequest request){
        reservationFacade.confirmReservation(request);
        return ResponseEntity.ok(ApiResponse.success("결제 검증에 성공하였습니다."));
    }

    //예약 내역(리스트) 가져오기 (이용 전, 이용 후, 취소 환불)
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<ReservationGetResponse>>> getReservationList(@AuthenticationPrincipal CustomUserDetails user,
                                                                                              @RequestParam ReservationStatus status,
                                                                                              @RequestParam(required = false) String cursor,
                                                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationList(user, status, cursor, size), "예약 내역 조회 성공"));
    }


    //상세 예약 정보 가져오기
}
