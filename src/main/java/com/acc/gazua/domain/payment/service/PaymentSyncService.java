package com.acc.gazua.domain.payment.service;

import com.acc.gazua.domain.payment.dto.RefundCommand;
import com.acc.gazua.domain.payment.entity.RefundOrigin;
import com.acc.gazua.domain.reservation.service.ReservationPaymentService;
import com.acc.gazua.global.portone.PortOneClient;
import com.acc.gazua.global.portone.exception.PortOneConflictException;
import com.acc.gazua.global.portone.exception.PortOnePaymentNotFoundException;
import com.acc.gazua.global.portone.exception.PortOneRequestException;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;

import static com.acc.gazua.global.portone.PortOneClient.*;

//PortOne과 DB를 호출하는 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSyncService {
    private final PortOneClient portOneClient;
    private final ReservationPaymentService reservationPaymentService;

    private final Clock clock;


    //결제 상태 동기화의 단일 진입점
    //웹훅 / /confirm API / 스케쥴러 모두 이 메서드를 호출
    public PaymentSyncResult syncPayment(String paymentId){

        PortOnePaymentSnapshot snapshot;
        try{
            snapshot = portOneClient.getPayment(paymentId);
        }catch (PortOneUnavailableException e){
            //포트원 조회 실패는 재시도
            log.warn("포트원 서버 오류 조회 실패 - 상태 유지 - paymentId={}", paymentId, e);
            return PaymentSyncResult.PORTONE_UNAVAILABLE;
        }catch (PortOnePaymentNotFoundException e){
            //포트원에 기록 자체가 없기 때문에 재시도 하지 않음
            log.warn("포트원에 결제 기록 없음 - paymentId={}", paymentId);
            return PaymentSyncResult.NOT_PAID_YET;
        }catch(PortOneRequestException e){
            log.error("포트원 조회 요청 실패 - 수동 확인 필요 - paymentId={}", paymentId, e);
            return PaymentSyncResult.MANUAL_REVIEW_REQUIRED;
        }

        if(snapshot.isPaid()){
            return handlePaid(snapshot);
        }
        if (snapshot.isFullyCancelled() || snapshot.isPartialCancelled()) {
            return reservationPaymentService.applyPgCancellations(snapshot);
        }

        if (snapshot.isFailed()) {
            return reservationPaymentService.applyFailed(paymentId, "결제 실패");
        }

        // READY / PENDING / VIRTUAL_ACCOUNT_ISSUED / PAY_PENDING
        log.debug("아직 미확정 결제 - paymentId={}, status={}", paymentId, snapshot.status());
        return PaymentSyncResult.NOT_PAID_YET;
    }

    public PaymentSyncResult handlePaid(PortOnePaymentSnapshot snapshot) {
        PaymentSyncResult result = reservationPaymentService.applyPaid(snapshot);

        String reason = switch (result) {
            case AMOUNT_MISMATCH -> "결제 금액 불일치(위변조 의심)";
            case RESERVATION_EXPIRED -> "이미 만료된 예약";
            default -> null;
        };

        if (reason == null) return result;

        log.error("자동 환불 진행 - paymentId={}, 사유={}", snapshot.paymentId(), reason);
        RefundOrigin origin = result == PaymentSyncResult.AMOUNT_MISMATCH ? RefundOrigin.AMOUNT_MISMATCH : RefundOrigin.EXPIRED;
        Optional<Long> refundId = reservationPaymentService.requestSystemRefund(snapshot, origin, reason + "(으)로 환불 처리되었습니다.");

        //웹훅 <-> /confim 중 다른 쪽이 이미 환불을 시작했다 - 그쪽이나 재동기화가 마무리한다
        if(refundId.isEmpty()) return result;

        return switch (executeRefund(refundId.get())) {
            case REFUNDED, REFUND_PENDING -> result;
            case REFUND_FAILED -> PaymentSyncResult.MANUAL_REVIEW_REQUIRED; //자동 환불 거절은 사람이 봐야 한다
            default -> PaymentSyncResult.PORTONE_UNAVAILABLE;
        };
    }

    //Refund 커밋 이후 PG 에 환불을 요청하고 응답을 Refund 에 기록한다
    //사용자 취소 / 자동 환불 / 재동기화 재요청이 모두 이 메서드를 쓴다
    public PaymentSyncResult executeRefund(Long refundId) {
        RefundCommand cmd = reservationPaymentService.findRefundInProgress(refundId).orElse(null);
        if(cmd == null) return PaymentSyncResult.REFUNDED; //다른 경로(웹훅 등) 에서 이미 마무리됨

        PortOneCancellation cancellation;
        try {
            cancellation = portOneClient.refundPayment(cmd.paymentId(), cmd.amount(), cmd.reason(), cmd.cancellableAmount());
        } catch (PortOneConflictException e) {
            //이미 취소됐거나 취소 가능 금액이 바뀌었다 (이전 시도가 성공 / 관리자 환불) - PG 기록으로 맞춘다
            log.warn("PG 취소 거부 - PG 상태로 동기화 - refundId={}, paymentID={}", refundId, cmd.paymentId());
            return syncPayment(cmd.paymentId());
        } catch (PortOneUnavailableException e) {
            //PG 가 처리했는지 모른다 - REQUESTED 로 남겨 웹훅 / 재동기화 스케쥴러가 마무리한다
            log.warn("PG 취소 요청 실패 - 재동기화 대상 - refundId={}", refundId, e);
            return PaymentSyncResult.PORTONE_UNAVAILABLE;
        } catch (PortOneRequestException e) {
            //PG 가 요청 자체를 거절 - 환불되지 않았으므로 FAILED 로 닫는다
            log.error("포트원 환불 요청 거절 - refundId={},", refundId, e);
            reservationPaymentService.failRefund(cmd.paymentId(), refundId, e.getMessage());
            return PaymentSyncResult.REFUND_FAILED;
        }

        return reservationPaymentService.applyRefundResult(cmd.paymentId(), refundId, cancellation);
    }

}
