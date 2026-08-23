package com.acc.gazua.domain.payment.service;

import com.acc.gazua.domain.payment.dto.PaymentPrepareRequest;
import com.acc.gazua.domain.payment.dto.PaymentPrepareResponse;
import com.acc.gazua.domain.payment.dto.PaymentVerifyRequest;
import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final AccommodationRepository accommodationRepository;


    //결제 준비
    //예약 생성, 결제 일부분 생성
    @Override
    public PaymentPrepareResponse preparePayment(CustomUserDetails customUserDetails, PaymentPrepareRequest request) {
        Long userId = customUserDetails.getUserId();

        //예약 번호가 존재하는지 확인
        Reservation reservation = reservationRepository.findByIdWithUser(request.reservationId())
                .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_FOUND));

        //예약 사용자와 요청 사용자가 같은지 확인
        reservation.equalsUser(userId);

        //Payment 객체를 만들고 DTO 반환
        String merchantId = "temp";

        Payment payment = Payment.builder()
                .price(0)
                .paymentId(merchantId)
                .reservation(reservation)
                .status(PaymentStatus.READY)
                .build();

        paymentRepository.save(payment);

        return new PaymentPrepareResponse(merchantId);
    }

    @Override
    public void verifyPayment(PaymentVerifyRequest request) {

        //포트원에서 impUid 로 결제 정보를 가지고 옴


    }
}
