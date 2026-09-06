package com.acc.gazua.domain.room.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.global.entity.CheckTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation;

    private Integer standardCapacity; //기준인원

    private Integer maxCapacity; //최대인원

    private String name; //방 이름

    @Embedded
    private CheckTime checkTime; //체크인, 체크아웃 시간

    private Integer amount; //방 개수

    private String thumbnailUrl; //방 썸네일 이미지
    private Integer imageCount; //이미지 개수 반정규화

    public void changeThumbnail(String imageUrl){
        this.thumbnailUrl = imageUrl;
    }

    public void setImageCount(int imageCount){ this.imageCount = imageCount;}
}
