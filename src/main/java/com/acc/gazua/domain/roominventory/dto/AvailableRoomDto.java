package com.acc.gazua.domain.roominventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableRoomDto {
    private Long accId;
    private Long roomId;
    private Long totalPrice;
}
