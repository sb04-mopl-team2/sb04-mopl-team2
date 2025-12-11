package com.codeit.mopl.batch.event;

import com.codeit.mopl.batch.event.config.step.PendingEventRetryStepConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RetryFollowerIncreaseTaskletTest {

    @Mock
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private PendingEventRetryStepConfig stepConfig;

    private User follower;
    private User followee;
    private Follow follow;

    private UUID followeeId;
    private UUID followId;

    private Tasklet tasklet;

    @BeforeEach
    void setUp() {
        follower = new User();
        ReflectionTestUtils.setField(follower, "id", UUID.randomUUID());

        followee = new User();
        followeeId = UUID.randomUUID();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(0L);

        follow = new Follow(follower, followee);
        followId = UUID.randomUUID();
        ReflectionTestUtils.setField(follow, "id", followId);
        follow.setFollowStatus(FollowStatus.PENDING);
        follow.setRetryCount(0);

        tasklet = stepConfig.retryFollowerIncreaseTasklet();
    }

    @Test
    @DisplayName("PENDING 상태 follow 객체 팔로워 수 증가 성공")
    void retryFollowerIncreaseTasklet_Success() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.PENDING)))
                .willReturn(List.of(follow));

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(FollowStatus.CONFIRM, follow.getFollowStatus());
        assertEquals(0, follow.getRetryCount());

        verify(followService, times(1))
                .processFollowerIncrease(eq(followId), eq(followeeId));
        verify(followRepository, times(1)).save(follow);
    }

    @Test
    @DisplayName("PENDING 상태 follow 객체 팔로워 수 증가 중단 - PENDING 상태의 팔로우 객체가 없습니다.")
    void retryFollowerIncreaseTasklet_Stop_NoPendingFollows() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.PENDING)))
                .willReturn(List.of());

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(followService, never()).processFollowerIncrease(any(UUID.class), any(UUID.class));
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    @DisplayName("PENDING 상태 follow 객체 팔로워 수 증가 실패 테스트 - retryCount 증가")
    void retryFollowerIncreaseTasklet_Failure() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.PENDING)))
                .willReturn(List.of(follow));

        doThrow(new RuntimeException("test"))
                .when(followService)
                .processFollowerIncrease(eq(followId), eq(followeeId));

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(FollowStatus.PENDING, follow.getFollowStatus());
        assertEquals(1, follow.getRetryCount());

        verify(followService, times(1)).processFollowerIncrease(eq(followId), eq(followeeId));
        verify(followRepository, times(1)).save(follow);
    }

    @Test
    @DisplayName("PENDING 상태 follow 객체 팔로워 수 증가 실패 테스트 - MAX_RETRY_COUNT 달성, FAILURE 전환")
    void retryFollowerIncreaseTasklet_Failure_MaxRetryCount() throws Exception {
        // given
        follow.setRetryCount(2);

        given(followRepository.findByStatus(eq(FollowStatus.PENDING)))
                .willReturn(List.of(follow));

        doThrow(new RuntimeException("test"))
                .when(followService)
                .processFollowerIncrease(eq(followId), eq(followeeId));

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(FollowStatus.FAILED, follow.getFollowStatus());
        assertEquals(3, follow.getRetryCount());

        verify(followService, times(1)).processFollowerIncrease(eq(followId), eq(followeeId));
        verify(followRepository, times(1)).save(follow);
    }
}
