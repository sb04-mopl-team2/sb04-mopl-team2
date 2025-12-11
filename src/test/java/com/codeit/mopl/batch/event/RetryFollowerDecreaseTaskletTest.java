package com.codeit.mopl.batch.event;

import com.codeit.mopl.batch.event.config.step.CancelledEventRetryStepConfig;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class RetryFollowerDecreaseTaskletTest {

    @Mock
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private CancelledEventRetryStepConfig stepConfig;

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 성공")
    public void retryFollowerDecreaseTasklet_Success() throws Exception {
        // given

        // when

        // then

    }

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 중단 - CANCELLED 상태의 팔로우 객체가 없습니다.")
    public void retryFollowerDecreaseTasklet_Stop_NoCancelledFollows() throws Exception {
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
        follow.setRetryCount(2);


        // when

        // then
    }

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 실패 - retryCount 증가")
    public void retryFollowerDecreaseTasklet_Failure() throws Exception {
        // given

        // when

        // then

    }

    @Test
    @DisplayName("CANCELLED 상태 follow 객체 팔로워 수 감소, 객체 삭제 실패 - MAX_RETRY_COUNT 달성, FAILURE 전환")
    public void retryFollowerDecreaseTasklet_Failure_MaxRetryCount() throws Exception {
        // given

        // when

        // then

    }
}
