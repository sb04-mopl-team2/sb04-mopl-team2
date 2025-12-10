package com.codeit.mopl.batch.event;

import com.codeit.mopl.batch.event.config.step.PendingEventRetryStepConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RetryFollowerIncreaseTaskletTest {

    @Autowired
    private PendingEventRetryStepConfig stepConfig;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private FollowRepository followRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Pending 상태의 follow 객체의 팔로워 수 증가 성공")
    public void retryFollowerIncreaseTasklet_Success() throws Exception {
        // given
        User follower = new User();
        UUID followerId = UUID.randomUUID();
        ReflectionTestUtils.setField(follower, "id", followerId);
        userRepository.saveAndFlush(follower);

        User followee = new User();
        UUID followeeId = UUID.randomUUID();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(0L);
        userRepository.saveAndFlush(followee);

        Follow follow = new Follow(follower, followee);
        UUID followId = UUID.randomUUID();
        ReflectionTestUtils.setField(follow, "id", followId);
        follow.setFollowStatus(FollowStatus.PENDING);
        followRepository.saveAndFlush(follow);

        Tasklet tasklet = stepConfig.retryFollowerIncreaseTasklet();

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
}
