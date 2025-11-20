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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
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
        ReflectionTestUtils.setField(follower, "id", followerId);

        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);

        Follow follow = new Follow(follower, followee);

        given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
                .willReturn(false);
        given(followRepository.save(any(Follow.class))).willReturn(follow);
        given(userRepository.findById(followerId)).willReturn(Optional.of(follower));
        given(userRepository.findById(followeeId)).willReturn(Optional.of(followee));
        given(followMapper.toDto(any(Follow.class))).willAnswer(invocation -> {
            Follow savedFollow = invocation.getArgument(0);
            return new FollowDto(UUID.randomUUID(), followerId, followeeId);
        });

        ArgumentCaptor<Follow> followCaptor = ArgumentCaptor.forClass(Follow.class);
        ArgumentCaptor<FollowerIncreaseEvent> eventCaptor = ArgumentCaptor.forClass(FollowerIncreaseEvent.class);

        // when
        FollowDto result = followService.createFollow(request, followerId);

        // then
        verify(followRepository).save(followCaptor.capture());
        Follow savedFollow = followCaptor.getValue();
        assertThat(savedFollow.getFollower().getId()).isEqualTo(follower.getId());
        assertThat(savedFollow.getFollowee().getId()).isEqualTo(followee.getId());

        assertThat(result.followerId()).isEqualTo(followerId);
        assertThat(result.followeeId()).isEqualTo(followeeId);
        assertThat(result.id()).isNotNull();

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        FollowerIncreaseEvent event = eventCaptor.getValue();
        assertThat(event.followDto()).isEqualTo(result);

        verify(notificationService).createNotification(eq(followeeId), any(String.class), eq(""), eq(Level.INFO));
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
        given(userRepository.findById(followerId)).willReturn(Optional.of(new User()));
        given(userRepository.findById(followeeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.createFollow(request, followerId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 성공 테스트")
    void increaseFollowerCount_Success() {
        // given
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowDto followDto = new FollowDto(followId, followerId, followeeId);

        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(0L);

        given(userRepository.findById(eq(followeeId))).willReturn(Optional.of(followee));

        // when
        followService.increaseFollowerCount(followDto);

        // then
        verify(userRepository, times(1)).findById(eq(followeeId));
        assertThat(followee.getFollowerCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 실패 - followDto가 null이 될 수 없음")
    void increaseFollowerCount_FollowDtoNull_ThrowsException() {
        // given

        // when & then
        assertThatThrownBy(() -> followService.increaseFollowerCount(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 실패 - followeeId가 null이 될 수 없음")
    void increaseFollowerCount_FolloweeIdNull_ThrowsException() {
        // given
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        FollowDto followDto = new FollowDto(followId, followerId, null);

        // when & then
        assertThatThrownBy(() -> followService.increaseFollowerCount(followDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("특정 유저의 팔로워 수 조회 성공 테스트")
    void getFollowerCount_Success() {
        // given
        UUID followeeId = UUID.randomUUID();
        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(1L);

        given(userRepository.findById(eq(followeeId))).willReturn(Optional.of(followee));

        // when
        long result = followService.getFollowerCount(followeeId);

        // then
        assertThat(result).isEqualTo(1L);
        verify(userRepository, times(1)).findById(eq(followeeId));
    }

    @Test
    @DisplayName("특정 유저의 팔로워 수 조회 실패 - followee가 존재하지 않는 유저")
    void getFollowerCount_FolloweeNotExists_ThrowsException() {
        // given
        UUID followeeId = UUID.randomUUID();
        given(userRepository.findById(eq(followeeId))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.getFollowerCount(followeeId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
