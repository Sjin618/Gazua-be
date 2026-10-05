package com.acc.gazua.domain.reservation.entity;

import com.acc.gazua.domain.policy.entity.RefundPolicySnapshot;
import com.acc.gazua.domain.review.exception.ReviewException;
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.global.base.BaseTimeEntity;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.entity.CheckDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_reservation_dedup_key", columnNames = "dedup_key"))
public class Reservation extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id; //예약 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; //예약한 사용자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room; //예약한 숙소의 방 ID

    @Enumerated(value = EnumType.STRING)
    private ReservationStatus status;

    @Embedded
    private CheckDateTime checkDateTime;

    @Embedded
    private RefundPolicySnapshot refundPolicy; // 예약 시점 환불 정책(불변)

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice; //가예약 생성 당시의 결제 금액

    @Column(name = "dedup_key", length = 120)
    private String dedupKey; // PENDING 동안만 값 보유, user,room,checkIn,checkOut 동일한 조건으로 요청 시, 로직 중복 실행을 막는 역할

    private LocalDateTime holdExpiresAt; //가예약 만료 시각

    public static String dedupKeyOf(Long userId, Long roomId, LocalDate in, LocalDate out) {
        return userId + ":" + roomId + ":" + in + ":" + out;
    }


    public boolean validateReviewAuthority(Long userId){
        equalsUser(userId);
        if(!ReservationStatus.COMPLETED.equals(status)) throw new ReviewException(ErrorCode.NOT_REVIEWABLE);
        return true;
    }

    public boolean equalsUser(Long userId){
        if(!user.equalsId(userId)) throw new AccessDeniedException("접근 권한이 존재하지 않습니다.");
        return true;
    }

    public void confirmReservation(){
        this.status = ReservationStatus.CONFIRM;
        releaseHold();
    }

    public void cancelReservation(){
        this.status = ReservationStatus.CANCELLED;
        releaseHold();
    }

    public void expireReservation(){
        this.status = ReservationStatus.EXPIRED;
        releaseHold();
    }

    private void releaseHold(){
        this.dedupKey = null;
        this.holdExpiresAt = null;
    }

    public boolean isPending() {
        return ReservationStatus.PENDING.equals(this.status);
    }
    public boolean isConfirmed(){
        return ReservationStatus.CONFIRM.equals(this.status);
    }

    public boolean holdsInventory() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRM;
    }

    public boolean isHoldExpired(LocalDateTime now) {
        return holdExpiresAt != null && !holdExpiresAt.isAfter(now);
    }

    public boolean hasStarted(LocalDateTime now){
        return !this.checkDateTime.getCheckInDateTime().isAfter(now);
    }

}
