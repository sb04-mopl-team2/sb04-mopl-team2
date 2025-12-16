package com.codeit.mopl.batch.event;

import com.codeit.mopl.batch.event.config.step.CancelledEventRetryStepConfig;
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
public class RetryFollowerDecreaseTaskletTest {

    @Mock
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private CancelledEventRetryStepConfig stepConfig;

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
        follow.setFollowStatus(FollowStatus.CANCELLED);
        follow.setRetryCount(0);

        tasklet = stepConfig.retryFollowerDecreaseTasklet();
    }

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 성공")
    void retryFollowerDecreaseTasklet_Success() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.CANCELLED)))
                .willReturn(List.of(follow));

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);

        verify(followService, times(1)).processFollowerDecrease(eq(followId), eq(followeeId));
        verify(followRepository, never()).save(any(Follow.class));
    }

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 중단 - CANCELLED 상태의 팔로우 객체가 없습니다.")
    void retryFollowerDecreaseTasklet_Stop_NoCancelledFollows() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.CANCELLED)))
                .willReturn(List.of());

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(FollowStatus.CANCELLED, follow.getFollowStatus());

        verify(followService, never()).processFollowerDecrease(eq(followId), eq(followeeId));
        verify(followRepository, never()).save(eq(follow));
    }

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 실패 - retryCount 증가")
    void retryFollowerDecreaseTasklet_Failure() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.CANCELLED)))
                .willReturn(List.of(follow));

        doThrow(new RuntimeException("test"))
                .when(followService)
                .processFollowerDecrease(eq(followId), eq(followeeId));

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        assertEquals(FollowStatus.CANCELLED, follow.getFollowStatus());
        assertEquals(1, follow.getRetryCount());

        verify(followService, times(1)).processFollowerDecrease(eq(followId), eq(followeeId));
        verify(followRepository, times(1)).save(eq(follow));
    }
}
