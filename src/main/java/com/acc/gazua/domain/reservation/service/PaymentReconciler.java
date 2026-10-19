package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
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

    private final Clock clock;

    public boolean reconcile(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId).orElse(null);

        //조회하는 사이 Payment 가 웹훅이나 사용자 재시도로 마무리 될 수도 있다.
        if (payment == null || payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            return false;
        }

        //markRefundRequested 가 확정한 금액을 그대로 재사용한다.
        Integer refundTotal = payment.getRefundAmount();
        boolean fully = payment.getCancelFee() == 0;
        String reason = payment.getCancelReason();

        PortOnePaymentSnapshot snapshot;
        try {
            snapshot = portOneClient.getPayment(paymentId);
        } catch (PortOneRequestException e) {
            log.error("PG 조회 불가 - 수동 확인 필요 - paymentId={}", paymentId, e);
            return false;
        } catch (PortOneUnavailableException e) {
            log.warn("PG 조회 실패 - 다음 주기 재시도 - paymentId={}", paymentId, e);
            return false;
        }

        // 1. 이미 환불된 결제는 PortOne에 환불 요청하지 않고 DB만 업데이트한다.
        if (snapshot.isFullyCancelled() || snapshot.isPartialCancelled()) {
            reservationPaymentService.applyCancelled(paymentId, snapshot.cancelledAt(), reason, snapshot.refundTotal(), snapshot.isFullyCancelled());
            log.info("환불 재동기화 - 유실된 PG 응답 반영 - paymentId={}", paymentId);
            return true;
        }

        // 2. 아직 결제 상태 - 취소 요청이 PG에 닿지 않음. 저장된 금액으로 재요청
        if (snapshot.isPaid()) {

            //환불 0원(위약금 100%) - PG 는 0원 취소를 거부하므로 PG 호출 없이 마무리
            if(refundTotal == 0){
                reservationPaymentService.applyForfeited(paymentId, reason);
                return true;
            }

            try {
                portOneClient.refundPayment(paymentId, fully ? null : refundTotal.longValue(), reason, payment.getPrice().longValue());
            } catch (PortOneConflictException e) {
                //getPayment 이후 PG 가 바뀌었다 ( 관리자 환불, 늦게 처리된 우리 요청 등등)
                //REFUND_REQUESTED 로 남겨두면 다음 주기 때 getPayment()로 PG 와 상태가 동기화된다.
                log.warn("PG 취소 거부 - 다음 주기에 PG 상태로 동기화 - paymentId={}", paymentId);
                return false;
            } catch (PortOneRequestException e) {
                log.error("PG 취소 재요청 영구 실패 - 수동 확인 필요 - paymentId={}", paymentId, e);
                return false;
            } catch (PortOneUnavailableException e) {
                log.warn("PG 취소 재요청 실패 - 다음 주기 재시도 - paymentId={}", paymentId);
                return false;
            }

            reservationPaymentService.applyCancelled(paymentId, clock.instant(), reason, refundTotal, fully);
            log.info("환불 재요청 성공 - paymentId={}. amount={}", paymentId, refundTotal);

            return true;
        }

        log.error("예상치 못한 PG 상태 - 수동 확인 필요 - paymentId={}, status={}", paymentId, snapshot.status());
        return false;
    }

}
