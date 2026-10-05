package com.acc.gazua.domain.payment.repository;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.reservation r JOIN FETCH r.user WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdWithReservationAndUser(String paymentId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.reservation r JOIN FETCH r.user WHERE p.reservation.id = :reservationId")
    Optional<Payment> findByReservationIdWithReservationAndUser(@Param("reservationId") Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);

    Optional<Payment> findByReservationId(Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p from Payment p WHERE p.reservation.id = :reservationId")
    Optional<Payment> findByReservationIdForUpdate(@Param("reservationId") Long reservationId);

    @Query("SELECT p from Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentId(@Param("paymentId") String paymentId);


    @Query("SELECT p.paymentId FROM Payment p " +
            "WHERE p.status = com.acc.gazua.domain.payment.entity.PaymentStatus.REFUND_REQUESTED " +
            "AND p.cancelledAt <= :until AND p.cancelledAt >= :since " +
            "ORDER BY p.cancelledAt ASC")
    List<String> findPendingRefundPaymentIds(@Param("until") LocalDateTime until,
                                             @Param("since") LocalDateTime since,
                                             Pageable pageable);

    @Query("SELECT p.paymentId FROM Payment p " +
            "WHERE p.status = com.acc.gazua.domain.payment.entity.PaymentStatus.REFUND_REQUESTED " +
            "AND p.cancelledAt < :since")
    List<String> findStalledRefundPaymentIds(@Param("since") LocalDateTime since, Pageable pageable);
}
