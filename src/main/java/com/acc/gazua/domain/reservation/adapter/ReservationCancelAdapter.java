package com.acc.gazua.domain.reservation.adapter;

import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.reservation.dto.ReservationCancelRequest;
import com.acc.gazua.domain.reservation.service.ReservationService;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.portone.PortOneClient;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationCancelAdapter {
    private final PortOneClient portOneClient; //포트원 호출
    private final ReservationService reservationService; //취소 준비
    private  final PaymentSyncService paymentSyncService; //PortOne과 상태 동기화

    public RefundCalculation cancel(Long userId, Long reservationId, String reason) {
        //아직 예약 취소와 결제 환불을 확정하지 않음
        //예약 취소와 결제 환불 준비
        //검증 + 환불액 확정 + REFUND_REQUESTED 커밋
        CancelPreparation prep = reservationService.prepareCancel(reservationId, userId, reason);
        RefundCalculation calc = prep.calc();
        String paymentId = prep.paymentId();

        //환불 0원이면 PG 호출하지 않음(0원 취소는 거부됨)
        if(calc.isNoRefund()){
            reservationService.applyForfeited(paymentId, reason);
            return calc;
        }

        //PortOne 에 환불 금액과 함께 환불 요청
        //PG 취소 - 전액이면 amount를 비워 보내 CANCELLED 상태를 받는다
        Long amount = calc.isFullRefund() ? null : (long) calc.refundAmount();
        try{
            portOneClient.refundPayment(paymentId, amount, reason);
        }catch(PortOneUnavailableException e){
            //REFUND_REQUESTED 로 남아 스케줄러 or 웹훅이 재동기화한다
            log.error("PG 취소 요청 실패 - 재동기화 대상 - paymentId={}", paymentId, e);
            throw new PaymentException(ErrorCode.REFUND_REQUEST_FAILED);
        }

        //최종적으로 PortOne과 상태 동기화
        //confirm과 동일한 단일 진입점으로 상태 확정 + 재고 복구
        paymentSyncService.syncPayment(paymentId);
        return calc;
    }

    public record CancelPreparation(String paymentId, RefundCalculation calc){}
}
