package com.acc.gazua.domain.reservation.controller;

import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.reservation.adapter.ReservationCancelAdapter;
import com.acc.gazua.domain.reservation.dto.*;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import com.acc.gazua.domain.reservation.adapter.ReservationConfirmAdapter;
import com.acc.gazua.domain.reservation.service.ReservationService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation")
public class ReservationController {
    private final ReservationService reservationService;
    private final ReservationConfirmAdapter reservationConfirmAdapter; //결제 확정 관련 처리
    private final ReservationCancelAdapter reservationCancelAdapter;   //결제 환불 관련 처리
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
    public ResponseEntity<ApiResponse<Void>> confirmReservation(@AuthenticationPrincipal CustomUserDetails user,
                                                                @RequestBody ReservationConfirmRequest request) {
        PaymentSyncResult result = reservationConfirmAdapter.confirmReservation(request, user.getUserId());
        //실패 상태는 예외로 올려 GlobalExceptionHandler 가 ErrorResponse 로 변환한다
        return switch(result){
            case CONFIRMED, ALREADY_DONE ->
                    ResponseEntity.ok(ApiResponse.success("예약이 확정되었습니다."));

            case NOT_PAID_YET ->
                    ResponseEntity.accepted().body(ApiResponse.success("결제 확인 중입니다."));

            case PORTONE_UNAVAILABLE ->
                    throw new PaymentException(ErrorCode.PAYMENT_CONFIRM_DELAYED);

            case AMOUNT_MISMATCH ->
                    throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);

            case CANCELLED, FAILED ->
                    throw new PaymentException(ErrorCode.PAYMENT_CANCELLED_OR_FAILED);

            case UNKNOWN_PAYMENT, PARTIAL_CANCEL_UNSUPPORTED ->
                    throw new PaymentException(ErrorCode.PAYMENT_STATUS_UNKNOWN);
        };
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelReservation(@AuthenticationPrincipal CustomUserDetails user,
                                                               @PathVariable("reservationId") Long reservationId,
                                                               @RequestBody ReservationCancelRequest request){

        RefundCalculation calc = reservationCancelAdapter.cancel(user.getUserId(), reservationId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(ReservationCancelResponse.from(calc),"예약이 취소되었습니다."));
    }

    //예약 내역(리스트) 가져오기 (이용 전, 이용 후, 취소 환불)
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<ReservationGetResponse>>> getReservationList(@AuthenticationPrincipal CustomUserDetails user,
                                                                                              @RequestParam ReservationStatus status,
                                                                                              @RequestParam(required = false) String cursor,
                                                                                              @RequestParam(defaultValue = "10") @Min(10) @Max(30) int size) {
        return ResponseEntity.ok(ApiResponse.success(reservationService.getReservationList(user.getUserId(), status, cursor, size), "예약 내역 조회 성공"));
    }


    //상세 예약 정보 가져오기
}
