package com.codeit.mopl.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.dto.PlaylistUpdateRequest;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.test.context.support.WithMockUser;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {

    @Mock private PlaylistRepository playlistRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlaylistMapper playlistMapper;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
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

            UserSummary summary = new UserSummary(ownerId, "test", "test");
            PlaylistDto dto =
                    new PlaylistDto(UUID.randomUUID(), summary, "test title","test description", null,10,false,null);

            given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
            given(playlistRepository.save(any(Playlist.class)))
                    .willReturn(saved);
            given(playlistMapper.toPlaylistDto(saved)).willReturn(dto);

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
            assertThrows(UserNotFoundException.class,
                    () -> playlistService.createPlaylist(nonExistentUserId, request));
            verify(userRepository).findById(nonExistentUserId);
            verify(playlistRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("find()")
    class findPlaylist {

        @Test
        @DisplayName("요청 파라미터 없이 모든 등록된 플레이리스트 목록을 조회함")
        void shouldReturnAllPlaylists() {
            //given
            PlaylistSearchCond cond = new PlaylistSearchCond();
            cond.setKeywordLike(null);
            cond.setOwnerIdEqual(null);
            cond.setSubscriberIdEqual(null);
            cond.setCursor(null);
            cond.setLimit(10);
            cond.setSortDirection(SortDirection.DESCENDING);
            cond.setSortBy(SortBy.UPDATED_AT);

            UUID loginUserId = UUID.randomUUID();
            UUID playlistId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UserSummary userSummary = new UserSummary(ownerId, "test", "test");
            Playlist playlist = Playlist.builder().title("테스트").subscribedByMe(true).build();
            setId(playlist, playlistId);

            given(playlistRepository.findAllByCond(any(PlaylistSearchCond.class)))
                    .willReturn(Arrays.asList(playlist));
            given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlistId))
                    .willReturn(true);
            given(playlistMapper.toPlaylistDto(playlist)).willReturn(new PlaylistDto(playlistId, userSummary, "테스트","테스트 설명", null, 0, false,null));
            given(playlistRepository.countAllByCond(any(PlaylistSearchCond.class)))
                    .willReturn(1L);
            // when
            CursorResponsePlaylistDto result = playlistService.getAllPlaylists(loginUserId,cond);

            //then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.data().get(0).title()).isEqualTo("테스트");
            assertThat(result.data().get(0).description()).isEqualTo("테스트 설명");
            assertThat(result.data().get(0).owner().name()).isEqualTo("test");
        }

        @Test
        @DisplayName("해당 키워드가 제목 또는 설명에 포함된 플레이리스트만 조회함")
        void shouldReturnPlaylistsByKeyword(){
            //given
            PlaylistSearchCond cond = new PlaylistSearchCond();
            cond.setKeywordLike("키워드");
            cond.setOwnerIdEqual(null);
            cond.setSubscriberIdEqual(null);
            cond.setCursor(null);
            cond.setLimit(10);
            cond.setSortDirection(SortDirection.DESCENDING);
            cond.setSortBy(SortBy.UPDATED_AT);
            UUID playlist1_Id = UUID.randomUUID();
            UUID playlist2_Id = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            User owner= new User();
            UserSummary userSummary = new UserSummary(ownerId, "test", "test");
            Playlist playlist1 = Playlist.builder().user(owner).title("키워드 포함 제목1").description("테스트 설명1").subscribedByMe(true).build();
            setId(playlist1, playlist1_Id);
            Playlist playlist2 = Playlist.builder().user(owner).title("키워드 포함 제목2").description("테스트 설명2").subscribedByMe(false).build();
            setId(playlist2, playlist2_Id);
            List<Playlist> playlists = Arrays.asList(playlist1, playlist2);
            given(playlistRepository.findAllByCond(any(PlaylistSearchCond.class)))
                    .willReturn(playlists);

            given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlist1_Id))
                    .willReturn(true);
            given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlist2_Id))
                    .willReturn(false);
            given(playlistMapper.toPlaylistDto(playlist1))
                    .willReturn(new PlaylistDto(playlist1_Id, userSummary, "키워드 포함 제목1", "테스트 설명1", null, 0, false,null));
            given(playlistMapper.toPlaylistDto(playlist2))
                    .willReturn(new PlaylistDto(playlist2_Id, userSummary, "키워드 포함 제목2", "테스트 설명2", null, 0, false,null));
            given(playlistRepository.countAllByCond(any(PlaylistSearchCond.class)))
                    .willReturn(2L);
            //when
            CursorResponsePlaylistDto result = playlistService.getAllPlaylists(loginUserId,cond);

            //then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.data().get(0).title()).isEqualTo("키워드 포함 제목1");
            assertThat(result.data().get(0).description()).isEqualTo("테스트 설명1");
            assertThat(result.data().get(0).owner().name()).isEqualTo("test");
        }

        @Test
        @DisplayName("키워드와 일치하는 결과가 없을 경우 빈 리스트를 반환함")
        void shouldReturnEmptyWhenKeywordNotFound() {
            //given
            PlaylistSearchCond cond = new PlaylistSearchCond();
            cond.setKeywordLike("없는 키워드");
            cond.setOwnerIdEqual(null);
            cond.setSubscriberIdEqual(null);
            cond.setCursor(null);
            cond.setLimit(10);
            cond.setSortDirection(SortDirection.DESCENDING);
            cond.setSortBy(SortBy.UPDATED_AT);
            UUID loginUserId = UUID.randomUUID();

            given(playlistRepository.findAllByCond(cond)).willReturn(Collections.emptyList());

            //when
            CursorResponsePlaylistDto result = playlistService.getAllPlaylists(loginUserId,cond);

            //then
            assertThat(result.totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("요청이 유효할 경우 플레이리스트 상세보기 선택 시(플레이리스트 클릭) 플레이리스트 단건 조회함")
        void shouldGetPlaylist() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UserSummary userSummary = new UserSummary(ownerId, "test", "test");

            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .subscribedByMe(false)
                    .build();

            PlaylistDto dto = new PlaylistDto(
                    playlistId,
                    userSummary,
                    "테스트 제목",
                    "테스트 설명",
                    null,
                    2,
                    true,
                    null);
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlistId)).willReturn(false);
            given(playlistMapper.toPlaylistDto(playlist)).willReturn(dto);

            //when
            PlaylistDto result = playlistService.getPlaylist(loginUserId,playlistId);

            //then
            assertThat(result.title()).isEqualTo("테스트 제목");
            assertThat(result.description()).isEqualTo("테스트 설명");
        }

        @Test
        @DisplayName("존재하지 않은 플레이리스트 ID로 단건 조회 요청 시 예외 발생")
        void shouldThrowExceptionWhenPlaylistNotFound() {
            //given
            UUID loginUserId = UUID.randomUUID();
            UUID nonExistentPlaylistId = UUID.randomUUID();

            given(playlistRepository.findById(nonExistentPlaylistId)).willReturn(Optional.empty());
            //when & then
            assertThrows(PlaylistNotFoundException.class, () -> {
                playlistService.getPlaylist(loginUserId,nonExistentPlaylistId);
            });
        }

        @Test
        @DisplayName("플레이리스트 내 콘텐츠가 존재하지 않을 경우 빈 리스트 반환")
        void shouldReturnEmptyWhenContentNotFound() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UserSummary userSummary = new UserSummary(ownerId, "test", "test");
            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .playlistItems(Collections.emptyList())
                    .subscribedByMe(true)
                    .build();
            PlaylistDto dto = new PlaylistDto(
                    playlistId,
                    userSummary,
                    "테스트 제목",
                    "테스트 설명",
                    null,
                    0,
                    true,
                    Collections.emptyList());
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            given(subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlistId)).willReturn(true);
            given(playlistMapper.toPlaylistDto(playlist)).willReturn(dto);

            //when
            PlaylistDto result = playlistService.getPlaylist(loginUserId,playlistId);

            //then
            assertThat(result.contents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("update()")
    class updatePlaylist {

        @Test
        @DisplayName("요청이 유효할 경우 플레이리스트 정보를 수정함")
        void shouldUpdatePlaylist() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .user(owner)
                    .playlistItems(Collections.emptyList())
                    .build();

            PlaylistUpdateRequest request = new PlaylistUpdateRequest(
                    "테스트 제목 수정",
                    "테스트 설명 수정"
            );
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            UserSummary summary = new UserSummary(UUID.randomUUID(), "test", "test");
            PlaylistDto updatedDto = new PlaylistDto(
                    playlistId,
                    summary,
                    "테스트 제목 수정",
                    "테스트 설명 수정",
                    null,
                    0,
                    true,
                    Collections.emptyList()
            );
            given(playlistMapper.toPlaylistDto(playlist)).willReturn(updatedDto);

            //when
            PlaylistDto result = playlistService.updatePlaylist(ownerId,playlistId,request);
            //then
            assertThat(result.title()).isEqualTo("테스트 제목 수정");
            assertThat(result.description()).isEqualTo("테스트 설명 수정");
        }

        @WithMockUser
        @Test
        @DisplayName("요청자가 플레이리스트의 owner가 아닐 경우 권한 예외 발생")
        void shouldThrowExceptionWhenUserNotAuthorized() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID requestUserId = UUID.randomUUID();

            User owner = new User();
            setId(owner, ownerId);

            User requester = new User();
            setId(requester, requestUserId);

            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .user(owner)
                    .playlistItems(Collections.emptyList())
                    .build();
            PlaylistUpdateRequest request = new PlaylistUpdateRequest(
                    "테스트 제목 수정",
                    "테스트 설명 수정"
            );
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));
            //when & then
            assertThrows(PlaylistUpdateForbiddenException.class, () -> playlistService.updatePlaylist(requestUserId,playlistId,request) );
            verify(playlistRepository).findById(playlistId);
            verify(playlistMapper, never()).toPlaylistDto(any());

        }
    }

    @Nested
    @DisplayName("delete()")
    class deletePlaylist {

        @Test
        @DisplayName("요청이 유효할 경우 플레이리스트를 삭제함")
        void shouldDeletePlaylist() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .user(owner)
                    .playlistItems(Collections.emptyList())
                    .build();
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));

            //when
            playlistService.deletePlaylist(playlistId,ownerId);
            //then
            verify(playlistRepository).findById(playlistId);
            verify(subscriptionRepository).deleteByPlaylistId(playlistId);
            verify(playlistRepository).deleteById(playlistId);
        }

        @Test
        @DisplayName("요청자가 플레이리스트 owner가 아닐 때 예외 발생 및 삭제 실패")
        void shouldThrowExceptionWhenUserUnauthorized() {
            //given
            UUID playlistId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            User owner = new User();
            setId(owner, ownerId);

            UUID requestUserId = UUID.randomUUID();
            User requester = new User();
            setId(requester, requestUserId);

            Playlist playlist = Playlist.builder()
                    .title("테스트 제목")
                    .description("테스트 설명")
                    .user(owner)
                    .playlistItems(Collections.emptyList())
                    .build();
            given(playlistRepository.findById(playlistId)).willReturn(Optional.ofNullable(playlist));

            //when & then
            assertThrows(PlaylistUpdateForbiddenException.class,
                    () -> playlistService.deletePlaylist(playlistId,requestUserId));
            verify(playlistRepository).findById(playlistId);
            verify(playlistMapper, never()).toPlaylistDto(any());
            verify(playlistRepository, never()).deleteById(playlistId);
        }
    }

    //UpdatableEntity 상속 받 엔티티의 setId()를 가능하게 하는 헬퍼메서드
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
