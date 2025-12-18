package com.codeit.mopl.domain.playlist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistUpdateRequest;
import com.codeit.mopl.domain.playlist.playlistitem.service.PlaylistItemService;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.service.PlaylistService;
import com.codeit.mopl.domain.playlist.subscription.service.SubscriptionService;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.exception.content.ContentErrorCode;
import com.codeit.mopl.exception.content.ContentNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistItemNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionDuplicateException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionNotFoundException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionSelfProhibitedException;
import com.codeit.mopl.oauth.service.OAuth2UserService;
import com.codeit.mopl.security.CustomUserDetailsService;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.security.jwt.handler.OAuth2UserSuccessHandler;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.util.WithCustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = PlaylistController.class)
@Import({TestSecurityConfig.class})
@ActiveProfiles("test")
public class PlaylistControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PlaylistService playlistService;

    @MockitoBean private PlaylistRepository playlistRepository;

    @MockitoBean
    private PlaylistItemService playlistItemService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private OAuth2UserService oAuth2UserService;

    @MockitoBean
    private OAuth2UserSuccessHandler oAuth2UserSuccessHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;


    /**
     * 플레이리스트 생성
     *
     */
    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 생성 - 플레이리스트 생성 성공함")
    void createPlaylist() throws Exception {
        //given
        PlaylistCreateRequest request = new PlaylistCreateRequest(
                "테스트 제목", "테스트 설명"
        );
        UUID playlistId = UUID.randomUUID();
        PlaylistDto response = new PlaylistDto(
                playlistId, null, "테스트 제목", "테스트 설명", Instant.now(), 0L, false, List.of());

        when(playlistService.createPlaylist(any(), any(PlaylistCreateRequest.class))).thenReturn(response);

        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
        //then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(playlistId.toString()));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 생성 - 제목을 작성하지 않으면 예외 발생함")
    void createPlaylistFailWhenTitleIsNull() throws Exception {
        //given
        PlaylistCreateRequest request = new PlaylistCreateRequest(null, "테스트 설명");
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
        //then
        result.andExpect(status().isBadRequest());
    }

    /**
     * 플레이리스트 목록 조회(커서 기반 페이지네이션)
     */
    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 목록 조회 - 플레이리스트 목록 조회를 성공함")
    void getPlaylistsSuccess() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();

        CursorResponsePlaylistDto response = new CursorResponsePlaylistDto(
                List.of(), "cursor123", playlistId,false, 1L ,SortBy.UPDATED_AT, SortDirection.DESCENDING
        );
        when(playlistService.getAllPlaylists(any(), any())).thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/playlists")
                        .param("cursor", "cursor123")
                        .param("idAfter", playlistId.toString())
                        .param("limit", "10")
                        .param("sortBy", "UPDATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("cursor123"));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 목록 조회 - keyword 검색을 적용함")
    void getPlaylistsWithKeywordSuccess() throws Exception {
        //given
        CursorResponsePlaylistDto response = new CursorResponsePlaylistDto(
                List.of(), null, null, false, 1L ,SortBy.UPDATED_AT, SortDirection.DESCENDING
        );
        when(playlistService.getAllPlaylists(any(), any())).thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/playlists")
                        .param("keywordLike", "test")
                        .param("limit", "10")
                        .param("sortBy", "UPDATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 목록 조회 - ownerId로 플레이리스트 필터링 조회함")
    void getPlaylistsWithOwnerIdSuccess() throws Exception {
        //given
        UUID ownerId = UUID.randomUUID();
        CursorResponsePlaylistDto response = new CursorResponsePlaylistDto(
                List.of(), null, null, false, 1L ,SortBy.UPDATED_AT, SortDirection.DESCENDING
        );
        when(playlistService.getAllPlaylists(any(), any())).thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/playlists")
                        .param("ownerIdEqual", ownerId.toString())
                        .param("limit", "10")
                        .param("sortBy", "UPDATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 목록 조회 - cursor 기반 페이징이 정상 동작함")
    void getPlaylistsWithCursorSuccess() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        CursorResponsePlaylistDto response = new CursorResponsePlaylistDto(
                List.of(), "test cursor", playlistId, true, 10L ,SortBy.UPDATED_AT, SortDirection.DESCENDING
        );
        when(playlistService.getAllPlaylists(any(), any())).thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/playlists")
                        .param("nextCursor", "test cursor")
                        .param("hasNext", "true")
                        .param("limit", "10")
                        .param("sortBy", "UPDATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("test cursor"))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 목록 조회 - 잘못된 파리미터형태 (limit값)으로 인하여 실패함")
    void getPlaylistFailWithInvalidLimit() throws Exception {
        ResultActions result = mockMvc.perform(
                get("/api/playlists")
                        .param("limit", "invalid") //limit가 숫자가 아닌 경우
        );
        result.andExpect(status().isBadRequest());
    }

    /**
     * 플레이리스트 단건 조회
     */
    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 단건 조회 - 플레이리스트 단건 조회를 성공함")
    void getPlaylistSuccess() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        PlaylistDto response = new PlaylistDto(
                playlistId, null, "테스트 제목", "테스트 설명", Instant.now(), 2L, true, List.of()
        );
        when(playlistService.getPlaylist(any(UUID.class), any(UUID.class)))
                .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/playlists/{playlistId}", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId.toString()))
                .andExpect(jsonPath("$.title").value("테스트 제목"))
                .andExpect(jsonPath("$.description").value("테스트 설명"))
                .andExpect(jsonPath("$.subscriberCount").value(2L))
                .andExpect(jsonPath("$.subscribedByMe").value(true));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 단건 조회 - 존재하지 않는 플레이리스트 이면 404 예외 발생함")
    void getPlaylistFailWithNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        when(playlistService.getPlaylist(any(UUID.class), any(UUID.class)))
                .thenThrow(PlaylistNotFoundException.withId(playlistId));
        //when
        ResultActions result = mockMvc.perform(
                get("/api/playlists/{playlistId}", playlistId)
                .contentType(MediaType.APPLICATION_JSON)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    /**
     * 플레이리스트 정보 수정
     */

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 정보 수정 - 플레이리스트 정보 수정 성공함")
    void updatePlaylistSuccess() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        PlaylistUpdateRequest request = new PlaylistUpdateRequest(
                "테스트 수정 제목", "테스트 수정 설명"
        );
        PlaylistDto response = new PlaylistDto(
                playlistId, null, "테스트 수정 제목", "테스트 수정 설명", Instant.now(), 2L, true, List.of()
        );
        when(playlistService.updatePlaylist(any(UUID.class), any(UUID.class), any(PlaylistUpdateRequest.class)))
                .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                patch("/api/playlists/{playlistId}", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId.toString()));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 정보 수정 - 존재하지 않는 플레이리스트라면 404 예외 발생함")
    void updatePlaylistFailWithNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        PlaylistUpdateRequest response = new PlaylistUpdateRequest(
                "테스트 수정 제목", "테스트 수정 설명"
        );
        doThrow(PlaylistNotFoundException.withId(playlistId))
                .when(playlistService)
                .updatePlaylist(any(UUID.class), any(UUID.class), any(PlaylistUpdateRequest.class));
        //when
        ResultActions result = mockMvc.perform(
                patch("/api/playlists/{playlistId}", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(response))
        );
        //then
        result.andExpect(status().isNotFound());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 정보 수정 - 요청자가 플레이리스트의 owner가 아닐 경우 403 예외 발생함")
    void updatePlaylistFailWithForbidden() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        PlaylistUpdateRequest request = new PlaylistUpdateRequest(
                "테스트 수정 제목", "테스트 수정 설명"
        );
        doThrow(new PlaylistUpdateForbiddenException(playlistId))
                .when(playlistService)
                .updatePlaylist(any(UUID.class), eq(playlistId), any());

        // when
        ResultActions result = mockMvc.perform(
                patch("/api/playlists/{playlistId}", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        result.andExpect(status().isForbidden());
    }

    /**
     * 플레이리스트 삭제
     */

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 삭제 - 플레이리스트를 삭제 성공함")
    void deletePlaylistSuccess() throws Exception {
        UUID playlistId = UUID.randomUUID();
        doNothing()
                .when(playlistService)
                .deletePlaylist(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}", playlistId)
        );
        //then
        result.andExpect(status().isOk());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 삭제 - 존재하지 않는 플레이리스트라면 404예외 발생함")
    void deletePlaylistFailWithNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        doThrow(PlaylistNotFoundException.withId(playlistId))
                .when(playlistService)
                .deletePlaylist(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}", playlistId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 삭제 - 요청자가 플레이리스트의 owner가 아닐 경우 403 예외 발생함")
    void deletePlaylistFailWithForbidden() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        doThrow(new PlaylistUpdateForbiddenException(playlistId))
                .when(playlistService)
                .deletePlaylist(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}", playlistId)
        );
        //then
        result.andExpect(status().isForbidden());
    }

    /**
     * 플레이리스트에 콘텐츠 추가
     */
    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 - 플레이리스트 추가 성공함")
    void addContentToPlaylistSuccess() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doNothing()
                .when(playlistItemService)
                .addContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isOk());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 - 존재하지 않는 플레이리스트라면 404 예외 발생함")
    void addContentToPlaylistFailWithPlaylistNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doThrow(PlaylistNotFoundException.withId(playlistId))
                .when(playlistItemService)
                .addContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 - 존재하지 않는 콘텐츠라면 404예외 발생함")
    void addContentToPlaylistFailWitContentNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doThrow(new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)))
                .when(playlistItemService)
                .addContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    /**
     * 플레이리스트에서 콘텐츠 삭제
     */

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 - 삭제 성공함")
    void deleteContentFromPlaylistSuccess() throws Exception {
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doNothing()
                .when(playlistItemService)
                .deleteContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isOk());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 - 존재하지 않는 플레이리스트라면 404 예외 발생함")
    void deleteContentFromPlaylistFailWithPlaylistNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doThrow(PlaylistNotFoundException.withId(playlistId))
                .when(playlistItemService)
                .deleteContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 - 요청자가 플레이리스트의 owner가 아닐 경우 403 예외 발생")
    void deleteContentFromPlaylistFailWithForbidden() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doThrow(new PlaylistUpdateForbiddenException(playlistId))
                .when(playlistItemService)
                .deleteContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isForbidden());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 - 해당 콘텐츠가 플레이리스트 내 존재하지 않으면 404예외 발생함")
    void deleteContentFromPlaylistFailWithPlaylistItemNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        doThrow(PlaylistItemNotFoundException.withId(contentId))
                .when(playlistItemService)
                .deleteContent(any(UUID.class),any(UUID.class),any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}/contents/{contentId}", playlistId, contentId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    /**
     * 플레이리스트 구독
     */

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 구독 - 플레이리스트 구독처리에 성공함")
    void subscribeToPlaylistSuccess() throws Exception {
        UUID playlistId = UUID.randomUUID();
        doNothing()
                .when(subscriptionService)
                .subscribe(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/subscription", playlistId)
        );
        //then
        result.andExpect(status().isNoContent());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 구독 - 존재하지 않는 플레이리스트라면 404 예외 발생함")
    void subscribeToPlaylistFailWithPlaylistNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        doThrow(PlaylistNotFoundException.withId(playlistId))
                .when(subscriptionService)
                .subscribe(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/subscription", playlistId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 구독 - 본인의 플레이리스트를 구독하려는 경우 403예외 발생함")
    void subscribeToPlaylistFailWhenSubscribeOwnPlaylist() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        doThrow(SubscriptionSelfProhibitedException.withId(playlistId, subscriberId))
                .when(subscriptionService)
                .subscribe(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/subscription", playlistId)
        );
        //then
        result.andExpect(status().isForbidden());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 구독 - 이미 구독 내역이 존재할 경우 409예외 발생함")
    void subscribeToPlaylistFailWhenSubscriptionAlreadyExists() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        doThrow(SubscriptionDuplicateException.withId(playlistId, subscriberId))
                .when(subscriptionService)
                .subscribe(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/playlists/{playlistId}/subscription", playlistId)
        );
        //then
        result.andExpect(status().isConflict());
    }

    /**
     * 플레이리스트 구독 취소
     */

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 구독 취소 - 구독 취소 처리에 성공함")
    void unsubscribeFromPlaylistSuccess() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        doNothing()
                .when(subscriptionService)
                .unsubscribe(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}/subscription", playlistId)
        );
        //then
        result.andExpect(status().isNoContent());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("플레이리스트 구독 취소 - 플레이리스트 구독 내역이 존재하지 않을 경우 404예외 발생함")
    void unsubscribeFromPlaylistFailWithSubscriptionNotFound() throws Exception {
        //given
        UUID playlistId = UUID.randomUUID();
        UUID subscriberId = UUID.randomUUID();
        doThrow(SubscriptionNotFoundException.withId(playlistId, subscriberId))
                .when(subscriptionService)
                .unsubscribe(any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                delete("/api/playlists/{playlistId}/subscription", playlistId)
        );
        //then
        result.andExpect(status().isNotFound());
    }
}