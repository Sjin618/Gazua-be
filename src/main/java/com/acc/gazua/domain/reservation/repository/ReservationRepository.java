package com.acc.gazua.domain.reservation.repository;

import com.acc.gazua.domain.reservation.dto.ReservationGetResponse;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Reservation> findByIdWithUser(@Param("id") Long reservationId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.room WHERE r.id = :id")
    Optional<Reservation> findByIdWithRoom(@Param("id") Long reservationId);

    @Query("SELECT r.id, r.thumbnailUrl, a.name as accommodationName, rm.name as roomName, r.checkDateTime.checkInDateTime,r.checkDateTime.checkOutDateTime " +
            "FROM Reservation r JOIN r.room rm " +
            "JOIN rm.accommodation a " +
            "WHERE r.status = :status AND r.user.id = :userId AND r.id < :cursorId " +
            "ORDER BY r.id DESC ")
    Slice<ReservationGetResponse> findByReservationStatus(@Param("status")ReservationStatus status,
                                                          @Param("cursorId") Long cursorId,
                                                          @Param("userId") Long userId,
                                                          Pageable pageable);
}
