package com.acc.gazua.domain.like.service;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.exception.AccommodationException;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.like.entity.Like;
import com.acc.gazua.domain.like.exception.LikeException;
import com.acc.gazua.domain.like.repository.LikeRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService{

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final AccommodationRepository accommodationRepository;

    //좋아요 생성
    @Override
    public void createLike(Long accId, CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        if(likeRepository.existsByAccommodationIdAndUserId(accId,userId)){
            throw new LikeException(ErrorCode.ALREADY_LIKE);
        }
        User user = userRepository.getReferenceById(userId);

        Accommodation accommodation = accommodationRepository.getReferenceById(accId);

        Like like = Like.builder()
                .accommodation(accommodation)
                .user(user)
                .build();

        likeRepository.save(like);
    }

    //좋아요 제거
    @Override
    public void deleteLike(Long accId, CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        likeRepository.directDelete(accId,userId);
    }
}
