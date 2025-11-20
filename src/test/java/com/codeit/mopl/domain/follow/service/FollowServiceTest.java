package com.codeit.mopl.domain.follow.service;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.mapper.FollowMapper;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.exception.follow.FollowDuplicateException;
import com.codeit.mopl.exception.follow.FollowSelfProhibitedException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private FollowMapper followMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FollowService followService;

    @Test
    @DisplayName("팔로우 생성 성공")
    void createFollow_success() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowRequest request = new FollowRequest(followeeId);

        User follower = new User();
        follower.setName("testFollower");

        User followee = new User();

        Follow follow = new Follow(follower, followee);
        FollowDto followDto = new FollowDto(
                UUID.randomUUID(),
                followerId,
                followeeId
        );

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
                .willReturn(false);
        given(followRepository.save(any(Follow.class))).willReturn(follow);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
        given(followMapper.toDto(follow)).willReturn(followDto);

        // when
        FollowDto result = followService.createFollow(request, followerId);
        String expectedTitle = followService.getFollowNotificationTitle(followerId);

        // then
        assertThat(result).isEqualTo(followDto);
        verify(eventPublisher).publishEvent(any(FollowerIncreaseEvent.class));
        verify(notificationService).createNotification(followeeId, expectedTitle, "", Level.INFO);
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 자기 자신은 팔로우할 수 없음")
    void createFollow_FollowSelf_ThrowsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = followerId;
        FollowRequest request = new FollowRequest(followeeId);

        // when & then
        assertThatThrownBy(() -> followService.createFollow(request, followerId))
                .isInstanceOf(FollowSelfProhibitedException.class);
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 중복된 사용자를 팔로우할 수 없음")
    void createFollow_FollowDuplicate_ThrowsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowRequest request = new FollowRequest(followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> followService.createFollow(request, followerId))
                .isInstanceOf(FollowDuplicateException.class);
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 유저 정보가 없으면 팔로우 객체를 생성할 수 없음")
    void createFollow_UserNotFound_ThrowsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowRequest request = new FollowRequest(followeeId);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
                .willReturn(false);
        given(userRepository.findById(followerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.createFollow(request, followerId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
