package com.acc.gazua.domain.room.controller;

import com.acc.gazua.domain.accommodation.entity.CheckDate;
import com.acc.gazua.domain.room.dto.RoomGetResponse;
import com.acc.gazua.domain.room.service.RoomService;
import com.acc.gazua.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RoomController {
    private final RoomService roomService;

    //방 목록 조회
    //기간, 인원은 필수로 들어와야 함
    @GetMapping("/accommodation/{accId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomGetResponse>>> getRooms(@PathVariable("accId") Long accId,
                                                                       @Valid @ModelAttribute CheckDate checkDate,
                                                                       @RequestParam Integer personal) {
        List<RoomGetResponse> rooms = roomService.getRooms(accId, checkDate, personal);

        return ResponseEntity.ok(ApiResponse.success(rooms, "방 목록 조회 성공"));
    }

    //방 사진 전체 조회
    @GetMapping("/rooms/{roomId}/images")
    public ResponseEntity<ApiResponse<List<String>>> getRoomImages(@PathVariable("roomId") Long roomId) {
        List<String> images = roomService.getRoomImages(roomId);

        return ResponseEntity.ok(ApiResponse.success(images, "방 사진 조회 성공"));
    }
}
