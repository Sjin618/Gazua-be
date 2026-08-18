package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.reservation.dto.ReservationConfirmRequest;
import com.acc.gazua.global.portone.PortOneClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationFacade {

    private final ReservationService reservationService;
    private final PortOneClient portOneClient;


    public void confirmReservation(ReservationConfirmRequest request){
        //결제된 금액과 merchantId 가져오기
        PortOneClient.PortOnePaymentInfo paymentInfo = portOneClient.getPaymentAmount(request.getPaymentId());

        //프론트에서 보낸 것과 PortOne에서 준 정보가 일치하는지 확인
        if(!paymentInfo.merchantId().equals(request.getMerchantId())){
            throw new IllegalArgumentException("결제 정보가 일치하지 않습니다.");
        }
        //DB와 실제 결제 금액이 맞는지 확인 후 boolean 반환
        boolean isPaidValid = reservationService.verifyAndConfirmReservation(request.getMerchantId(), request.getReservationId(), paymentInfo.paidTotal());

        //결제 위변조 의심 취소 환불 진행
        if(!isPaidValid){

            //결제 환불 요청
            portOneClient.refundPayment(request.getPaymentId(), null, "결제 금액이 달라 환불 처리되었습니다.(위변조 의심)");
            //예약과 결제 환불로 상태 변화
            reservationService.failReservationAndPayment(paymentInfo.merchantId(), request.getReservationId());
            throw new IllegalStateException("결제 금액이 일치하지 않아 환불 처리되었습니다.");
        }

    }
}
