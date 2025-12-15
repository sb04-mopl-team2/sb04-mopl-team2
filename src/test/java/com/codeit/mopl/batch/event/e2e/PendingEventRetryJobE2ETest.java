package com.codeit.mopl.batch.event.e2e;

import com.codeit.mopl.batch.event.config.job.PendingEventRetryJobConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@SpringBatchTest
@Import(PendingEventRetryJobConfig.class)
public class PendingEventRetryJobE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job retryFollowerIncreaseJob;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ProcessedEventRepository processedEventRepository;

    private User follower;
    private User followee;

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
        followee.setFollowerCount(0L);

        userRepository.saveAndFlush(follower);
        userRepository.saveAndFlush(followee);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
        jdbcTemplate.update("DELETE FROM BATCH_STEP_EXECUTION");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION_PARAMS");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_EXECUTION");
        jdbcTemplate.update("DELETE FROM BATCH_JOB_INSTANCE");

        followRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트")
    void retryFollowerIncreaseJobE2ETest() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.PENDING);
        follow.setRetryCount(0);
        followRepository.saveAndFlush(follow);

        given(processedEventRepository.existsByEventIdAndEventType(eq(follow.getId()), eq(EventType.FOLLOWER_INCREASE)))
                .willReturn(false);

        // when
        launchJob();

        // then
        Follow afterJobFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.CONFIRM, afterJobFollow.getFollowStatus());
        assertEquals(0, afterJobFollow.getRetryCount());

        User afterJobFollowee = getUser(followee.getId());
        assertEquals(1L, afterJobFollowee.getFollowerCount());

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트 중단 - PENDING 상태의 팔로우 객체가 없음")
    void retryFollowerIncreaseJobE2ETest_Stop_NoPendingFollows() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.CONFIRM);
        follow.setRetryCount(0);
        followRepository.saveAndFlush(follow);

        followee.setFollowerCount(1L);
        userRepository.saveAndFlush(followee);

        // when
        launchJob();

        // then
        Follow afterJobFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.CONFIRM, afterJobFollow.getFollowStatus());
        assertEquals(0, afterJobFollow.getRetryCount());

        User afterJobFollowee = getUser(followee.getId());
        assertEquals(1L, afterJobFollowee.getFollowerCount());

        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트 실패 - retryCount 증가, PENDING 유지")
    void retryFollowerIncreaseJobE2ETest_Failure_retryCount() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.PENDING);
        follow.setRetryCount(0);
        followRepository.saveAndFlush(follow);

        given(processedEventRepository.existsByEventIdAndEventType(eq(follow.getId()), eq(EventType.FOLLOWER_INCREASE)))
                .willReturn(false);

        doThrow(new RuntimeException("save processed event failed"))
                .when(processedEventRepository)
                .save(any());

        // when
        launchJob();

        // then
        Follow afterJobFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.PENDING, afterJobFollow.getFollowStatus());
        assertEquals(1, afterJobFollow.getRetryCount());

        User afterJobFollowee = getUser(followee.getId());
        assertEquals(0L, afterJobFollowee.getFollowerCount());
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트 실패 - retryCount MAX 달성, FAILED로 전환")
    void retryFollowerIncreaseJobE2ETest_Failure_MaxRetryCount_FAILED() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.PENDING);
        follow.setRetryCount(Follow.MAX_RETRY_COUNT - 1);
        followRepository.saveAndFlush(follow);

        given(processedEventRepository.existsByEventIdAndEventType(eq(follow.getId()), eq(EventType.FOLLOWER_INCREASE)))
                .willReturn(false);

        doThrow(new RuntimeException("save processed event failed"))
                .when(processedEventRepository)
                .save(any());

        // when
        launchJob();

        // then
        Follow afterJobFollow = getFollow(follow.getId());
        assertEquals(Follow.MAX_RETRY_COUNT, afterJobFollow.getRetryCount());
        assertEquals(FollowStatus.FAILED, afterJobFollow.getFollowStatus());

        User afterJobFollowee = getUser(followee.getId());
        assertEquals(0L, afterJobFollowee.getFollowerCount());
    }

    private void launchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(retryFollowerIncreaseJob);
        jobLauncherTestUtils.launchJob(jobParameters);
    }

    private Follow getFollow(UUID followId) {
        return followRepository.findById(followId)
                .orElseThrow(() -> new AssertionError("팔로우 찾기 실패 - 해당 id의 팔로우를 찾을 수 없음"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AssertionError("유저 찾기 실패 - 해당 id의 followee를 찾을 수 없음"));
    }
}
