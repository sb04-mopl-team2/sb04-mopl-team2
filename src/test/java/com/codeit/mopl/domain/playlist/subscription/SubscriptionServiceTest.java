package com.codeit.mopl.domain.playlist.subscription;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.domain.playlist.subscription.service.SubscriptionService;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionNotFoundException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionSelfProhibitedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private SubscriptionService subscriptionService;

    @Nested
    @DisplayName("create()")
    class createSubscription {

        @Test
        @DisplayName("정상 요청일 경우 해당 플레이리스트를 구독함")
        void shouldCreateSubscription() {
            //given
            LocalDateTime subscribedAt = LocalDateTime.now();
            UUID subscriberId = UUID.randomUUID();
            User subscriber = new User();
            setId(subscriber, subscriberId);

            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            UUID playlistId = UUID.randomUUID();
            Playlist playlist = Playlist.builder()
                    .user(owner)
                    .build();

            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            given(userRepository.findById(subscriberId)).willReturn(Optional.ofNullable(subscriber));

            Subscription saved = Subscription.builder()
                    .playlist(playlist)
                    .subscriber(subscriber)
                    .subscribedAt(subscribedAt)
                    .build();
            given(subscriptionRepository.save(any(Subscription.class)))
                    .willReturn(saved);
            // when
            subscriptionService.subscribe(playlistId, subscriberId);

            //then
            verify(playlistRepository).findById(playlistId);
            verify(userRepository).findById(subscriberId);
            verify(subscriptionRepository).save(any(Subscription.class));
        }

        @Test
        @DisplayName("구독하려는 플레이리스트가 존재하지 않을 경우 예외 발생")
        void shouldThrowExceptionWhenPlaylistNotFound() {
            //given
            UUID nonExistentPlaylistId = UUID.randomUUID();
            UUID subscriberId = UUID.randomUUID();
            given(playlistRepository.findById(nonExistentPlaylistId)).willReturn(Optional.empty());

            //when & then
            assertThrows(PlaylistNotFoundException.class,
                    () -> subscriptionService.subscribe(nonExistentPlaylistId, subscriberId));
            verify(playlistRepository).findById(nonExistentPlaylistId);
            verify(subscriptionRepository,never()).save(any());
        }

        @Test
        @DisplayName("이미 구독한 플레이리스트일 경우 구독 생성하지 않음")
        void shouldNotSubscribeWhenAlreadySubscribed() {
            // given
            UUID subscriberId = UUID.randomUUID();
            User subscriber = new User();
            setId(subscriber, subscriberId);

            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            UUID playlistId = UUID.randomUUID();
            Playlist playlist = Playlist.builder()
                    .user(owner)
                    .build();

            given(playlistRepository.findById(playlistId)).willReturn(Optional.of(playlist));
            given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
            given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(subscriberId, playlistId))
                    .willReturn(true);
            // when & then
            assertThrows(SubscriptionSelfProhibitedException.class,
                    () -> subscriptionService.subscribe(playlistId, subscriberId));
            verify(playlistRepository).findById(playlistId);
            verify(userRepository).findById(subscriberId);
            verify(subscriptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class deleteSubscription {

        @Test
        @DisplayName("정상 요청일 경우 플레이리스트 구독을 취소함")
        void shouldDeleteSubscription() {
            UUID playlistId = UUID.randomUUID();
            Playlist playlist = new Playlist();
            setId(playlist, playlistId);
            playlist.setUser(new User());

            UUID subscriberId = UUID.randomUUID();
            User subscriber = new User();
            setId(subscriber, subscriberId);

            LocalDateTime subscribedAt = LocalDateTime.now();
            Subscription subscription = Subscription.builder()
                    .playlist(playlist)
                    .subscriber(subscriber)
                    .subscribedAt(subscribedAt)
                    .build();
            given(subscriptionRepository.findBySubscriberIdAndPlaylistId(subscriberId, playlistId))
                    .willReturn(Optional.of(subscription));

            //when
            subscriptionService.unsubscribe(playlistId, subscriberId);

            //then
            verify(subscriptionRepository).findBySubscriberIdAndPlaylistId(subscriberId, playlistId);
            verify(subscriptionRepository).delete(subscription);
            verify(subscriptionRepository, never()).save(any(Subscription.class));
        }

        @Test
        @DisplayName("구독 내역이 존재하지 않을 경우 예외발생")
        void shouldThrowExceptionWhenSubscriptionNotFound() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID subscriberId = UUID.randomUUID();

            //when & then
            assertThrows(SubscriptionNotFoundException.class,
                    () -> subscriptionService.unsubscribe(playlistId, subscriberId));
            verify(subscriptionRepository).findBySubscriberIdAndPlaylistId(subscriberId, playlistId);
            verify(subscriptionRepository, never()).delete(any());
            verify(subscriptionRepository, never()).save(any());
        }
    }

    private static void setId(Object target, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }
}
