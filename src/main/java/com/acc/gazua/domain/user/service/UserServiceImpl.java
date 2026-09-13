package com.acc.gazua.domain.user.service;


import com.acc.gazua.domain.user.dto.UserDetailResponse;
import com.acc.gazua.domain.user.dto.UserSignUpRequest;
import com.acc.gazua.domain.user.dto.UserSummaryResponse;
import com.acc.gazua.domain.user.dto.UserUpdateRequest;
import com.acc.gazua.domain.user.entity.Role;
import com.acc.gazua.domain.user.entity.User;
import com.acc.gazua.domain.user.exception.UserException;
import com.acc.gazua.domain.user.repository.UserRepository;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Value("${app.user.default-profile-image}")
    private final String DEFAULT_PROFILE_IMAGE;

    @Override
    public void signup(UserSignUpRequest request){
        //이메일 중복 체크
        if(userRepository.existsByEmail(request.email())){
            throw new UserException(ErrorCode.DUPLICATE_EMAIL);
        }
        //패스워드 인코딩
        String encodedPassword = passwordEncoder.encode(request.password());
        //회원 정보 설정
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .profileImage(null) //기본 이미지로 수정
                .gender(request.gender())
                .nickname(request.nickname())
                .role(Role.USER)
                .birthDate(request.birthDate())
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .build();

        //회원 저장
        //회원가입 중복 실행 시 에러 메시지 일관되게 반환
        //나중에 user에 unique 제약이 추가되면, 에러를 필드에 따라 상세하게 분리해야 함 예를 들어 휴대폰 번호 중복, 닉네임 중복 등등
        try {
            userRepository.save(user);
        }catch(DataIntegrityViolationException e){
            log.warn("회원가입 중복실행 감지");
            throw new UserException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Override
    public void withdraw(Long userId){
        User user = findActiveUser(userId);

        user.delete(LocalDateTime.now(clock));
        //유저와 관련된 모든 refresh token DB에서 삭제
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getMySummary(Long userId){
        User user = findActiveUser(userId);

        return UserSummaryResponse.from(user, DEFAULT_PROFILE_IMAGE);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getMyDetail(Long userId){
        User user = findActiveUser(userId);

        return UserDetailResponse.from(user, DEFAULT_PROFILE_IMAGE);
    }

    @Override
    public void updateMe(UserUpdateRequest request, Long userId) {
        User user = findActiveUser(userId);

        if(request.name() != null) user.changeName(request.name());
        if(request.nickname() != null) user.changeNickname(request.nickname());
        if(request.phoneNumber() != null) user.changePhoneNumber(request.phoneNumber());
        if(request.birthDate() != null ) user.changeBirthDate(request.birthDate());
        if(request.gender() != null ) user.changeGender(request.gender());

    }

    @Override
    public void updateMyProfileImage(MultipartFile newImage, Long userId) {
        User user = findActiveUser(userId);

        if (newImage == null || newImage.isEmpty()) {
            throw new UserException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String oldImageUrl = user.getProfileImage();
        // 이미지 저장소에 새 이미지 저장
        //String newProfileImageUrl = 파일저장소.save(newProfileImage, userId);
        //user.changeProfileImage(newProfileImageUrl);

        //deleteAfterCommit(oldImageUrl); //null 이면 지울게 없음
    }

    @Override
    public void deleteMyProfileImage(Long userId) {
        User user = findActiveUser(userId);

        String oldImage = user.getProfileImage();
        user.changeProfileImage(null); // null = 기본이미지

        //deleteAfterCommit(oldImageUrl); //null 이면 지울게 없음
    }

    private User findActiveUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if(user.isDeleted()){
            throw new UserException(ErrorCode.DELETED_USER);
        }

        return user;
    }


}
