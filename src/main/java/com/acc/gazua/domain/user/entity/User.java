package com.acc.gazua.domain.user.entity;

import com.acc.gazua.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id; //user_id

    @Column(nullable = false, unique = true)
    private String email; //ID 로 사용, 중복 불가능

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String profileImage; //회원가입 시 기본 이미지 사용, 정보 수정에서 이미지 설정

    @Column(nullable = false)
    private String nickname; //닉네임 중복 가능

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender; //성별

    @Column(nullable = false)
    private String phoneNumber; //전화 번호

    @Column(nullable = false)
    private LocalDate birthDate; //생년월일

    @Column(nullable = false)
    private String name; //본명

    public boolean equalsId(Long requestId){
        return Objects.equals(this.id, requestId);
    }

    public void changeName(String name){
        this.name = name;
    }

    public void changeNickname(String nickname){
        this.nickname = nickname;
    }

    public void changePhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }

    public void changeBirthDate(LocalDate birthDate){
        this.birthDate = birthDate;
    }

    public void changeGender(Gender gender){
        this.gender = gender;
    }

    public void changeProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
