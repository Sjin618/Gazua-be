package com.acc.gazua.domain.reservation.facade;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import com.acc.gazua.domain.policy.dto.RefundCalculation;
import com.acc.gazua.domain.reservation.dto.*;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.service.ReservationHoldService;
import com.acc.gazua.domain.reservation.service.ReservationPaymentService;
import com.acc.gazua.domain.roominventory.exception.RoomInventoryException;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.portone.PortOneClient;
import com.acc.gazua.global.portone.exception.PortOneRequestException;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 트랜잭션 밖에서 수행되어야 할 기능과 트랜잭션 메서드들을 조립한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationFacade {
    private final ReservationHoldService reservationHoldService;
    private final ReservationPaymentService reservationPaymentService;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final PaymentSyncService paymentSyncService;

    //사용자가 예약 취소를 눌렀을 때 실행되는 환불 메서드
    public CancelOutcome cancel(Long userId, Long reservationId, String reason) {
        //아직 예약 취소와 결제 환불을 확정하지 않음
        //예약 취소와 결제 환불 준비
        //검증 + 환불액 확정 + REFUND_REQUESTED 커밋
        CancelPreparation prep = reservationPaymentService.prepareCancel(reservationId, userId, reason);
        RefundCalculation calc = prep.calc();
        String paymentId = prep.paymentId();

        //환불 0원이면 PG 호출하지 않음(0원 취소는 거부됨)
        if(calc.isNoRefund()){
            reservationPaymentService.applyForfeited(paymentId, reason);
            return new CancelOutcome(calc, true);
        }

        //PortOne 에 환불 금액과 함께 환불 요청
        //PG 취소 - 전액이면 amount를 비워 보내 CANCELLED 상태를 받는다
        Long amount = calc.isFullRefund() ? null : (long) calc.refundAmount();
        try{
            portOneClient.refundPayment(paymentId, amount, reason);
        }catch(PortOneUnavailableException e){
            //REFUND_REQUESTED 로 남아 스케줄러 or 웹훅이 재동기화한다
            log.warn("PG 취소 요청 실패 - 재동기화 대상 - paymentId={}", paymentId, e);
            return new CancelOutcome(calc, false);
        }catch (PortOneRequestException e){
            log.error("포트원 환불 요청 실패 - 수동 조작 필요 - paymentId={}", paymentId, e);
            return new CancelOutcome(calc, false);
        }

        //최종적으로 PortOne과 상태 동기화
        //confirm과 동일한 단일 진입점으로 상태 확정 + 재고 복구
        PaymentSyncResult result = paymentSyncService.syncPayment(paymentId);
        boolean settled = result == PaymentSyncResult.CANCELLED || result == PaymentSyncResult.ALREADY_DONE;

        return new CancelOutcome(calc,settled);
    }

    public PaymentSyncResult confirmReservation(ReservationConfirmRequest request, Long userId){
        Payment payment = paymentRepository.findByPaymentIdWithReservation(request.getPaymentId())
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        //동일한 유저가 아니면 AccessDeniedException 발생
        payment.getReservation().equalsUser(userId);

        return paymentSyncService.syncPayment(request.getPaymentId());
    }

    //제약 위반으로 롤백된 트랜잭션 안에서는 재조회가 불가능하기 때문에 트랜잭션 밖에서 수행 - REPEATABLE READ 락 스냅샷
    public ReservationInitResponse init(ReservationInitRequest request, Long userId) {
        String dedupKey = Reservation.dedupKeyOf(userId, request.roomId(), request.checkDate().checkIn(), request.checkDate().checkOut());

        //살아있는 예약이 있으면 재고를 건드리지 않고 그대로 반환
        Optional<ReservationInitResponse> reused = reservationHoldService.reuseHold(dedupKey);
        if (reused.isPresent()) {
            return reused.get();
        }

        try{
            return reservationHoldService.createHold(request, userId, dedupKey);
        }catch(DataIntegrityViolationException | RoomInventoryException e){
            //같은 사용자의 중복 요청에서 진 쪽 - 트랜잭션이 롤백되어 재고 차감도 원복된 상태
            //OUT_OF_STOCK 도 에러를 보여주는게 아닌, 정상 응답으로 처리되도록 한다.
            log.info("가예약 동시 생성 경합 - 기존 건 반환 - dedupKey={}", dedupKey);
            return reservationHoldService.reuseHold(dedupKey)
                    .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_AVAILABLE));
        }
    }

}
