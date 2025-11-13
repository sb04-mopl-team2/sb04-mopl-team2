package com.codeit.mopl.domain.user.controller;

import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
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
}
