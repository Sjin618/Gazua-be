package com.acc.gazua.domain.reservation.repository;

import com.acc.gazua.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Reservation> findByIdWithUser(@Param("id") Long id);
}
