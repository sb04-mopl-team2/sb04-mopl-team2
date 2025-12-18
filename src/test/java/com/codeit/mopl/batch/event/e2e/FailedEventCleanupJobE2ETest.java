package com.codeit.mopl.batch.event.e2e;

import com.codeit.mopl.batch.event.config.job.FailedEventCleanupJobConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@SpringBatchTest
@Import(FailedEventCleanupJobConfig.class)
public class FailedEventCleanupJobE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job failedFollowCleanupJob;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        followee.setFollowerCount(1L);

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
    @DisplayName("FAILED 상태 팔로우 객체 삭제 배치 처리 테스트")
    void failedFollowCleanupJobE2ETest() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.FAILED);
        followRepository.saveAndFlush(follow);

        // when
        JobExecution result = launchJob();

        // then
        assertEquals(BatchStatus.COMPLETED, result.getStatus());

        // then
        List<Follow> afterJobFailedFollows = followRepository.findByStatus(FollowStatus.FAILED);
        assertEquals(0, afterJobFailedFollows.size());
    }

    @Test
    @DisplayName("FAILED 상태 팔로우 객체 삭제 중단 - FAILED 상태인 팔로우 객체가 없음")
    void failedFollowCleanupJobE2ETest_Stop_NoFailedFollows() throws Exception {
        // given
        Follow follow = new Follow(follower, followee);
        follow.setFollowStatus(FollowStatus.CONFIRM);
        followRepository.saveAndFlush(follow);

        // when
        JobExecution result = launchJob();

        // then
        assertEquals(BatchStatus.COMPLETED, result.getStatus());

        Follow afterJobFollow = getFollow(follow.getId());
        assertEquals(FollowStatus.CONFIRM, afterJobFollow.getFollowStatus());
        assertEquals(1, followRepository.findAll().size());
    }

    private Follow getFollow(UUID followId) {
        return followRepository.findById(followId)
                .orElseThrow(() -> new AssertionError("팔로우 찾기 실패 - 해당 id의 팔로우를 찾을 수 없음"));
    }

    private JobExecution launchJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.setJob(failedFollowCleanupJob);
        return jobLauncherTestUtils.launchJob(jobParameters);
    }
}
