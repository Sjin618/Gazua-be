package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.payment.dto.RefundCommand;
import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.entity.RefundStatus;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.repository.RefundRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import com.acc.gazua.global.portone.PortOneClient;
import com.acc.gazua.global.portone.exception.PortOneConflictException;
import com.acc.gazua.global.portone.exception.PortOneRequestException;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;

import static com.acc.gazua.global.portone.PortOneClient.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciler {
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final ReservationPaymentService reservationPaymentService;
    private final PaymentSyncService paymentSyncService;
    private final RefundRepository refundRepository;

    private final Clock clock;


    public boolean reconcile(Long refundId) {
        RefundCommand cmd = reservationPaymentService.findRefundInProgress(refundId).orElse(null);
        //조회하는 사이 웹훅이나 사용자 재시도로 마무리 될 수도 있다
        if(cmd == null) return false;

        PortOnePaymentSnapshot snapshot;
        try{
            snapshot = portOneClient.getPayment(cmd.paymentId());
        }catch(PortOneRequestException e){
            log.error("PG 조회 불가 - 수동 확인 필요 - refundId={}", refundId, e);
            return false;
        }catch(PortOneUnavailableException e){
            log.warn("PG 조회 실패 - 다음 주기 재시도 - refundId={}", refundId, e);
            return false;
        }

        // 1. PG의 취소 내역을 Refund 와 맞춘다
        //      - 성공한 취소: 유실된 응답 회수
        //      - 실패한 취소: PENDING 이었던 Refund 를 FAILED 로 닫는다
        if (!snapshot.cancellations().isEmpty()) {
            reservationPaymentService.applyPgCancellations(snapshot);
            if(isClosed(refundId)){
                log.info("환불 재동기화 - PG 취소 내역 반영 - refundId={}", refundId);
                return true;
            }
        }

        // 2. PG 가 접수만 한 취소 - 1번에서 방금 PENDING 이 됐을 수 있으므로 DB 에서 다시 읽는다.
        //    PENDING 상태라면 재요청 하지 않고, 다음 웹훅 / 동기화 에서 재확인한다
        if(isPending(refundId)) return false;

        // 3. 더 취소할 금액이 없는데 짝이 되는 PG 취소도 없다
        if (snapshot.isFullyCancelled()) {
            reservationPaymentService.failRefund(cmd.paymentId(), refundId, "PG 에 대응하는 취소 내역 없음");
            log.error("대응하는 PG 취소 없음 - 수동 확인 필요 - refundId={}", refundId);
            return false;
        }

        if (!snapshot.isPaid() && !snapshot.isPartialCancelled()) {
            log.error("예상치 못한 PG 상태 - 수동 확인 필요 - refundId={}, status={}", refundId, snapshot.status());
            return false;
        }

        // 4. 요청이 PG 에 닿지 않았다 - 같은 금액, 같은 취소 가능 금액으로 재요청
        //    첫 요청이 실은 성공했다면 취소 가능 금액이 달라 PG 가 409 로 거절하므로 이중 환불 되지 않는다
        PaymentSyncResult result = paymentSyncService.executeRefund(refundId);
        log.info("환불 재요청 - refundId={}, result={}", refundId, result);
        return result == PaymentSyncResult.REFUNDED || result == PaymentSyncResult.REFUND_PENDING;
    }

    private boolean isClosed(Long refundId) {
        return refundRepository.findById(refundId)
                .map(r -> !r.isInProgress())
                .orElse(true);
    }

    private boolean isPending(Long refundId) {
        return refundRepository.findById(refundId)
                .map(r -> r.getStatus() == RefundStatus.PENDING)
                .orElse(false);
    }


}
