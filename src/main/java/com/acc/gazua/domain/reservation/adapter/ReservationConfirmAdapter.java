package com.acc.gazua.domain.reservation.adapter;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.exception.PaymentException;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import com.acc.gazua.domain.reservation.dto.ReservationConfirmRequest;
import com.acc.gazua.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationConfirmAdapter {
    private final PaymentSyncService paymentSyncService;
    private final PaymentRepository paymentRepository;

    //결제 확정 요청이 동일한 유저인지 확인만 한다
    public PaymentSyncResult confirmReservation(ReservationConfirmRequest request, Long userId){
        Payment payment = paymentRepository.findByPaymentIdWithReservation(request.getPaymentId())
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_FOUND));

        //동일한 유저가 아니면 AccessDeniedException 발생
        payment.getReservation().equalsUser(userId);

        return paymentSyncService.syncPayment(request.getPaymentId());
    }
}
