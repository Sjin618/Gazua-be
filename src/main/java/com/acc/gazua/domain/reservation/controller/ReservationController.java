package com.acc.gazua.domain.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation")
public class ReservationController {
    //가예약 만들기 or 가져오기
    //어떤 방에 대한 예약인지
    //어떤 사용자가 예약하는지
    //언제 체크인 하고 체크아웃 하는지
    @PostMapping
    public ResponseEntity<Void> getOrCreateReservation(){

        return ResponseEntity.ok(null);
    }

    //예약 내역(리스트) 가져오기

    //상세 예약 정보 가져오기
}
