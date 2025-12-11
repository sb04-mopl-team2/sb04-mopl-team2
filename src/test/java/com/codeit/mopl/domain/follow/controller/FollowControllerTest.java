package com.codeit.mopl.domain.follow.controller;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.oauth.service.OAuth2UserService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.handler.OAuth2UserSuccessHandler;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockitoBean
    private OAuth2UserService oAuth2UserService;

    @MockitoBean
    private OAuth2UserSuccessHandler oAuth2UserSuccessHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("팔로우 성공 테스트")
    void follow_Success() throws Exception {
        // given
        UUID followId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
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

        verify(followService, times(1)).createFollow(eq(request), eq(followerId));
    }

    @Test
    @DisplayName("팔로우 실패 테스트 - 유효하지 않은 요청")
    void follow_Failure_InvalidRequest() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
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

    @Test
    @DisplayName("특정 유저 팔로우 여부 조회 API 성공 테스트")
    void isFollowedByMe_Success() throws Exception {
        // given
        UUID followeeId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");

        given(followService.isFollowedByMe(eq(followerId), eq(followeeId))).willReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/follows/followed-by-me")
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("followeeId", followeeId.toString())
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        verify(followService, times(1)).isFollowedByMe(eq(followerId), eq(followeeId));
    }

    @Test
    @DisplayName("특정 유저 팔로우 여부 조회 API 실패 테스트 - 유효하지 않은 요청")
    void isFollowedByMe_Failure_InvalidRequest() throws Exception {
        // given
        String followeeId = "testId";
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");

        // when & then
        ResultActions resultActions = mockMvc.perform(
                get("/api/follows/followed-by-me")
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("followeeId", followeeId)
        );
        resultActions.andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("팔로워 수 조회 성공 테스트")
    void getFollowerCount_Success() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");

        given(followService.getFollowerCount(eq(followeeId))).willReturn(1L);

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/follows/count")
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("followeeId", followeeId.toString())
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
        verify(followService, times(1)).getFollowerCount(eq(followeeId));
    }

    @Test
    @DisplayName("팔로워 수 조회 실패 테스트 - 유효하지 않은 요청")
    void getFollowerCount_Failure_InvalidRequest() throws Exception {
        // given
        UUID followerId = UUID.randomUUID();
        String followeeId = "testId";
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");

        // when & then
        ResultActions resultActions = mockMvc.perform(
                get("/api/follows/count")
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("followeeId", followeeId)
        );
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("팔로우 삭제 성공 테스트")
    void deleteFollow_Success() throws Exception {
        // given
        UUID followId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                requesterId,
                Instant.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");

        // when
        ResultActions resultActions = mockMvc.perform(
                delete("/api/follows/" + followId)
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isNoContent());
        verify(followService, times(1)).deleteFollow(eq(followId), eq(requesterId));
    }

    @Test
    @DisplayName("팔로우 삭제 실패 테스트 - 유효하지 않은 요청")
    void deleteFollow_Failure_InvalidRequest() throws Exception {
        // given
        String followId = "testId";
        UUID followerId = UUID.randomUUID();
        UserDto userDto = new UserDto(
                followerId,
                Instant.now(),
                "testUser@test.com",
                "test",
                null,
                Role.USER,
                false
        );
        CustomUserDetails follower = new CustomUserDetails(userDto, "test1234");

        // when & then
        ResultActions resultActions = mockMvc.perform(
                delete("/api/follows/" + followId)
                        .with(user(follower))
                        .contentType(MediaType.APPLICATION_JSON)
        );
        resultActions.andExpect(status().isBadRequest());
    }
}
