package com.acc.gazua.domain.reservation.controller;

import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.reservation.facade.ReservationFacade;
import com.acc.gazua.domain.reservation.dto.*;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import com.acc.gazua.domain.reservation.service.ReservationQueryService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.security.details.CustomUserDetails;
import jakarta.validation.Valid;
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
    private final ReservationQueryService reservationQueryService;
    private final ReservationFacade reservationFacade; // 트랜잭션 밖에서 해야 할 메서드들을 실행한다
    //가예약 만들기 or 가져오기
    //어떤 방에 대한 예약인지
    //어떤 사용자가 예약하는지
    //언제 체크인 하고 체크아웃 하는지
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<ReservationInitResponse>> getAndCreateReservation(@Valid @RequestBody ReservationInitRequest request,
                                                                                        @AuthenticationPrincipal CustomUserDetails user){

        return ResponseEntity.ok(ApiResponse.success(reservationFacade.init(request,user.getUserId()),"가예약 생성 및 조회 성공")
        );
    }

    //PortOne 동기화 API
    //사용자 결제 후 프론트에서 호출해야 한다.
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmReservation(@AuthenticationPrincipal CustomUserDetails user,
                                                                @Valid @RequestBody ReservationConfirmRequest request) {
        PaymentSyncResult result = reservationFacade.confirmReservation(request, user.getUserId());
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

            case RESERVATION_EXPIRED ->
                    throw new PaymentException(ErrorCode.RESERVATION_EXPIRED);

            case MANUAL_REVIEW_REQUIRED ->
                    throw new PaymentException(ErrorCode.PAYMENT_MANUAL_REVIEW);

            case UNKNOWN_PAYMENT ->
                    throw new PaymentException(ErrorCode.PAYMENT_STATUS_UNKNOWN);
        };
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelReservation(@AuthenticationPrincipal CustomUserDetails user,
                                                               @PathVariable("reservationId") Long reservationId,
                                                               @Valid @RequestBody ReservationCancelRequest request){

        CancelOutcome outcome = reservationFacade.cancel(user.getUserId(), reservationId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(ReservationCancelResponse.from(outcome.calc(), outcome.settled()),
                outcome.settled() ? "예약이 취소되었습니다." : "예약 취소가 접수되었습니다."));
    }

    //예약 내역(리스트) 가져오기 (이용 전, 이용 후, 취소 환불)
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPage<ReservationGetResponse>>> getReservationList(@AuthenticationPrincipal CustomUserDetails user,
                                                                                              @RequestParam ReservationStatus status,
                                                                                              @RequestParam(required = false) String cursor,
                                                                                              @RequestParam(defaultValue = "10") @Min(10) @Max(30) int size) {
        return ResponseEntity.ok(ApiResponse.success(reservationQueryService.getReservationList(user.getUserId(), status, cursor, size), "예약 내역 조회 성공"));
    }


    //상세 예약 정보 가져오기
}
