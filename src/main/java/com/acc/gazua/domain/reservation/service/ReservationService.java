package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.payment.entity.Payment;
import com.acc.gazua.domain.payment.entity.PaymentStatus;
import com.acc.gazua.domain.payment.repository.PaymentRepository;
import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.reservation.dto.ReservationGetResponse;
import com.acc.gazua.domain.reservation.dto.ReservationInitRequest;
import com.acc.gazua.domain.reservation.dto.ReservationInitResponse;
import com.acc.gazua.domain.reservation.entity.Reservation;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.domain.room.entity.Room;
import com.acc.gazua.domain.room.repository.RoomRepository;
import com.acc.gazua.domain.roominventory.entity.RoomInventory;
import com.acc.gazua.domain.roominventory.repository.RoomInventoryRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.entity.CheckDateTime;
import com.acc.gazua.global.page.JsonCursorUtils;
import com.acc.gazua.global.portone.PortOneClient;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.acc.gazua.global.portone.PortOneClient.*;

@RequiredArgsConstructor
@Transactional
@Service
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;

    private final JsonCursorUtils jsonCursorUtils;

    public ReservationInitResponse getAndCreateReservation(ReservationInitRequest request,
                                                           Long userId){
        Long roomId = request.roomId();
        //유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        LocalDate checkIn = request.checkDate().getCheckIn();
        LocalDate checkOut = request.checkDate().getCheckOut();
        long nightDays = ChronoUnit.DAYS.between(checkIn,checkOut);
        //재고 조회
        List<RoomInventory> inventories = roomInventoryRepository.findInventoriesForUpdate(roomId, checkIn, checkOut);

        //재고 개수와 박 일수가 맞지 않으면 에러
        if(inventories.size() != nightDays){
            //날짜가 맞지 않다는 에러 던지기
            throw new ReservationException(ErrorCode.RESERVATION_NOT_AVAILABLE);
        }

        //모든 날의 재고가 1 이상인지 확인
        for (RoomInventory inventory : inventories) {
            if(inventory.getAvailableCount() < 1){
                //재고 없음 에러 던지기
                throw new IllegalStateException("재고가 부족합니다.");
            }
        }

        //재고 차감
        //결제해야 할 금액 계산
        int price = calculatePriceAndDecreaseStock(inventories);

        //가예약 생성 후 저장
        Room referenceRoom = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 방입니다."));

        LocalDateTime checkInDateTime = LocalDateTime.of(checkIn, referenceRoom.getCheckTime().getCheckIn());
        LocalDateTime checkOutDateTime = LocalDateTime.of(checkOut, referenceRoom.getCheckTime().getCheckOut());

        Reservation reservation = Reservation.builder()
                .room(referenceRoom)
                .user(user)
                .status(ReservationStatus.PENDING)
                .checkDateTime(new CheckDateTime(checkInDateTime, checkOutDateTime))
                .build();

        reservationRepository.save(reservation);

        //결제 Entity 생성 후 저장
        String paymentId = makePaymentId(roomId);

        Payment payment = Payment.builder()
                .status(PaymentStatus.READY)
                .price(price)
                .reservation(reservation)
                .paymentId(paymentId)
                .build();

        paymentRepository.save(payment);

        return new ReservationInitResponse(paymentId,
                reservation.getId(),
                price,
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail()
        );
    }

    //실제 결제 금액과 DB 금액 대조 로직(예약 확정 전 마지막 단계) - 결제 확정
    public PaymentSyncResult applyPaid(PortOnePaymentSnapshot snapshot) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(snapshot.paymentId()).orElse(null);

        if(payment == null){
            log.warn("DB에 없는 결제 건 동기화 시도 - paymentId={}", snapshot.paymentId());
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //이미 결제 확정된 건은 재시도 하지 않음
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentSyncResult.ALREADY_DONE;
        }
        if (payment.getStatus() == PaymentStatus.REFUND) {
            // 순서 역전: Cancelled가 먼저 처리된 뒤 Paid가 뒤늦게 도착
            log.warn("이미 환불된 건에 결제 확정 시도 - 무시 - paymentId={}", snapshot.paymentId());
            return PaymentSyncResult.ALREADY_DONE;
        }

        if (payment.getPrice().longValue() != snapshot.paidTotal()) {
            log.warn("결제 금액 불일치 - paymentId={}, expected={}, actual={}",
                    snapshot.paymentId(), payment.getPrice(), snapshot.paidTotal());
            return PaymentSyncResult.AMOUNT_MISMATCH;
        }

        payment.confirmPayment(toLocalDateTIme(snapshot.paidAt()), snapshot.payMethod(), snapshot.pgProvider());
        payment.getReservation().confirmReservation();

        log.info("결제 확정 - paymentId={}, reservationId={}",
                snapshot.paymentId(), payment.getReservation().getId());
        return PaymentSyncResult.CONFIRMED;
    }

    //예약 취소 및 환불 로직 — READY(미결제 취소)든 COMPLETED(환불)든 재고는 동일하게 복구
    public PaymentSyncResult applyCancelled(String paymentId, Instant cancelledAt, String reason) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);

        if(payment == null){
            log.warn("DB에 없는 결제 건 취소 시도 - paymentId={}", paymentId);
            return PaymentSyncResult.UNKNOWN_PAYMENT;
        }

        //이미 환불된 결제 재요청 막기
        if (payment.getStatus() == PaymentStatus.REFUND) {
            return PaymentSyncResult.ALREADY_DONE;
        }

        Reservation reservation = payment.getReservation();
        payment.refundPayment(toLocalDateTIme(cancelledAt), reason);
        reservation.cancelReservation();
        increaseRoomInventoryCount(reservation.getCheckDateTime(), reservation.getRoom());

        log.info("예약 취소 및 재고 복구 - paymentId={}, reservationId={}, reason={}",
                paymentId, reservation.getId(), reason);
        return PaymentSyncResult.CANCELLED;
    }



    public CursorPage<ReservationGetResponse> getReservationList(CustomUserDetails user,
                                                                 ReservationStatus status,
                                                                 String cursor,
                                                                 int size) {
        CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
        Long cursorId = customCursor != null ? customCursor.cursorId() : Long.MAX_VALUE;

        PageRequest pageRequest = PageRequest.of(0, size);

        Slice<ReservationGetResponse> reservationList = null;
                //reservationRepository.findByReservationStatus(status, cursorId, user.getUserId(), pageRequest);

        if(reservationList.isEmpty()){
            return new CursorPage<>(null,null,false);
        }

        List<ReservationGetResponse> list = reservationList.getContent();

        ReservationGetResponse lastItem = list.get(list.size() - 1);
        String encodedCursor = jsonCursorUtils.serialize(new CustomCursor(null, lastItem.getId()));

        return new CursorPage<>(list, encodedCursor, reservationList.hasNext());

    }

    private int calculatePriceAndDecreaseStock(List<RoomInventory> inventories){
        int price = 0;
        for (RoomInventory inventory : inventories) {
            inventory.decreaseAvailableCount(1);
            price += inventory.getPrice();
        }
        return price;
    }

    private String makePaymentId(Long roomId){
        return "reserve_" + UUID.randomUUID() + "_" + roomId;
    }

    private void increaseRoomInventoryCount(CheckDateTime checkDateTime,Room room){
        List<RoomInventory> inventories = roomInventoryRepository.findInventoriesForUpdate(room.getId(),
                checkDateTime.getCheckInDateTime().toLocalDate(),
                checkDateTime.getCheckOutDateTime().toLocalDate());
        inventories.forEach(inventory -> inventory.increaseAvailableCount(1));
    }

    private LocalDateTime toLocalDateTIme(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
    }
}
