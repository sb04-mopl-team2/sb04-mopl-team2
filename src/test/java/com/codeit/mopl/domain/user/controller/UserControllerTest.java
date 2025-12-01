package com.codeit.mopl.domain.user.controller;

import com.codeit.mopl.domain.user.dto.request.*;
import com.codeit.mopl.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.fixture.UserFixture;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.util.WithCustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({TestSecurityConfig.class})
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final UUID TEST_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UserDto USER_DTO = new UserDto(TEST_UUID,LocalDateTime.now(),"test@test.com","testName",null,Role.USER,false);

    @DisplayName("이메일, 비밀번호, 이름이 정상적으로 들어오면 회원가입을 시도한다.")
    @Test
    void userCreateShouldSucceedWhenValidRequest() throws Exception {
        // 본문 생성
        UserCreateRequest request = new UserCreateRequest("test","test@example.com","password");
        String content = om.writeValueAsString(request);

        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@example.com","test",null, Role.USER, false);
        given(userService.create(request)).willReturn(userDto);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/users")
                        .with(csrf())
                        .content(content)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(status().isCreated());
    }

    @DisplayName("이메일, 비밀번호, 이름 중 하나의 필드라도 누락되면 회원가입에 실패한다.")
    @Test
    void userCreateShouldFailWhenInvalidRequest() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest("test", null, "password");
        String content = om.writeValueAsString(request);

        // when & then
        ResultActions resultActions = mockMvc.perform(
                post("/api/users",request)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
                        .accept(MediaType.APPLICATION_JSON)
        );
        resultActions.andExpect(status().isBadRequest());
    }

    @DisplayName("UUID 형태의 userId가 들어오면 정상적으로 동작한다.")
    @Test
    void checkUserInformationShouldSucceedWhenValidRequest() throws Exception {
        // 본문 생성
        UUID userId = UUID.randomUUID();

        // given
        UserDto userDto = new UserDto(userId, LocalDateTime.now(), "test@example.com", "test", null, Role.USER, false);
        given(userService.findUser(userId)).willReturn(userDto);

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/users/" + userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isOk());
    }

    @DisplayName("유저 ID가 UUID 이외의 형태로 접근되면 BAD_REQUEST를 반환한다")
    @Test
    void checkUserInformationShouldFailWhenInvalidRequest() throws Exception {
        // 본문 생성
        Long userId = 1L;

        // when & then
        ResultActions resultActions = mockMvc.perform(
                get("/api/users/" + userId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );
        resultActions.andExpect(status().isBadRequest());
    }

    @DisplayName("유저 권한 변경 호출 시 UUID 형태의 userId와 올바른 Role이 주어졌을 때 " +
            "호출한 계정이 어드민의 권한을 가지고 있을 경우 유저의 권한 변경에 성공한다.")
    @WithMockUser(username = "admin@admin.com", roles = {"ADMIN"})
    @Test
    void updateUserRoleShouldSucceedWhenValidRequestAndRequesterIsAdmin() throws Exception {
        // 본문 생성
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
        String content = om.writeValueAsString(request);
        UUID userId = UUID.randomUUID();

        // when
        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + userId + "/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isNoContent());
    }

    @DisplayName("ADMIN 권한이 아닌 유저가 유저 권한 변경 호출 시 403 Forbidden을 반환한다")
    @WithMockUser(username = "test", roles = {"USER"})
    @Test
    void updateUserRoleShouldFailWhenRequesterIsNotAdmin() throws Exception {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ADMIN);
        String content = om.writeValueAsString(request);

        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + UUID.randomUUID() + "/role")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isForbidden());
    }

    @DisplayName("유저 잠금상태 변경 호출 시 UUID 형태의 userId와 올바른 Boolean이 주어졌을 때 " +
            "호출한 계정이 어드민의 권한을 가지고 있을 경우 유저의 권한 변경에 성공한다.")
    @WithMockUser(username = "admin@admin.com", roles = {"ADMIN"})
    @Test
    void updateUserLockedShouldSucceedWhenValidRequestAndRequesterIsAdmin() throws Exception {
        // 본문 생성
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);
        String content = om.writeValueAsString(request);
        UUID userId = UUID.randomUUID();

        // when
        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + userId + "/locked")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isNoContent());
    }

    @DisplayName("ADMIN 권한이 아닌 유저가 유저 잠금상태 변경 호출 시 403 Forbidden을 반환한다")
    @WithMockUser(username = "test", roles = {"USER"})
    @Test
    void updateUserLockedShouldFailWhenRequesterIsNotAdmin() throws Exception {
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);
        String content = om.writeValueAsString(request);

        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + UUID.randomUUID() + "/locked")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isForbidden());
    }

    @DisplayName("UUID 형태의 userId와 검증된 ChangePasswordRequest가 들어왔을 때 비밀번호를 변경할 수 있다")
    @WithCustomMockUser
    @Test
    void updateUserPasswordShouldSucceedWhenValidRequestAndUserId() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("changePassword");
        String content = om.writeValueAsString(request);

        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + TEST_UUID + "/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isNoContent());
    }

    @DisplayName("본인이 아닌 userId의 password 변경 시도 시 실패한다")
    @WithCustomMockUser
    @Test
    void updateUserPasswordShouldFailWhenRequesterIsNotMe() throws Exception {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("changePassword");
        String content = om.writeValueAsString(request);

        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + userId + "/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isForbidden());
    }

    @DisplayName("변경되는 Password는 값이 없거나 공백일 수 없다")
    @WithCustomMockUser
    @Test
    void updateUserPasswordShouldFailWhenInvalidRequest() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(" ");
        String content = om.writeValueAsString(request);
        ResultActions resultActions = mockMvc.perform(
                patch("/api/users/" + TEST_UUID + "/password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isBadRequest());
    }

    @DisplayName("올바른 유저ID로 변경할 이름 혹은 Multipartfile이 주어졌을 때 프로필 변경을 시도한다")
    @WithCustomMockUser
    @Test
    void updateUserProfileShouldSucceedWhenValidRequest() throws Exception {
        // given
        UserUpdateRequest updateRequest = new UserUpdateRequest("changeName");
        MockMultipartFile image = new MockMultipartFile("image", "file".getBytes());
        String content = om.writeValueAsString(updateRequest);
        MockPart request = new MockPart("request", content.getBytes());
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        given(userService.updateProfile(TEST_UUID, updateRequest, image)).willReturn(USER_DTO);

        // when
        ResultActions resultActions = mockMvc.perform(
                multipart("/api/users/" + TEST_UUID)
                        .file(image)
                        .part(request)
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(req -> {req.setMethod("PATCH"); return req;})
                .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_UUID.toString()));
        then(userService).should(times(1)).updateProfile(TEST_UUID, updateRequest, image);
    }

    @DisplayName("본인 이외의 프로필은 수정할 수 없다")
    @WithCustomMockUser
    @Test
    void updateUserProfileShouldFailWhenIsNotMe() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UserUpdateRequest updateRequest = new UserUpdateRequest("changeName");
        MockMultipartFile image = new MockMultipartFile("image", "file".getBytes());
        String content = om.writeValueAsString(updateRequest);
        MockPart request = new MockPart("request", content.getBytes());
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // when
        ResultActions resultActions = mockMvc.perform(
                multipart("/api/users/" + userId)
                        .file(image)
                        .part(request)
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(req -> {req.setMethod("PATCH"); return req;})
                        .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isForbidden());
    }

    @DisplayName("어드민 권한을 가진 유저는 모든 유저 목록을 조회할 수 있다.")
    @WithMockUser(roles = {"ADMIN"})
    @Test
    void getAllUsersShouldSucceedWhenRequesterIsAdmin() throws Exception {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                "test",
                null,
                null,
                null,
                null,
                20,
                "ASCENDING",
                "name"
        );
        UserDto userDto1 = UserFixture.createUserDto1();
        UserDto userDto2 = UserFixture.createUserDto2();
        UserDto userDto3 = UserFixture.createUserDto3();
        List<UserDto> users = List.of(userDto1, userDto2, userDto3);
        CursorResponseUserDto response = new CursorResponseUserDto(
                users,
                userDto3.name(),
                userDto3.id(),
                false,
                3L,
                "name",
                "ASCENDING"
        );
        given(userService.getAllUsers(request)).willReturn(response);

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/users")
                        .param("emailLike", "test")
                        .param("limit", String.valueOf(20))
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size()").value(users.size()))
                .andExpect(jsonPath("$.data[0].id").value(userDto1.id().toString()))
                .andExpect(jsonPath("$.totalCount").value(users.size()));
    }

    @DisplayName("ADMIN 권한이 없는 유저는 유저 목록 조회에 실패한다")
    @WithMockUser(roles = {"USER"})
    @Test
    void getAllUsersShouldFailWhenRequesterIsNotAdmin() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/users")
                        .param("emailLike", "test")
                        .param("limit", String.valueOf(20))
                        .param("sortDirection", "ASCENDING")
                        .param("sortBy", "name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
        );

        // then
        resultActions.andExpect(status().isForbidden());
    }
}
