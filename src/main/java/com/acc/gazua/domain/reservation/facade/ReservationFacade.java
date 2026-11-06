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
    public RefundCalculation cancel(Long userId, Long reservationId, String reason) {
        //DB 가 PG 보다 늦은 경우(READY)는 웹훅 / 가예약 만료 스케쥴러가 먼저 확정하므로 여기서 동기화하지 않는다
        //[TX 1] 본인 확인 + 검증 + 환불액 확정 + Refund(REQUESTED) 커밋 - 0원이면 여기서 종결
        CancelPreparation prep = reservationPaymentService.prepareCancel(reservationId, userId, reason);
        if(prep.calc().isNoRefund()) return prep.calc();

        //PG 환불 요청 -> [TX] 응답을 Refund 에 기록
        PaymentSyncResult result = paymentSyncService.executeRefund(prep.refundId());

        //PG 가 거절 - Refund 는 FAILED 로 닫혔으므로 사용자가 다시 취소할 수 있다
        if (result == PaymentSyncResult.REFUND_FAILED) {
            throw new PaymentException(ErrorCode.REFUND_FAILED);
        }

        //PORTONE_UNAVAILABLE / REFUND_PENDING 은 접수로 응답 - 웹훅/재동기화가 마무리한다
        return prep.calc();
    }

    public PaymentSyncResult confirmReservation(ReservationConfirmRequest request, Long userId){
        Payment payment = paymentRepository.findByPaymentIdWithReservationAndUser(request.getPaymentId())
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
