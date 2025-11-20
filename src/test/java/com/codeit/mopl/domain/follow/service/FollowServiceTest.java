package com.codeit.mopl.domain.follow.service;

import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    @Test
    @DisplayName("특정 유저 팔로우 여부 조회 성공 테스트 - true 반환")
    void isFollowedByMe_Success_ReturnTrue() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        given(userRepository.existsById(any(UUID.class))).willReturn(true);
        given(followRepository.existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class))).willReturn(true);

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

        given(userRepository.existsById(any(UUID.class))).willReturn(true);
        given(followRepository.existsByFollowerIdAndFolloweeId(any(UUID.class), any(UUID.class))).willReturn(false);

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
        given(userRepository.existsById(any(UUID.class))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> followService.isFollowedByMe(followerId, followeeId))
                .isInstanceOf(UserNotFoundException.class);
        verify(followRepository, never()).existsByFollowerIdAndFolloweeId(eq(followerId), eq(followeeId));
    }
}
