package com.codeit.mopl.batch.event.e2e;

import com.codeit.mopl.batch.event.config.job.PendingEventRetryJobConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

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

    @MockitoBean
    private FollowService followService;

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
        followRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트")
    void retryFollowerIncreaseJobE2ETest() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.PENDING);
        followRepository.saveAndFlush(follow);

        // when
        launchJob();

        // then
        Follow confirmedFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.CONFIRM, confirmedFollow.getFollowStatus());
        assertEquals(0, confirmedFollow.getRetryCount());

        User increasedFollowee = getUser(followee.getId());
        assertEquals(1L, increasedFollowee.getFollowerCount());
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트 중단 - PENDING 상태의 팔로우 객체가 없음")
    void retryFollowerIncreaseJobE2ETest_Stop_NoPendingFollows() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.CONFIRM);
        followRepository.saveAndFlush(follow);

        followee.setFollowerCount(1L);
        userRepository.saveAndFlush(followee);

        // when
        launchJob();

        // then
        Follow confirmedFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.CONFIRM, confirmedFollow.getFollowStatus());
        assertEquals(0, confirmedFollow.getRetryCount());

        User increasedFollowee = getUser(followee.getId());
        assertEquals(1L, increasedFollowee.getFollowerCount());
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트 실패 - retryCount 증가, PENDING 유지")
    void retryFollowerIncreaseJobE2ETest_Failure_retryCount() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.PENDING);
        followRepository.saveAndFlush(follow);

        // when
        doThrow(new RuntimeException("increase failed"))
                .when(followService)
                .processFollowerIncrease(eq(follow.getId()), eq(followee.getId()));

        launchJob();

        // then
        Follow afterFailureFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.PENDING, afterFailureFollow.getFollowStatus());
        assertEquals(1, afterFailureFollow.getRetryCount());

        User afterFailureFollowee = getUser(followee.getId());
        assertEquals(0L, afterFailureFollowee.getFollowerCount());
    }

    @Test
    @DisplayName("팔로워 수 증가 배치 처리 테스트 실패 - retryCount MAX 달성, FAILED로 전환")
    void retryFollowerIncreaseJobE2ETest_Failure_MaxRetryCount_FAILED() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.PENDING);
        follow.setRetryCount(Follow.MAX_RETRY_COUNT - 1);
        followRepository.saveAndFlush(follow);

        // when
        doThrow(new RuntimeException("increase failed"))
                .when(followService)
                .processFollowerIncrease(eq(follow.getId()), eq(followee.getId()));

        launchJob();

        // then
        Follow afterFailureFollow = getFollow(follow.getId());
        assertEquals(Follow.MAX_RETRY_COUNT, afterFailureFollow.getRetryCount());
        assertEquals(FollowStatus.FAILED, afterFailureFollow.getFollowStatus());

        User afterFailureFollowee = getUser(followee.getId());
        assertEquals(0L, afterFailureFollowee.getFollowerCount());
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
        return userRepository.findById(followee.getId())
                .orElseThrow(() -> new AssertionError("유저 찾기 실패 - 해당 id의 followee를 찾을 수 없음"));
    }
}
