package com.codeit.mopl.domain.playlist.playlistItem;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.playlistitem.repository.PlaylistItemRepository;
import com.codeit.mopl.domain.playlist.playlistitem.service.PlaylistItemService;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.entity.Subscription;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PlaylistItemServiceTest {

    @Mock private PlaylistItemRepository playlistItemRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks
    private PlaylistItemService playlistItemService;

    @Nested
    @DisplayName("create()")
    class createPlaylistItem {

        @Test
        @DisplayName("정상 요청 시 플레이리스트에 콘텐츠 추가함")
        void shouldCreatePlaylistItem() {
            UUID playlistId = UUID.randomUUID();
            UUID contentId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .user(owner)
                    .playlistItems(new ArrayList<>())
                    .build();

            Content content = new Content();
            content.setTitle("테스트 콘텐츠");

            UUID subscriberId = UUID.randomUUID();
            User subscriber = new User();
            setId(subscriber, subscriberId);

            Subscription subscription = Subscription.builder()
                    .playlist(playlist)
                    .subscriber(subscriber)
                    .build();

            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            given(contentRepository.findById(contentId)).willReturn(Optional.ofNullable(content));
            given(subscriptionRepository.findByPlaylistId(playlistId))
                    .willReturn(List.of(subscription));
            PlaylistItem playlistItem = new PlaylistItem(playlist, content);

            //when
            playlistItemService.addContent(playlistId, contentId, ownerId);

            //then
            verify(playlistRepository).findById(playlistId);
            verify(contentRepository).findById(contentId);
            verify(subscriptionRepository).findByPlaylistId(playlistId);
            verify(notificationService).createNotification(
                    eq(subscriberId),
                    eq("구독한 플레이리스트에 새로운 콘텐츠 추가"),
                    any(String.class),
                    eq(Level.INFO)
            );
        }

        @Test
        @DisplayName("콘텐츠를 추가하려는 플레이리스트가 존재하지 않을 경우 예외 발생")
        void shouldThrowExceptionWhenPlaylistNotFound() {
            //given
            UUID nonExistentPlaylistId = UUID.randomUUID();
            UUID contentId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            Playlist playlist = new Playlist();
            given(playlistRepository.findById(nonExistentPlaylistId)).willReturn(Optional.empty());

            //when & then
            assertThrows(PlaylistNotFoundException.class,
                    () -> playlistItemService.addContent(nonExistentPlaylistId, contentId, ownerId));
            verify(playlistRepository).findById(nonExistentPlaylistId);
            verify(playlistItemRepository,never()).save(any());
            verify(subscriptionRepository, never()).findByPlaylistId(any());
            verify(notificationService, never()).createNotification(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class deletePlaylistItem {

        @Test
        @DisplayName("정상 요청일 경우 플레이리스트에 추가된 콘텐츠를 삭제함")
        void shouldDeleteContentFromPlaylistItem() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID contentId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            User owner = new User();
            setId(owner, ownerId);

            Playlist playlist = Playlist.builder()
                    .user(owner)
                    .build();
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));

            PlaylistItem playlistItem = PlaylistItem.builder()
                    .playlist(playlist)
                    .build();
            given(playlistItemRepository.findByPlaylistIdAndContentId(playlistId,contentId)).willReturn(Optional.ofNullable(playlistItem));

            //when
            playlistItemService.deleteContent(playlistId, contentId, ownerId);

            // then
            verify(playlistRepository).findById(playlistId);
            verify(playlistItemRepository).findByPlaylistIdAndContentId(playlistId,contentId);
        }

        @Test
        @DisplayName("요청자가 플레이리스트 owner가 아닐 때 예외 발생 및 플레이리스트에서 콘텐츠 삭제 실패")
        void shouldThrowExceptionWhenUserUnauthorized() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID contentId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            UUID realOwnerId = UUID.randomUUID();
            User requester = new User();
            setId(requester, realOwnerId);

            Playlist playlist = Playlist.builder()
                    .user(owner)
                    .build();
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));

            //when & then
            assertThrows(PlaylistUpdateForbiddenException.class,
                    () -> playlistItemService.deleteContent(playlistId, contentId, realOwnerId));
            verify(playlistRepository).findById(playlistId);
            verify(playlistItemRepository,never()).delete(any());
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
