package com.codeit.mopl.batch.event;

import com.codeit.mopl.batch.event.config.step.FailedEventCleanupStepConfig;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
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
public class FailedFollowCleanupTaskletTest {

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private FailedEventCleanupStepConfig stepConfig;

    private Follow follow;
    private UUID followId;

    private Tasklet tasklet;

    @BeforeEach
    void setUp() {
        follow = new Follow();
        followId = UUID.randomUUID();
        ReflectionTestUtils.setField(follow, "id", followId);

        tasklet = stepConfig.failedFollowCleanupTasklet();
    }

    @Test
    @DisplayName("FAILED 상태 팔로우 객체 삭제 성공")
    void failedFollowCleanupTasklet_Success() throws Exception {
        // given
        follow.setRetryCount(3);
        follow.setFollowStatus(FollowStatus.FAILED);

        given(followRepository.findByStatus(eq(FollowStatus.FAILED)))
                .willReturn(List.of(follow));

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(followRepository, times(1)).deleteAll(eq(List.of(follow)));
    }

    @Test
    @DisplayName("FAILED 상태 팔로우 객체 삭제 중단 - FAILED 상태 객체 없음")
    void failedFollowCleanupTasklet_Stop_NoFailedFollows() throws Exception {
        // given
        given(followRepository.findByStatus(eq(FollowStatus.FAILED)))
                .willReturn(List.of());

        // when
        RepeatStatus status = tasklet.execute(null, null);

        // then
        assertEquals(RepeatStatus.FINISHED, status);
        verify(followRepository, never()).deleteAll();
    }
}
