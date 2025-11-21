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
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.exception.follow.FollowDuplicateException;
import com.codeit.mopl.exception.follow.FollowSelfProhibitedException;
import com.codeit.mopl.exception.user.UserIdIsNullException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


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
        assertThat(event.followeeId()).isEqualTo(followeeId);

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
        UUID followeeId = UUID.randomUUID();
        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(0L);

        given(userRepository.findById(eq(followeeId))).willReturn(Optional.of(followee));

        // when
        followService.increaseFollowerCount(followeeId);

        // then
        verify(userRepository, times(1)).findById(eq(followeeId));
        assertThat(followee.getFollowerCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("팔로워 증가 이벤트 처리 실패 - followeeId가 null이 될 수 없음")
    void increaseFollowerCount_UserId_Is_Null_ThrowsException() {
        // given

        // when & then
        assertThatThrownBy(() -> followService.increaseFollowerCount(null))
                .isInstanceOf(UserIdIsNullException.class);
    }

    @Test
    @DisplayName("특정 유저 팔로우 여부 조회 성공 테스트 - true 반환")
    void isFollowedByMe_Success_ReturnTrue() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        given(userRepository.existsById(eq(followeeId))).willReturn(true);
        given(followRepository.existsByFollowerIdAndFolloweeId(eq(followerId), eq(followeeId))).willReturn(true);

        // when
        Boolean isFollowed = followService.isFollowedByMe(followerId, followeeId);

        // then
        assertThat(isFollowed).isTrue();
        verify(userRepository, times(1)).existsById(eq(followeeId));
        verify(followRepository, times(1)).existsByFollowerIdAndFolloweeId(eq(followerId), eq(followeeId));
    }

    @Test
    @DisplayName("특정 유저 팔로우 여부 조회 성공 테스트 - false 반환")
    void isFollowedByMe_Success_ReturnFalse() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        given(userRepository.existsById(eq(followeeId))).willReturn(true);
        given(followRepository.existsByFollowerIdAndFolloweeId(eq(followerId), eq(followeeId))).willReturn(false);

        // when
        Boolean isFollowed = followService.isFollowedByMe(followerId, followeeId);

        // then
        assertThat(isFollowed).isFalse();
        verify(userRepository, times(1)).existsById(eq(followeeId));
        verify(followRepository, times(1)).existsByFollowerIdAndFolloweeId(eq(followerId), eq(followeeId));
    }

    @Test
    @DisplayName("특정 유저 팔로우 여부 조회 실패 - 존재하지 않는 유저")
    void isFollowedByMe_UserNotFound_ThrowsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        given(userRepository.existsById(eq(followeeId))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> followService.isFollowedByMe(followerId, followeeId))
                .isInstanceOf(UserNotFoundException.class);
        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(eq(followerId), eq(followeeId));
    }

    @Test
    @DisplayName("팔로우 삭제 성공 테스트")
    void deleteFollow_Success() {
        // given
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UUID requesterId = followerId;

        User follower = new User();
        ReflectionTestUtils.setField(follower, "id", followerId);

        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(1L);

        Follow follow = new Follow(follower, followee);
        ReflectionTestUtils.setField(follow, "id", followId);

        given(followRepository.findById(eq(followId))).willReturn(Optional.of(follow));

        ArgumentCaptor<FollowerDecreaseEvent> eventCaptor = ArgumentCaptor.forClass(FollowerDecreaseEvent.class);

        // when
        followService.deleteFollow(followId, requesterId);

        // then
        verify(followRepository, times(1)).deleteById(eq(followId));

        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        FollowerDecreaseEvent event = eventCaptor.getValue();
        assertThat(event.followeeId()).isEqualTo(followeeId);
    }

    @Test
    @DisplayName("팔로우 삭제 실패 - 해당 팔로우가 존재하지 않음")
    void deleteFollow_Follow_Not_Found_ThrowsException() {
        // given
        UUID followId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        given(followRepository.findById(eq(followId))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.deleteFollow(followId, requesterId))
                .isInstanceOf(FollowNotFoundException.class);
    }

    @Test
    @DisplayName("팔로우 삭제 실패 - 팔로워 본인이 아니면 삭제 불가능")
    void deleteFollow_Follow_Delete_Forbidden_ThrowsException() {
        // given
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        User follower = new User();
        ReflectionTestUtils.setField(follower, "id", followerId);

        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);

        Follow follow = new Follow(follower, followee);
        ReflectionTestUtils.setField(follow, "id", followId);

        given(followRepository.findById(eq(followId))).willReturn(Optional.of(follow));

        // when & then
        assertThatThrownBy(() -> followService.deleteFollow(followId, requesterId))
                .isInstanceOf(FollowDeleteForbiddenException.class);
    }

    @Test
    @DisplayName("팔로우 삭제 실패 - followerCount가 0이하면 삭제 불가능")
    void deleteFollow_Follower_Count_Cannot_Be_Negative_ThrowsException() {
        // given
        UUID followId = UUID.randomUUID();
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UUID requesterId = followerId;

        User follower = new User();
        ReflectionTestUtils.setField(follower, "id", followerId);

        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(0L);

        Follow follow = new Follow(follower, followee);
        ReflectionTestUtils.setField(follow, "id", followId);

        given(followRepository.findById(eq(followId))).willReturn(Optional.of(follow));

        // when & then
        assertThatThrownBy(() -> followService.deleteFollow(followId, requesterId))
                .isInstanceOf(FollowerCountCannotBeNegativeException.class);
    }

    @Test
    @DisplayName("팔로우 감소 이벤트 처리 성공")
    void decreaseFollowerCount_Success() {
        // given
        UUID followeeId = UUID.randomUUID();
        User followee = new User();
        ReflectionTestUtils.setField(followee, "id", followeeId);
        followee.setFollowerCount(1L);

        given(userRepository.findById(eq(followeeId))).willReturn(Optional.of(followee));

        // when
        followService.decreaseFollowerCount(followeeId);

        // then
        verify(userRepository, times(1)).findById(eq(followeeId));
        assertThat(followee.getFollowerCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("팔로우 감소 이벤트 처리 실패 - followeeId가 null이 될 수 없음")
    void decreaseFollowerCount_UserId_Is_Null_ThrowsException() {
        // given

        // when & then
        assertThatThrownBy(() -> followService.decreaseFollowerCount(null))
                .isInstanceOf(UserIdIsNullException.class);
    }

    @Test
    @DisplayName("팔로우 감소 이벤트 처리 실패 - followeeId에 해당하는 유저가 없음")
    void decreaseFollowerCount_User_Not_Found_ThrowsException() {
        // given
        UUID followeeId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> followService.decreaseFollowerCount(followeeId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
