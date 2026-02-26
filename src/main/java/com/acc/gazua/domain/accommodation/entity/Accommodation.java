package com.acc.gazua.domain.accommodation.entity;

import com.acc.gazua.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


//필드 추가 필요
@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Accommodation {

    @Id @GeneratedValue
    @Column(name = "accommodation_id")
    private Long id;

    @OneToOne
    private User host; //숙소 주인

    private String name; //숙소 이름

    private String description; //숙소 소개 글
}
