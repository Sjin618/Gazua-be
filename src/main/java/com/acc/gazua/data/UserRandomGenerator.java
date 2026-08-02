package com.acc.gazua.data;

import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.entity.Gender;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UserRandomGenerator {

    public static UserSignUpRequest createRandomUserHost(){
        //중복 방지 고유 식별자 생성
        String uniqueId = UUID.randomUUID().toString();

        //랜덤 이메일 생성
        String email = "host_" + uniqueId.substring(0, 8);

        //비밀번호, 닉네임, 이름 생성
        String password = "Password123!";
        String nickname = "호스트_" + uniqueId;
        String name = "호스트" + uniqueId.substring(0,4);

        //랜덤 성별 생성
        Gender[] genders = Gender.values();
        Gender gender = genders[ThreadLocalRandom.current().nextInt(genders.length)];

        //랜덤 전화번호 생성
        int randomSuffix = ThreadLocalRandom.current().nextInt(10000000, 100000000);
        String phoneNumber = "010" + randomSuffix;


        //랜덤 생년월일 생성
        int year =ThreadLocalRandom.current().nextInt(1970,2001);
        int month = ThreadLocalRandom.current().nextInt(1,13);
        int day = ThreadLocalRandom.current().nextInt(1,29);
        LocalDate birthDate = LocalDate.of(year, month, day);

        return new UserSignUpRequest(
                email,
                password,
                nickname,
                gender,
                phoneNumber,
                birthDate,
                name
        );
    }
}
