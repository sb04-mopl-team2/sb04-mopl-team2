package com.codeit.mopl.domain.user.service;

import com.codeit.mopl.domain.user.dto.request.*;
import com.codeit.mopl.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.NotImageContentException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
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
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        User user = new User("test@test.com","password","beforeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName.jpg",MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateProfile(userId,request,profile);

        // then
        verify(s3Storage).upload(any(MultipartFile.class),anyString());
        assertEquals("changeName",user.getName());
        assertNotNull(user.getProfileImageUrl());
    }

    @DisplayName("존재하지 않는 유저 ID가 주어지면 404 NOT_FOUND가 발생한다.")
    @Test
    void updateUserProfileShouldFailWhenUserIdNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName.jpeg",MediaType.IMAGE_JPEG_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.updateProfile(userId,request,profile);
        });

        // then
        assertEquals(HttpStatus.NOT_FOUND, exception.getErrorCode().getStatus());
    }

    @DisplayName("유저는 프로필을 업데이트할 때 프로필 이미지로 jpg, png, webp, svg만 가능하다")
    @Test
    void updateUserProfileShouldFailWhenInvalidContentType() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        User user = new User("test@test.com","password","beforeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName.gif",MediaType.IMAGE_GIF_VALUE,"image".getBytes(StandardCharsets.UTF_8));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        NotImageContentException exception = assertThrows(NotImageContentException.class, () -> {
            userService.updateProfile(userId,request,profile);
        });

        // then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getErrorCode().getStatus());
        assertEquals(MediaType.IMAGE_GIF_VALUE, exception.getDetails().get("contentType"));
    }

    @DisplayName("프로필 이미지가 존재하지만 비어있는 파일의 경우 프로필 업데이트에 실패한다")
    @Test
    void updateUserProfileShouldFailWhenEmptyFile() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        User user = new User("test@test.com","password","beforeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName.jpg",MediaType.IMAGE_JPEG_VALUE, (byte[]) null);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        NotImageContentException exception = assertThrows(NotImageContentException.class, () -> {
            userService.updateProfile(userId,request,profile);
        });

        // then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getErrorCode().getStatus());
        assertEquals("empty", exception.getDetails().get("file"));
    }

    @DisplayName("이미지의 확장자가 존재하지 않으면 실패한다")
    @Test
    void updateUserProfileShouldFailWhenNotIncludedExtension() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        User user = new User("test@test.com","password","beforeName");
        MockMultipartFile profile = new MockMultipartFile("image","originalName",MediaType.IMAGE_JPEG_VALUE, "image".getBytes(StandardCharsets.UTF_8));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        NotImageContentException exception = assertThrows(NotImageContentException.class, () -> {
            userService.updateProfile(userId,request,profile);
        });

        // then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getErrorCode().getStatus());
        assertEquals("originalName", exception.getDetails().get("filename"));
    }

    @DisplayName("유저의 프로필이미지가 존재하는 상태로 프로필 이미지를 업데이트하면 기존 프로필 이미지를 삭제한다")
    @Test
    void updateUserProfileShouldSucceedChangeProfileImage() {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("changeName");
        User user = new User("test@test.com","password","beforeName");
        user.setProfileImageUrl("oldImage");
        MockMultipartFile profile = new MockMultipartFile("image","originalName.jpg",MediaType.IMAGE_JPEG_VALUE,"newImage".getBytes(StandardCharsets.UTF_8));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willDoNothing().given(s3Storage).delete("oldImage");

        // when
        userService.updateProfile(userId,request,profile);

        // then

        verify(s3Storage).upload(any(MultipartFile.class),anyString());
        assertEquals("changeName",user.getName());
        assertNotEquals("oldImage",user.getProfileImageUrl());
    }

    @DisplayName("유저는 비밀번호를 변경할 수 있다.")
    @Test
    void updateUserPasswordShouldSucceedWhenValidUserIdAndValidRequest() {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("changePassword");
        User user = new User("test@test.com","password","test");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode(request.password())).willReturn("newEncodedPassword");

        // when
        userService.changePassword(userId,request);

        // then
        assertEquals("newEncodedPassword",user.getPassword());
    }

    @DisplayName("유저 목록 조회의 성공한다")
    @Test
    void getAllUsersShouldSucceedWhenValidRequest() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                "test",
                Role.USER,
                false,
                null,
                null,
                20,
                "ASCENDING",
                "name"
        );
        UserDto userDto1 = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test1@example.com", "test1", null, Role.USER, false);
        UserDto userDto2 = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test2@example.com", "test2", null, Role.USER, true);
        UserDto userDto3 = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test3@example.com", "test3", null, Role.ADMIN, false);
        List<UserDto> content = List.of(userDto1,userDto2,userDto3);
        Slice<UserDto> page = new SliceImpl<>(content, PageRequest.of(0, request.limit(), Sort.Direction.ASC, request.sortBy()),false);
        given(userRepository.findAllPage(request)).willReturn(page);
        given(userRepository.countTotalElements(request.emailLike())).willReturn(3L);

        // when
        CursorResponseUserDto response = userService.getAllUsers(request);

        // then
        assertEquals(null, response.nextCursor());
        assertEquals(null, response.nextIdAfter());
        assertFalse(response.hasNext());
        assertEquals(3, response.totalCount());
    }

    @DisplayName("유저 목록 조회 시 해당하는 유저가 한명도 없을 경우 비어있는 data를 반환한다")
    @Test
    void getAllUsersReturnEmptyDataWhenSearchResultIsEmpty() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                "test",
                Role.USER,
                false,
                null,
                null,
                20,
                "ASCENDING",
                "name"
        );
        List<UserDto> content = List.of();
        Slice<UserDto> page = new SliceImpl<>(content, PageRequest.of(0, request.limit(), Sort.Direction.ASC, request.sortBy()),false);
        given(userRepository.findAllPage(request)).willReturn(page);

        // when
        CursorResponseUserDto response = userService.getAllUsers(request);

        // then
        assertEquals(0L, response.totalCount());
        assertEquals(null, response.nextCursor());
        assertEquals(null, response.nextIdAfter());
    }

    @DisplayName("이메일이 주어지면 유저를 찾을 수 있다")
    @Test
    void getUserShouldSucceedWithEmailAddress() {
        // given
        String email = "test@test.com";
        User user = new User("test@test.com","password","test");
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@test.com","test", null, Role.USER, false);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(userMapper.toDto(user)).willReturn(userDto);

        // when
        UserDto findUserDto = userService.findByEmail(email);

        // then
        verify(userRepository).findByEmail(email);
        assertEquals(userDto.id(), findUserDto.id());
        assertEquals(email,findUserDto.email());
    }

    @DisplayName("주어진 이메일의 해당하는 유저가 없으면 404 NOT_FOUND를 응답한다")
    @Test
    void getUserWithEmailAddressShouldFailWhenUserNotFound() {
        // given
        String email = "fail@test.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.findByEmail(email);
        });

        // then
        assertEquals("유저를 찾을 수 없습니다.", exception.getErrorCode().getMessage());
    }
}
