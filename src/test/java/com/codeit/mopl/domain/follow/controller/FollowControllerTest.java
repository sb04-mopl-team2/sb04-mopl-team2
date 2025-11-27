package com.codeit.mopl.domain.follow.controller;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowController.class)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class})
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private SseEmitterRegistry sseEmitterRegistry;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("팔로우 성공 테스트")
    void follow_Success() throws Exception {
        // given
        UUID followId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                LocalDateTime.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");
        FollowRequest request = new FollowRequest(followeeId);
        String content = objectMapper.writeValueAsString(request);

        FollowDto followDto = new FollowDto(
                followId,
                followerId,
                followeeId
        );

        given(followService.createFollow(eq(request), eq(followerId))).willReturn(followDto);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(followId.toString()))
                .andExpect(jsonPath("$.followerId").value(followerId.toString()))
                .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));
    }

    @Test
    @DisplayName("팔로우 실패 테스트 - 유효하지 않은 요청")
    void follow_Failure_InvalidRequest() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                LocalDateTime.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");
        FollowRequest request = new FollowRequest(null);
        String content = objectMapper.writeValueAsString(request);

        // when & then
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );
        resultActions.andExpect(status().isBadRequest());
    }
}
