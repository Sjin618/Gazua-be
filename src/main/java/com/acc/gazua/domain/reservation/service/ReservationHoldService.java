package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.policy.entity.RefundPolicySnapshot;
import com.acc.gazua.domain.policy.service.CancellationPolicyResolver;
import com.acc.gazua.domain.reservation.dto.ReservationInitRequest;
import com.acc.gazua.domain.reservation.dto.ReservationInitResponse;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.room.exception.RoomException;
import com.acc.gazua.domain.room.repository.RoomRepository;
import com.acc.gazua.domain.roominventory.service.RoomInventoryService;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.entity.CheckDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReservationHoldService {
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomInventoryService roomInventoryService;
    private final CancellationPolicyResolver policyResolver;
    private final Clock clock;
    private static final Duration HOLD_DURATION = Duration.ofMinutes(20);

    //기존 홀드 재사용 여부를 조회한다, 만들어져 있던 가예약이 있다면 map이 실행된다.
    public Optional<ReservationInitResponse> reuseHold(String dedupKey){
        Optional<Reservation> found = reservationRepository.findByDedupKey(dedupKey);
        if(found.isEmpty()) return Optional.empty();

        Reservation reservation = found.get();
        //스케줄러는 5분 주기라 만료 시각은 지났지만 아직 만료 처리 안된 홀드가 항상 존재한다.
        //그대로 돌려주면 사용자가 결제한 직후 스케줄러가 만료 처리해, 결제 이후 자동 환불 처리된다.(/confirm)
        if(reservation.isHoldExpired(LocalDateTime.now(clock))) {
            expireHold(reservation.getId()); //같은 트랜잭션에서 회수 - 락 + isPending 재확인 포함
            return Optional.empty();         //createHold 로 새로 만들게 한다.
        }

        Payment payment = paymentRepository.findFirstByReservation_IdAndStatusOrderByIdDesc(reservation.getId(), PaymentStatus.READY)
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .status(PaymentStatus.READY)
                        .price(reservation.getTotalPrice())
                        .reservation(reservation)
                        .paymentId(makePaymentId(reservation.getRoom().getId()))
                        .build()));
        return Optional.of(toInitResponse(payment.getPaymentId(), reservation, reservation.getUser()));
    }

    public ReservationInitResponse createHold(ReservationInitRequest request, Long userId, String dedupKey) {
        // 락 없는 구간
        LocalDate checkIn = request.checkDate().checkIn();
        LocalDate checkOut = request.checkDate().checkOut();
        LocalDate today = LocalDate.now(clock);

        //몇 박인지 계산, 1보다 낮거나 체크인 날짜가 오늘 이전이면 불가능
        long nightDays = request.checkDate().stayDays();
        if(nightDays < 1 || checkIn.isBefore(today)){
            throw new ReservationException(ErrorCode.INVALID_CHECK_DATE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Room room = roomRepository.findByIdWithAccommodation(request.roomId())
                .orElseThrow(() -> new RoomException(ErrorCode.ROOM_NOT_FOUND));

        CheckDateTime checkDateTime = new CheckDateTime(LocalDateTime.of(checkIn, room.getCheckTime().getCheckIn()),
                LocalDateTime.of(checkOut, room.getCheckTime().getCheckOut()));
        RefundPolicySnapshot policy = RefundPolicySnapshot.of(policyResolver.resolve(room));

        String paymentId = makePaymentId(room.getId());

        // 여기서부터 락 시작, 예약 불가능하면 예외를 던짐
        int price = roomInventoryService.checkRoomInventoryAvailability(nightDays, room, checkIn, checkOut);

        Reservation reservation = Reservation.builder()
                .room(room)
                .user(user)
                .checkDateTime(checkDateTime)
                .refundPolicy(policy)
                .totalPrice(price)
                .dedupKey(dedupKey)
                .holdExpiresAt(LocalDateTime.now(clock).plus(HOLD_DURATION))
                .build();

        // 결제하기 중복 요청이 들어온다면 여기서 dedup key unique 위반에 걸려 에러가 터진다.
        reservationRepository.save(reservation);

        paymentRepository.save(Payment.builder()
                .status(PaymentStatus.READY)
                .price(price)
                .reservation(reservation)
                .paymentId(paymentId)
                .build());

        return toInitResponse(paymentId, reservation, user);
    }

    //스케쥴러 호출 메서드, 시간이 지난 예약을 만료 처리한다.
    public boolean expireHold(Long reservationId){
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId).orElse(null);

        //락을 잡은 사이 예약이 확정됐을 수도 있으므로 재확인
        if(reservation == null || !reservation.isPending()) return false;

        reservation.expireReservation();
        roomInventoryService.increaseRoomInventoryCount(reservation.getCheckDateTime(), reservation.getRoom());
        return true;
    }

    private String makePaymentId(Long roomId){
        return "reserve_" + UUID.randomUUID() + "_" + roomId;
    }


    private ReservationInitResponse toInitResponse(String paymentId, Reservation reservation, User user){
        return new ReservationInitResponse(paymentId, reservation.getId(), reservation.getTotalPrice(), user.getName(), user.getPhoneNumber(), user.getEmail());
    }
}
