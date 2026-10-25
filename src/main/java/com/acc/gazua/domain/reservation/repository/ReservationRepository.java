package com.acc.gazua.domain.reservation.repository;

import com.acc.gazua.domain.reservation.dto.ExpiryReservation;
import com.acc.gazua.domain.reservation.dto.ReservationGetResponse;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room WHERE r.id = :id")
    Optional<Reservation> findByIdWithUserAndRoom(@Param("id") Long reservationId);

    @Query("SELECT new com.acc.gazua.domain.reservation.dto.ReservationGetResponse(" +
            "r.id, a.id, COALESCE(rm.thumbnailUrl, a.thumbnailUrl), a.name, rm.name, " +
            "r.checkDateTime.checkInDateTime, r.checkDateTime.checkOutDateTime) " +
            "FROM Reservation r " +
            "JOIN r.room rm " +
            "JOIN rm.accommodation a " +
            "WHERE r.status = :status " +
            "AND r.user.id = :userId " +
            "AND r.id < :cursorId " +
            "ORDER BY r.id DESC ")
    Slice<ReservationGetResponse> findByReservationStatus(@Param("status") ReservationStatus status,
                                                          @Param("cursorId") Long cursorId,
                                                          @Param("userId") Long userId,
                                                          Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Reservation r " +
            "SET r.status = com.acc.gazua.domain.reservation.entity.ReservationStatus.COMPLETED " +
            "WHERE r.status = com.acc.gazua.domain.reservation.entity.ReservationStatus.CONFIRM " +
            "AND r.checkDateTime.checkOutDateTime <= :now")
    int completeReservation(@Param("now") LocalDateTime now); //ReservationCheckOutScheduler 에서 호출

    @Query("SELECT r FROM Reservation r  JOIN FETCH r.user WHERE r.dedupKey = :dedupKey")
    Optional<Reservation> findByDedupKey(@Param("dedupKey") String dedupKey);

    @Query("SELECT new com.acc.gazua.domain.reservation.dto.ExpiryReservation(r.id,p.paymentId) " +
            "FROM Reservation r JOIN Payment p on p.reservation = r " +
            "WHERE r.status = com.acc.gazua.domain.reservation.entity.ReservationStatus.PENDING " +
            "AND r.holdExpiresAt <= :now " +
            "ORDER BY r.holdExpiresAt ASC")
    List<ExpiryReservation> findExpiredHold(@Param("now") LocalDateTime now, Pageable pageable); //ReservationHoldExpiryScheduler 에서 호출, 시간이 지난 예약 만료 처리

    // JOIN FETCH 없이 잠근다 — fetch join하면 MySQL이 room 행까지 FOR UPDATE로 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);
}
