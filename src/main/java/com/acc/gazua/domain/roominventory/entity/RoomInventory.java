package com.acc.gazua.domain.roominventory.entity;

import com.acc.gazua.domain.room.entity.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class RoomInventory {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDate date;

    private Integer price;

    private Integer stock;
}
