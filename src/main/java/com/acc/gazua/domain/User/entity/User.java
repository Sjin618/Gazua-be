package com.acc.gazua.domain.User.entity;

import com.acc.gazua.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity(name = "users")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id @GeneratedValue
    @Column(name = "user_id")
    private Long id; //user_id

    @Column(nullable = false, unique = true)
    private String email; //ID 로 사용, 중복 불가능

    private String password;

    private String image; //회원가입 시 기본 이미지 사용, 정보 수정에서 이미지 설정

    private String nickname; //닉네임 중복 가능

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
}
