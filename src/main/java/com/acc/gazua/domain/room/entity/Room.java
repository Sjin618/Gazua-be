package com.acc.gazua.domain.room.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Room {
    @Id @GeneratedValue
    @Column(name = "room_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation;

    private Integer standardCapacity; //기준인원

    private Integer maxCapacity; //최대인원

    private String name; //방 이름

    private Integer amount;
}
