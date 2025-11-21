package com.codeit.mopl.domain.user.service;

import com.codeit.mopl.domain.user.dto.request.*;
import com.codeit.mopl.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.ImageContentType;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.NotImageContentException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserEmailAlreadyExistsException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.s3.S3Storage;
import com.codeit.mopl.security.jwt.JwtRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publisher;
    private final JwtRegistry jwtRegistry;
    private final S3Storage s3Storage;

    @Transactional
    public UserDto create(UserCreateRequest request) {
        log.info("[사용자 관리] 유저 생성 실행 email = {}",request.email());
        validateEmail(request.email());
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.email(), encodedPassword, request.name());
        userRepository.save(user);
        log.info("[사용자 관리] 유저 생성 완료 userEmail = {}", user.getEmail());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto findByEmail(String email) {
        log.info("[사용자 관리] 유저 찾기 실행 Email = {}", email);
        User findUser = findUserByEmail(email);
        UserDto userDto = userMapper.toDto(findUser);
        log.info("[사용자 관리] 유저 찾기 완료 email = {}, userId = {}", email, userDto.id());
        return userDto;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("[사용자 관리] 유저 비밀번호 변경 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        String encodedNewPassword = passwordEncoder.encode(request.password());
        findUser.updatePassword(encodedNewPassword);
        log.info("[사용자 관리] 유저 비밀번호 변경 완료 userId = {}", userId);
    }

    public UserDto findUser(UUID userId) {
        log.info("[사용자 관리] 회원 정보 조회 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        UserDto userDto = userMapper.toDto(findUser);
        log.info("[사용자 관리] 회원 정보 조회 성공 userId = {}", userDto.id());
        return userDto;
    }

    @Transactional
    public void updateRole(UUID userId, UserRoleUpdateRequest request) {
        log.info("[사용자 관리] 회원 권한 수정 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        log.debug("[사용자 관리] 회원 권한 수정 {} -> {}", findUser.getRole(), request.role());
        findUser.updateRole(request.role());

        removeToken(findUser.getId());

        // **추후 이벤트가 정해지면 수정하겠습니다**
        // publisher.publishEvent(new UserRoleUpdateEvent(userMapper.toDto(findUser)));
        log.info("[사용자 관리] 회원 권한 수정 완료 userId = {}, Role = {}", userId, request.role());
    }

    @Transactional
    public void updateLock(UUID userId, UserLockUpdateRequest request) {
        log.info("[사용자 관리] 회원 잠금상태 변경 동작 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        log.debug("[사용자 관리] 회원 잠금 상태 변경 {} -> {}", findUser.isLocked(), request.locked());
        findUser.updateLock(request.locked());

        removeToken(findUser.getId());

        log.info("[사용자 관리] 회원 잠금상태 수정 완료 userId = {}", userId);
    }

    @Transactional(readOnly = true)
    public CursorResponseUserDto getAllUsers(CursorRequestUserDto request) {
        log.info("[사용자 관리] 목록 조회 실행 ");
        Slice<UserDto> page = userRepository.findAllPage(request);

        if (page.getContent().isEmpty()) {
            Sort.Direction direction = request.sortDirection().equals("ASCENDING") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Slice slice = new SliceImpl<>(List.of(), PageRequest.of(0,request.limit(), direction, request.sortBy()),false);
            return CursorResponseUserDto.from(slice,null,null,0L, request.sortBy(), request.sortDirection());
        }

        UserDto lastDto = page.getContent().get(page.getContent().size() - 1);
        String lastItemCursor = null;

        if ("name".equalsIgnoreCase(request.sortBy())){
            lastItemCursor = lastDto.name();
        } else if ("email".equalsIgnoreCase(request.sortBy())) {
            lastItemCursor = lastDto.email();
        } else if ("createdAt".equalsIgnoreCase(request.sortBy())) {
            lastItemCursor = lastDto.createdAt().toString();
        } else if ("isLocked".equalsIgnoreCase(request.sortBy())) {
            lastItemCursor = lastDto.locked().toString();
        } else if ("role".equalsIgnoreCase(request.sortBy())) {
            lastItemCursor = lastDto.role().toString();
        }

        UUID lastItemAfter = lastDto.id();

        Long totalElements = userRepository.countTotalElements(request.emailLike());
        CursorResponseUserDto response = CursorResponseUserDto.from(page,lastItemCursor,lastItemAfter,totalElements, request.sortBy(), request.sortDirection());
        log.info("[사용자 관리] 목록 조회 완료 검색어 = {}, 결과수 = {}", request.emailLike(), response.data().size());
        log.debug("[사용자 관리] 목록 조회 완료 cursor = {}, after = {}", lastItemCursor, lastItemAfter);

        return response;
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UserUpdateRequest request, MultipartFile profileImage) {
        log.info("[사용자 관리] 유저 프로필 업데이트 실행 userId = {}", userId);
        User findUser = getValidUserByUserId(userId);
        Optional.ofNullable(request.name()).ifPresent(name -> {
            log.debug("[사용자 관리] 닉네임 변경 {} -> {}", findUser.getName(), request.name());
            findUser.setName(name);
        });
        Optional.ofNullable(profileImage).ifPresent(profile -> {
            validateImage(profileImage);
            log.debug("[사용자 관리] 프로필 이미지 생성");
            String extension = getFileExtension(profile.getOriginalFilename());
            String key = UUID.randomUUID() + extension;
            s3Storage.upload(profile,key);
            if (StringUtils.hasText(findUser.getProfileImageUrl())){
                log.debug("[사용자 관리] 기존 프로필 삭제 imageKey = {}", findUser.getProfileImageUrl());
                s3Storage.delete(findUser.getProfileImageUrl());
            }
            findUser.setProfileImageUrl(key);
        });
        userRepository.save(findUser);
        return userMapper.toDto(findUser);
    }

    private void validateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("[사용자 관리] 이메일 중복 가입 email = {}", email);
            throw new UserEmailAlreadyExistsException(UserErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", email));
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.info("[사용자 관리] 해당 유저를 찾을 수 없음 email = {}", email);
                    throw new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("email", email));
                });
    }

    private User getValidUserByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[사용자 관리] 해당 유저를 찾을 수 없음 userId = {}", userId);
                    throw new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId",userId));
                });
    }

    private void removeToken(UUID userId) {
        log.info("[사용자 관리] 강제 로그아웃 - token 삭제 userId = {}", userId);
        jwtRegistry.invalidateJwtInformationByUserId(userId);
    }

    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new NotImageContentException(UserErrorCode.NOT_IMAGE, Map.of("filename", String.valueOf(filename)));
        }
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new NotImageContentException(UserErrorCode.NOT_IMAGE, Map.of("filename", filename));
        }
        return filename.substring(dotIndex);
    }

    private void validateImage(MultipartFile profileImage) {
        if (profileImage.isEmpty()) {
            throw new NotImageContentException(UserErrorCode.NOT_IMAGE, Map.of("file", "empty"));
        }
        if (!ImageContentType.isImage(profileImage.getContentType())) {
            throw new NotImageContentException(UserErrorCode.NOT_IMAGE, Map.of("contentType",String.valueOf(profileImage.getContentType())));
        }
    }
}
