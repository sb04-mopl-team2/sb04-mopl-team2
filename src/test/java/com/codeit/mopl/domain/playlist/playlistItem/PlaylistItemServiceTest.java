package com.codeit.mopl.domain.playlist.playlistItem;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.playlistitem.repository.PlaylistItemRepository;
import com.codeit.mopl.domain.playlist.playlistitem.service.PlaylistItemService;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.service.PlaylistService;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            Playlist playlist = new Playlist();
            Content content = new Content();
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            given(contentRepository.findById(contentId)).willReturn(Optional.ofNullable(content));
            PlaylistItem playlistItem = new PlaylistItem(playlist, content);

            //when
            playlistItemService.addContent(playlistId, contentId);

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
            Playlist playlist = new Playlist();
            given(playlistRepository.findById(nonExistentPlaylistId)).willReturn(Optional.empty());

            //when & then
            assertThrows(PlaylistNotFoundException.class,
                    () -> playlistItemService.addContent(nonExistentPlaylistId, contentId));
            verify(playlistRepository).findById(nonExistentPlaylistId);
            verify(playlistItemRepository,never()).save(any());
        }
    }
}
