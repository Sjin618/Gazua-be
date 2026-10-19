package com.acc.gazua.domain.payment.service;

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
    //웹훅 / confirm API / 스케쥴러 모두 이 메서드를 호출
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
            return reservationPaymentService.applyCancelled(paymentId, snapshot.cancelledAt(), "취소 정책 적용 환불",
                    snapshot.refundTotal(), snapshot.isFullyCancelled());
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
        try {
            portOneClient.refundPayment(snapshot.paymentId(), null, reason + "(으)로 환불 처리되었습니다.", snapshot.paidTotal());
        } catch (PortOneConflictException e) {
            //웹훅과 /confirm 이 동시에 와서 다른 쪽이 먼저 전액 환불했다 - 결과가 같으므로 아래에서 전액 환불로 기록
            log.info("자동 환불 중복 요청 - 이미 처리됨 - paymentId={}", snapshot.paymentId());
        } catch (PortOneRequestException e) {
            log.warn("포트원 환불 요청 실패 - 수동 조작 필요 - paymentId={}", snapshot.paymentId(), e);
            return PaymentSyncResult.MANUAL_REVIEW_REQUIRED;
        } catch (PortOneUnavailableException e) {
            //포트원 조회 실패는 재시도
            log.warn("포트원 서버 오류 환불 요청 실패 - 상태 유지 - paymentId={}", snapshot.paymentId(), e);
            return PaymentSyncResult.PORTONE_UNAVAILABLE;
        }
        reservationPaymentService.applyCancelled(snapshot.paymentId(), clock.instant(), reason, snapshot.paidTotal(), true);
        return result;
    }


}
