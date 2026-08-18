package com.acc.gazua.domain.payment.repository;

import com.acc.gazua.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentIdAndReservationId(String paymentId, Long reservationId);
}
