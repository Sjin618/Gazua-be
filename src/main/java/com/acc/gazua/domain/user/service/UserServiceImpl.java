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
import com.acc.gazua.global.exception.FileException;
import com.acc.gazua.global.security.auth.repository.RefreshTokenRepository;
import com.acc.gazua.global.validation.ImageValidator;
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
    private final ImageValidator imageValidator;
    private final Clock clock;

    @Value("${app.user.default-profile-image}")
    private final String defaultProfileImage;

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
                .profileImage(null) //null 은 기본 이미지
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

        return UserSummaryResponse.from(user, defaultProfileImage);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getMyDetail(Long userId){
        User user = findActiveUser(userId);

        return UserDetailResponse.from(user, defaultProfileImage);
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

    /**
     * polyglot 파일이 통과하는건 매직 바이트 검증 방식의 한계
     * PNG 서명 뒤에 붙은 HTML이나 파일이 통과할 수 있음
     * 저장소를 구현할 때 UUID 파일명과 nosniff 헤더로 막기
     * @param newImage
     * @param userId
     */
    @Override
    public void updateMyProfileImage(MultipartFile newImage, Long userId) {
        User user = findActiveUser(userId);
        String detectedType = imageValidator.validate(newImage);

        String oldImageUrl = user.getProfileImage();
        // 이미지 저장소에 새 이미지 저장
        //String newImageUrl = fileStorage.save(newImage, detectedType, userId);
        //user.changeProfileImage(newImageUrl);

        //deleteAfterCommit(oldImageUrl); //null 이면 지울게 없음

        throw new FileException(ErrorCode.NOT_IMPLEMENTED); // 저장소 확정 후 제거
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
