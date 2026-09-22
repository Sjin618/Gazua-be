package com.acc.gazua.domain.roominventory.entity;

import com.acc.gazua.domain.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "room_inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_inventory_room_date",
                columnNames = {"room_id", "date"}))
@Entity
public class RoomInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDate date;

    private Integer price;

    private Integer totalCount;

    private Integer availableCount;

    public void decreaseAvailableCount(int days){
        this.availableCount -= days;
    }

    public void increaseAvailableCount(int days){this.availableCount += days;}
}
