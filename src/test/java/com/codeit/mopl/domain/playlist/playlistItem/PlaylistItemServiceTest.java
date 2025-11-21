package com.codeit.mopl.domain.playlist.playlistItem;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.playlistitem.repository.PlaylistItemRepository;
import com.codeit.mopl.domain.playlist.playlistitem.service.PlaylistItemService;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.service.PlaylistService;
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
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PlaylistItemServiceTest {

    @Mock private PlaylistItemRepository playlistItemRepository;
    @Mock private PlaylistService playlistService;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private ContentRepository contentRepository;
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
                    .playlistItems(Collections.emptyList())
                    .build();

            Content content = new Content();
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            given(contentRepository.findById(contentId)).willReturn(Optional.ofNullable(content));
            PlaylistItem playlistItem = new PlaylistItem(playlist, content);

            //when
            playlistItemService.addContent(playlistId, contentId, ownerId);

            //then
            verify(playlistRepository).findById(playlistId);
            verify(contentRepository).findById(contentId);
            verify(playlistItemRepository).save(any(PlaylistItem.class));
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
            verify(playlistItemRepository).delete(any(PlaylistItem.class));
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
