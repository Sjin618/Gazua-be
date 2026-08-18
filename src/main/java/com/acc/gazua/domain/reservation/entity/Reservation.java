package com.acc.gazua.domain.reservation.entity;

import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.global.entity.CheckDateTime;
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
    private Room room; //예약한 숙소의 방 ID

    @Enumerated(value = EnumType.STRING)
    private ReservationStatus status;

    @Embedded
    private CheckDateTime checkDateTime;



    public boolean validateReviewAuthority(Long userId){
        equalsUser(userId);
        if(!ReservationStatus.COMPLETED.equals(status)) throw new IllegalStateException("완료된 예약에 대해서만 리뷰할 수 있습니다.");
        return true;
    }

    public boolean equalsUser(Long userId){
        if(!user.equalsId(userId)) throw new AccessDeniedException("접근 권한이 존재하지 않습니다.");
        return true;
    }

    public void confirmReservation(){
        this.status = ReservationStatus.CONFIRM;
    }

    public void cancelReservation(){
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isConfirmed(){
        return ReservationStatus.CONFIRM.equals(this.status);
    }

}
