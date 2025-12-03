package com.codeit.mopl.domain.follow.integration;

import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class FollowApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User follower;
    private User followee;

    private CustomUserDetails followerUserDetails;

    @BeforeEach
    void setUp() {
        follower = new User(
                "follower@test.com",
                "password",
                "follower"
        );
        follower.setRole(Role.USER);

        followee = new User(
                "followee@test.com",
                "password",
                "followee"
        );
        followee.setRole(Role.USER);

        userRepository.saveAndFlush(follower);
        userRepository.saveAndFlush(followee);

        UserDto followerDto = userMapper.toDto(follower);

        followerUserDetails = new CustomUserDetails(
                followerDto,
                "password"
        );
    }

    @Test
    @DisplayName("팔로우 생성 성공 통합 테스트")
    void follow_Success() throws Exception {
        // given
        FollowRequest request = new FollowRequest(followee.getId());
        String content = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(csrf())
                        .with(user(followerUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.followerId").value(follower.getId().toString()))
                .andExpect(jsonPath("$.followeeId").value(followee.getId().toString()));

        List<Follow> allFollows = followRepository.findAll();
        assertThat(allFollows).hasSize(1);
        Follow follow = allFollows.get(0);
        assertThat(follow.getFollower().getId()).isEqualTo(follower.getId());
        assertThat(follow.getFollowee().getId()).isEqualTo(followee.getId());
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 유효하지 않은 요청")
    void follow_Failure_InvalidRequest() throws Exception {
        // given
        FollowRequest request = new FollowRequest(null);
        String content = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(csrf())
                        .with(user(followerUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 자기 자신은 팔로우할 수 없음")
    void follow_Failure_FollowSelfForbidden() throws Exception {
        // given
        FollowRequest request = new FollowRequest(follower.getId());
        String content = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(csrf())
                        .with(user(followerUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 중복 팔로우 금지")
    void follow_Failure_FollowDuplicateForbidden() throws Exception {
        // given
        Follow duplicatedFollow = new Follow(follower, followee);
        followRepository.saveAndFlush(duplicatedFollow);

        FollowRequest request = new FollowRequest(followee.getId());
        String content = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(csrf())
                        .with(user(followerUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );

        // then
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 존재하지 않는 유저")
    void follow_Failure_UserNotFound() throws Exception {
        // given
        FollowRequest request = new FollowRequest(UUID.randomUUID());
        String content = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/follows")
                        .with(csrf())
                        .with(user(followerUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        );

        // then
        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }
}
