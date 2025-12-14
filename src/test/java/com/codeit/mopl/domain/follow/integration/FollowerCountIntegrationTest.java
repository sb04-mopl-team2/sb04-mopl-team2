package com.codeit.mopl.domain.follow.integration;

import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka
public class FollowerCountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        followRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("카프카 이벤트 발행 후 팔로워 수 증가 테스트")
    void followerCount_Increase_AfterKafkaEvent() throws Exception {
        // given
        FollowRequest followRequest = new FollowRequest(followee.getId());
        String content = objectMapper.writeValueAsString(followRequest);

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

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    User updatedFollowee =
                            userRepository.findById(followee.getId()).orElseThrow();
                    assertThat(updatedFollowee.getFollowerCount()).isEqualTo(1L);

                    List<Follow> follows = followRepository.findAll();
                    assertThat(follows).hasSize(1);
                    Follow createdFollow = follows.get(0);
                    assertThat(createdFollow.getFollowStatus()).isEqualTo(FollowStatus.CONFIRM);
                });
    }

    @Test
    @DisplayName("카프카 이벤트 발행 후 팔로워 수 감소 테스트")
    void followerCount_Decrease_AfterKafkaEvent() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.CONFIRM);
        followRepository.saveAndFlush(follow);

        // 팔로워 수 증가
        followee.setFollowerCount(1L);
        userRepository.saveAndFlush(followee);

        // when
        ResultActions resultActions = mockMvc.perform(
                delete("/api/follows/" + follow.getId().toString())
                        .with(csrf())
                        .with(user(followerUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isNoContent());

        Follow cancelledFollow = followRepository.findById(follow.getId()).orElseThrow();
        assertThat(cancelledFollow.getFollowStatus()).isEqualTo(FollowStatus.CANCELLED);

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    User updatedFollowee =
                            userRepository.findById(followee.getId()).orElseThrow();

                    assertThat(updatedFollowee.getFollowerCount()).isEqualTo(0L);
                });
        assertThat(followRepository.findAll()).hasSize(0);
    }
}
