package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {

    @Mock private PlaylistRepository playlistRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private PlaylistService playlistService;

    @Nested
    @DisplayName("create()")
    class createPlaylist {

        @Test
        @DisplayName("유저가 존재하고, 요청이 유효할 경우 요청자의 플레이리스트를 생성함")
        void shouldCreatePlaylist() {
            //given
            UUID ownerId = UUID.randomUUID();
            User user = new User();
            user.setEmail("test@example.com");
            user.setName("test");
            user.setPassword("test123");

            PlaylistCreateRequest request =
                    new PlaylistCreateRequest("test title", "test description");
            List<PlaylistItem> playlistItems = new ArrayList<>();
            Playlist saved = Playlist.builder()
                    .playlistItems(playlistItems)
                    .title("test title")
                    .description("test description")
                    .subscriberCount(10)
                    .subscribedByMe(false)
                    .build();

            given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
            given(playlistRepository.save(any(Playlist.class)))
                    .willReturn(saved);

            //when
            PlaylistDto result =  playlistService.createPlaylist(ownerId, request);

            //then
            verify(userRepository).findById(ownerId);
            verify(playlistRepository).save(any(Playlist.class));

            assertThat(result.title()).isEqualTo("test title");
            assertThat(result.description()).isEqualTo("test description");
        }

        @Test
        @DisplayName("api 요청 유저 정보가 유효하지 않을 경우 예외 발생")
        void shouldThrowExceptionWhenUserNotFound() {
            //when
            UUID nonExistentUserId = UUID.randomUUID();
            PlaylistCreateRequest request =
                    new PlaylistCreateRequest("test title", "test description");

            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            //when & then
            assertThrows(IllegalAccessException.class,
                    () -> playlistService.createPlaylist(nonExistentUserId, request));
            verify(userRepository).findById(nonExistentUserId);
            verify(playlistRepository, never()).save(any());
        }


    }

}
