package com.acc.gazua.domain.like.service;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.like.dto.LikeAccommodationDto;
import com.acc.gazua.domain.like.entity.Like;
import com.acc.gazua.domain.like.exception.LikeException;
import com.acc.gazua.domain.like.repository.LikeRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.JsonCursorUtils;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService{

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final AccommodationRepository accommodationRepository;

    private final JsonCursorUtils jsonCursorUtils;

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

    public CursorPage<LikeAccommodationDto> getLikeAccommodation(CustomUserDetails user, String cursor, int size){
        Long userId = user.getUserId();
        CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
        Long cursorId = customCursor != null ? customCursor.cursorId() : Long.MAX_VALUE;

        PageRequest pageRequest = PageRequest.of(0, size);

        Slice<LikeAccommodationDto> likeList = likeRepository.getLikeList(userId, cursorId, pageRequest);

        if(likeList.isEmpty()){
            return new CursorPage<>(null,null,false);
        }

        List<LikeAccommodationDto> list = likeList.getContent();

        LikeAccommodationDto lastItem = list.get(list.size() - 1);
        String encodedCursor = jsonCursorUtils.serialize(new CustomCursor(null, lastItem.getId()));

        return new CursorPage<>(list, encodedCursor, likeList.hasNext());
    }

}
