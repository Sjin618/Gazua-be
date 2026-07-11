package com.acc.gazua.domain.accommodation.service;

import com.acc.gazua.domain.accommodation.dto.AccommodationRegisterRequest;
import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.entity.CheckTime;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationService {
    private final AccommodationRepository accommodationRepository;
    private final UserRepository userRepository;

    public void searchAccommodation(String region,
                                    Integer personal,
                                    CheckTime checkTime,
                                    String sortType){

    }

    public void registerAccommodation(AccommodationRegisterRequest registerRequest){
        User host = userRepository.findById(registerRequest.getHostId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Accommodation accommodation = Accommodation.builder()
                .host(host)
                .name(registerRequest.getName())
                .type(registerRequest.getType())
                .address(registerRequest.getAddress())
                .build();

        accommodationRepository.save(accommodation);
    }
}
