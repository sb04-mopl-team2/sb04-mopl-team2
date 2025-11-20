package com.codeit.mopl.domain.user.service;

import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.request.UserLockUpdateRequest;
import com.codeit.mopl.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.mopl.domain.user.dto.request.UserUpdateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.UserEmailAlreadyExistsException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.s3.S3Storage;
import com.codeit.mopl.security.jwt.JwtRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private S3Storage s3Storage;

    @Mock
    private JwtRegistry jwtRegistry;

    @InjectMocks
    private UserService userService;

    @DisplayName("이메일, 비밀번호, 이름을 정확히 입력하였고 가입되지 않은 이메일로 회원가입 시 회원가입이 정상적으로 완료한다.")
    @Test
    void createUserShouldSucceedWhenValidateRequest() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "test@example.com", "testPassword");
        User user = new User("test@example.com","encodedPassword","test");
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@example.com","test",null, Role.USER, false);
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode("testPassword")).willReturn("encodedPassword");
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(userDto);

        // when
        UserDto createdUserDto = userService.create(request);

        // then
        assertEquals(createdUserDto.email(), "test@example.com");
        assertEquals(createdUserDto.role(), Role.USER);
    }

    @DisplayName("이메일, 비밀번호, 이름을 정확히 입력하였지만 이미 가입된 이메일로 회원가입을 시도하는 경우 회원가입에 실패한다.")
    @Test
    void createUserShouldFailWhenSingUpTryExistsEmail() {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "test@example.com", "testPassword");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        UserEmailAlreadyExistsException exception = assertThrows(UserEmailAlreadyExistsException.class, () -> userService.create(request));

        assertEquals("해당 이메일로 가입된 아이디가 이미 존재합니다.", exception.getErrorCode().getMessage());
    }

    @DisplayName("올바른 유저 아이디를 조회하면 정상적으로 해당 유저의 상세정보가 표시된다.")
    @Test
    void checkUserInformationShouldSucceedWhenValidateUserId() {
        // given
        User findUser = new User("test@example.com","testPassword","test");
        UUID uuid = UUID.randomUUID();
        UserDto findUserDto = new UserDto(uuid, LocalDateTime.now(), "test@example.com","test",null, Role.USER, false);
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.of(findUser));
        given(userMapper.toDto(findUser)).willReturn(findUserDto);

        // when
        UserDto responseUserDto = userService.findUser(uuid);

        // then
        assertEquals(responseUserDto.email(), findUser.getEmail());
        assertEquals(responseUserDto.role(), Role.USER);
        assertEquals(responseUserDto.id(), uuid);
        assertEquals(responseUserDto.name(), findUser.getName());
    }

    @DisplayName("존재하지 않는 유저ID로 유저 정보를 조회하면 404 NOT_FOUND 예외가 발생한다")
    @Test
    void checkUserInformationShouldFailWhenUserIdNotFound() {
        // given
        UUID uuid = UUID.randomUUID();
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.findUser(uuid);
        });

        assertEquals("유저를 찾을 수 없습니다.", exception.getErrorCode().getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getErrorCode().getStatus());
    }

    @DisplayName("올바른 유저 ID와 올바른 ROLE이 주어지면 유저의 권한이 성공적으로 변경된다")
    @Test
    void updateRoleShouldSucceedWhenValidUserIdAndValidRole() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
        User findUser = new User("test@example.com","password","test");  // new User는 Default Role.USER
        given(userRepository.findById(userId)).willReturn(Optional.of(findUser));

        // when
        userService.updateRole(userId, request);

        // when
        assertEquals(Role.ADMIN, findUser.getRole());
    }

    @DisplayName("올바른 유저 ID와 Request가 주어졌을 때 업데이트의 성공한다")
    @Test
    void updateUserLockShouldSucceedWhenValidUserIdAndValidRequest() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);
        User findUser = new User("test@example.com","password","test");
        given(userRepository.findById(userId)).willReturn(Optional.of(findUser));

        // when
        userService.updateLock(userId, request);

        // then
        assertEquals(true, findUser.isLocked());
    }

    @DisplayName("유저의 이름 혹은 프로필 이미지가 주어졌을때 null이 아닌 값을 추가하거나 변경한다.")
    @Test
    void updateUserProfileShouldSucceedWithUsernameOrProfileImage() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        User user = new User("test@test.com","password","beforeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName.jpg",MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        userService.updateProfile(userId,request,profile);

        verify(s3Storage).upload(any(MultipartFile.class),anyString());
        assertEquals("changeName",user.getName());
        assertNotNull(user.getProfileImageUrl());
    }

    @DisplayName("존재하지 않는 유저 ID가 주어지면 404 NOT_FOUND가 발생한다.")
    @Test
    void updateUserProfileShouldFailWhenUserIdNotFound() {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName",MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.updateProfile(userId,request,profile);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getErrorCode().getStatus());
    }
}
