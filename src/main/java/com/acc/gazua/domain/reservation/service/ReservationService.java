package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.Payment.entity.Payment;
import com.acc.gazua.domain.Payment.entity.PaymentStatus;
import com.acc.gazua.domain.Payment.repository.PaymentRepository;
import com.acc.gazua.domain.accommodation.entity.CheckDate;
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
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional
@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;

    public ReservationInitResponse getAndCreateReservation(ReservationInitRequest request,
                                                           Long userId){
        Long roomId = request.roomId();;
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
            if(inventory.getStock() < 1){
                //재고 없음 에러 던지기
                throw new IllegalStateException("재고가 부족합니다.");
            }
        }

        //재고 차감
        //결제해야 할 금액 계산
        int price = calculatePriceAndDecreaseStock(inventories);

        //가예약 생성 후 저장
        Room referenceRoom = roomRepository.getReferenceById(roomId);
        Reservation reservation = Reservation.builder()
                .room(referenceRoom)
                .user(user)
                .status(ReservationStatus.PENDING)
                .checkDate(request.checkDate())
                .build();

        reservationRepository.save(reservation);

        //결제 Entity 생성 후 저장
        String merchantId = makeMerchantId(roomId);

        Payment payment = Payment.builder()
                .status(PaymentStatus.READY)
                .price(price)
                .reservation(reservation)
                .merchantId(merchantId)
                .build();

        paymentRepository.save(payment);

        return new ReservationInitResponse(merchantId,
                reservation.getId(),
                price,
                user.getName(),
                user.getPhoneNumber(),
                user.getEmail()
        );
    }

    //실제 결제 금액과 DB 금액 대조 로직(예약 확정 전 마지막 단계)
    public boolean verifyAndConfirmReservation(String merchantId, Long reservationId, int paidTotal) {
        Payment payment = paymentRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 건입니다."));
        //결제 정보가 맞는지 검증
        if (payment.getPrice() != paidTotal){
            return false;
        }
        //맞으면 예약과 결제의 상태 변화
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_FOUND));
        reservation.confirmReservation();
        payment.confirmPayment();

        return true;
    }

    //예약 실패 및 환불 로직
    public void failReservationAndPayment(String merchantId, Long reservationId) {
        Payment payment = paymentRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 건입니다."));

        Reservation reservation = reservationRepository.findByIdWithRoom(reservationId)
                .orElseThrow(() -> new ReservationException(ErrorCode.RESERVATION_NOT_FOUND));

        //예약과 결제 환불 상태로 변화
        payment.refundPayment();
        reservation.cancelReservation();

        //결제 하려고 차감했었던 RoomInventory 재고 1씩 증가
        CheckDate checkDate = reservation.getCheckDate();
        Room room = reservation.getRoom();

        List<RoomInventory> inventories = roomInventoryRepository.findInventoriesForUpdate(room.getId(),
                checkDate.getCheckIn(),
                checkDate.getCheckOut());
        inventories.forEach(inventory -> inventory.increaseStock(1));
    }

    public void getReservationList(CustomUserDetails user,
                                   ReservationStatus status) {

    }

    private int calculatePriceAndDecreaseStock(List<RoomInventory> inventories){
        int price = 0;
        for (RoomInventory inventory : inventories) {
            inventory.decreaseStock(1);
            price += inventory.getPrice();
        }
        return price;
    }

    private String makeMerchantId(Long roomId){
        return "reserve_" + UUID.randomUUID() + "_" + roomId;
    }
}
