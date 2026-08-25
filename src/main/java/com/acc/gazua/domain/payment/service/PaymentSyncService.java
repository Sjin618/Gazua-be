package com.acc.gazua.domain.payment.service;

import com.acc.gazua.domain.reservation.service.ReservationService;
import com.acc.gazua.global.portone.PortOneClient;
import com.acc.gazua.global.portone.exception.PortOnePaymentNotFoundException;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.acc.gazua.global.portone.PortOneClient.*;

//PortOne과 DB를 호출하는 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSyncService {
    private final PortOneClient portOneClient;
    private final ReservationService reservationService;


    //결제 상태 동기화의 단일 진입점
    //웹훅 / confirm API / 스케쥴러 모두 이 메서드를 호출
    public PaymentSyncResult syncPayment(String paymentId){

        PortOnePaymentSnapshot snapshot;
        try{
            snapshot = portOneClient.getPayment(paymentId);
        }catch (PortOneUnavailableException e){
            //포트원 조회 실패는 재시도
            log.warn("포트원 조회 실패 - 상태 유지 - paymentId={}", paymentId, e);
            return PaymentSyncResult.PORTONE_UNAVAILABLE;
        }catch (PortOnePaymentNotFoundException e){
            //포트원에 기록 자체가 없기 때문에 재시도 하지 않음
            log.warn("포트원에 결제 기록 없음 - paymentId={}", paymentId);
            return PaymentSyncResult.NOT_PAID_YET;
        }

        if(snapshot.isPaid()){
            return handlePaid(snapshot);
        }
        if (snapshot.isFullyCancelled() || snapshot.isPartialCancelled()) {
            return reservationService.applyCancelled(paymentId, snapshot.cancelledAt(), "취소 정책 적용 환불",
                    snapshot.cancelledTotal(), snapshot.isFullyCancelled());
        }

        if (snapshot.isFailed()) {
            return reservationService.applyCancelled(paymentId, Instant.now(), "결제 실패", 0L, true);
        }

        // READY / PENDING / VIRTUAL_ACCOUNT_ISSUED / PAY_PENDING
        log.debug("아직 미확정 결제 - paymentId={}, status={}", paymentId, snapshot.status());
        return PaymentSyncResult.NOT_PAID_YET;
    }

    public PaymentSyncResult handlePaid(PortOnePaymentSnapshot snapshot){
        PaymentSyncResult result = reservationService.applyPaid(snapshot);

        if(result != PaymentSyncResult.AMOUNT_MISMATCH){
            return result;
        }

        //금액이 맞지 않으니 예약 취소 및 환불 조치
        log.warn("위변조 의심 - 자동 환불 진행 - paymentId={}", snapshot.paymentId());
        portOneClient.refundPayment(snapshot.paymentId(), null, "결제 금액이 달라 환불 처리되었습니다.(위변조 의심)");

        reservationService.applyCancelled(snapshot.paymentId(), Instant.now(), "결제 금액 불일치(위변조 의심)", snapshot.paidTotal(), true);

        return PaymentSyncResult.AMOUNT_MISMATCH;
    }
}
