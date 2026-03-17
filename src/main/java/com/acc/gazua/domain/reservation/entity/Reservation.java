package com.acc.gazua.domain.reservation.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Reservation {

    @Id @GeneratedValue
    @Column(name = "reservation_id")
    private Long id; //예약 번호

    @ManyToOne
    private User user; //예약한 사용자 ID

    @ManyToOne
    private Accommodation accommodation; //예약한 숙소 ID

    @Enumerated(value = EnumType.STRING)
    private ReservationStatus status;

    public boolean validateReservation(Long requestUserId){
        if(!user.equalsId(requestUserId)) throw new AccessDeniedException("리뷰 권한이 존재하지 않습니다.");
        if(!ReservationStatus.COMPLETED.equals(status)) throw new IllegalStateException("완료된 예약에 대해서만 리뷰할 수 있습니다.");
        return true;
    }

}
