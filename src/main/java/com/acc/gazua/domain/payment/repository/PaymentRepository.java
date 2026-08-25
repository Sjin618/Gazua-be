package com.acc.gazua.domain.payment.repository;

import com.acc.gazua.domain.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.reservation r WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdWithReservation(String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.reservation.id = :reservationId " +
            "AND p.status <> com.acc.gazua.domain.payment.entity.PaymentStatus.READY")
    Optional<Payment> findSettledByReservationIdForUpdate(@Param("reservationId") Long reservationId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.reservation r JOIN FETCH r.room " +
            "WHERE r.id = :reservationId " +
            "AND p.status <> com.acc.gazua.domain.payment.entity.PaymentStatus.READY")
    Optional<Payment> findSettledByReservationId(@Param("reservationId") Long reservationId);
}
