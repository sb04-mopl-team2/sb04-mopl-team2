package com.codeit.mopl.batch.event;

import com.codeit.mopl.batch.event.config.step.PendingEventRetryStepConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.domain.user.entity.User;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RetryFollowerIncreaseTaskletTest {

    @Mock
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private PendingEventRetryStepConfig stepConfig;

    @Test
    @DisplayName("Pending 상태 follow 객체 팔로워 수 증가 성공")
    public void retryFollowerIncreaseTasklet_Success() throws Exception {
        // given
        User follower = new User();
        UUID followerId = UUID.randomUUID();
        ReflectionTestUtils.setField(follower, "id", followerId);

        User followee = new User();
        UUID followeeId = UUID.randomUUID();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(0L);

        Follow follow = new Follow(follower, followee);
        UUID followId = UUID.randomUUID();
        ReflectionTestUtils.setField(follow, "id", followId);
        follow.setFollowStatus(FollowStatus.PENDING);
        follow.setRetryCount(0);

        Tasklet tasklet = stepConfig.retryFollowerIncreaseTasklet();


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
    @DisplayName("Pending 상태 follow 객체 팔로워 수 증가 중단 - PENDING 상태의 팔로우 객체가 없습니다.")
    void retryFollowerIncreaseTasklet_NoPendingFollows() throws Exception {
        // given
        Tasklet tasklet = stepConfig.retryFollowerIncreaseTasklet();

        given(followRepository.findByStatus(eq(FollowStatus.PENDING)))
                .willReturn(List.of());

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
    }
}
