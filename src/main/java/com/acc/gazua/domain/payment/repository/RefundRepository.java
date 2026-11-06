package com.acc.gazua.domain.payment.repository;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.Refund;
import com.acc.gazua.domain.payment.entity.RefundStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    boolean existsByPaymentAndStatusIn(Payment payment, Collection<RefundStatus> statuses);

    Optional<Refund> findByCancellationId(String cancellationId);

    Optional<Refund> findFirstByPaymentAndStatus(Payment payment, RefundStatus status);

    @Query("SELECT r FROM Refund r JOIN FETCH r.payment WHERE r.id = :id")
    Optional<Refund> findWithPaymentById(@Param("id") Long id);

    @Query("SELECT r.id FROM Refund r WHERE r.status IN :statuses " +
            "AND r.requestedAt <= :until AND r.requestedAt >= :since " +
            "ORDER BY r.requestedAt ASC")
    List<Long> findPendingRefundIds(@Param("statuses") Collection<RefundStatus> statuses,
                                    @Param("until") LocalDateTime until,
                                    @Param("since") LocalDateTime since,
                                    Pageable pageable);

    @Query("SELECT r.id FROM Refund r WHERE r.status = :status " +
            "AND r.requestedAt <= :until ORDER BY r.requestedAt ASC")
    List<Long> findRefundIdsByStatusBefore(@Param("status") RefundStatus status,
                                           @Param("until") LocalDateTime until,
                                           Pageable pageable);

    @Query("SELECT r.id FROM Refund r WHERE r.status IN :statuses AND r.requestedAt < :since")
    List<Long> findStalledRefundIds(@Param("statuses") Collection<RefundStatus> statuses,
                                    @Param("since") LocalDateTime since,
                                    Pageable pageable);
}
