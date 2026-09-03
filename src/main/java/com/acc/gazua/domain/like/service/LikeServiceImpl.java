package com.acc.gazua.domain.like.service;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.exception.AccommodationException;
import com.acc.gazua.domain.accommodation.repository.AccommodationRepository;
import com.acc.gazua.domain.like.dto.LikeAccommodationDto;
import com.acc.gazua.domain.like.entity.Like;
import com.acc.gazua.domain.like.repository.LikeRepository;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.JsonCursorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
    public void createLike(Long accId, Long userId) {
        if(likeRepository.existsByAccommodationIdAndUserId(accId,userId)){
            return;
        }
        User user = userRepository.getReferenceById(userId);

        Accommodation accommodation = accommodationRepository.findById(accId)
                .orElseThrow(() -> new AccommodationException(ErrorCode.ACCOMMODATION_NOT_FOUND));

        Like like = Like.builder()
                .accommodation(accommodation)
                .user(user)
                .build();

        likeRepository.save(like);
    }

    //좋아요 제거
    @Override
    public void deleteLike(Long accId, Long userId) {
        likeRepository.directDelete(accId,userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<LikeAccommodationDto> getLikeAccommodation(Long userId, String cursor, int size){
        CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
        Long cursorId = customCursor != null ? customCursor.cursorId() : Long.MAX_VALUE;

        PageRequest pageRequest = PageRequest.of(0, size);

        Slice<LikeAccommodationDto> likeList = likeRepository.getLikeList(userId, cursorId, pageRequest);

        if(likeList.isEmpty()){
            return CursorPage.empty();
        }

        List<LikeAccommodationDto> list = likeList.getContent();

        LikeAccommodationDto lastItem = list.get(list.size() - 1);
        String encodedCursor = jsonCursorUtils.serialize(new CustomCursor(null, lastItem.getLikeId()));

        return new CursorPage<>(list, encodedCursor, likeList.hasNext());
    }

}
