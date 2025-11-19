package com.codeit.mopl.domain.playlist.controller;

import co.elastic.clients.elasticsearch.security.get_token.AuthenticatedUser;
import com.codeit.mopl.domain.playlist.dto.*;
import com.codeit.mopl.domain.playlist.playlistitem.service.PlaylistItemService;
import com.codeit.mopl.domain.playlist.service.PlaylistService;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;
    private final PlaylistItemService playlistItemService;

    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            @Valid @RequestBody PlaylistCreateRequest request ) {
        log.info("[플레이리스트] 플레이리스트 생성 요청 - userId = {}", loginUser.getUser().id());
        PlaylistDto response = playlistService.createPlaylist(loginUser.getUser().id(), request);
        log.info("[플레이리스트] 플레이리스트 생성 응답 - playlistId = {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CursorResponsePlaylistDto> getPlaylists(@Validated @ModelAttribute PlaylistSearchCond request) {
        log.info("[플레이리스트] 플레이리스트 목록 조회 요청 - keyword = {}, ownerId = {}, subscriberId = {}, cursor = {}",
                request.getKeywordLike(), request.getOwnerIdEqual(), request.getSubscriberIdEqual(), request.getCursor());
        CursorResponsePlaylistDto response = playlistService.getAllPlaylists(request);
        log.info("[플레이리스트] 플레이리스트 목록 조회 응답 - totalCount={}, hasNext={}, nextCursor={}",
                response.totalCount(), response.hasNext(), response.nextCursor());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(@PathVariable UUID playlistId) {
        log.info("[플레이리스트] 플레이리스트 단건 조회 요청 - playlistId = {}", playlistId);
        PlaylistDto response = playlistService.getPlaylist(playlistId);
        log.info("[플레이리스트] 플레이리스트 단건 조회 응답 - playlistId = {}", response.id());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> updatePlaylist(@PathVariable UUID playlistId,
                                                      @AuthenticationPrincipal CustomUserDetails loginUser,
                                                      @Valid @RequestBody PlaylistUpdateRequest request) {
        log.info("[플레이리스트] 플레이리스트 정보 수정 요청 - playlistId = {}", playlistId);
        PlaylistDto response = playlistService.updatePlaylist(playlistId, loginUser.getUser().id(), request);
        log.info("[플레이리스트] 플레이리스트 정보 수정 응답 - playlistId = {}", response.id());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID playlistId,
                               @AuthenticationPrincipal CustomUserDetails loginUser) {
        log.info("[플레이리스트] 플레이리스트 삭제 요청 - playlistId = {}, userId = {}", playlistId, loginUser.getUser().id());
        playlistService.deletePlaylist(playlistId, loginUser.getUser().id());
        log.info("[플레이리스트] 플레이리스트 삭제 응답 - playlistId = {}", playlistId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContentToPlaylist(@PathVariable UUID playlistId,
                                                     @PathVariable UUID contentId,
                                                     @AuthenticationPrincipal CustomUserDetails loginUser) {
        log.info("[플레이리스트] 플레이리스트에 콘텐츠 추가 요청 - playlistId = {}, contentId = {}, userId = {}",
                playlistId, contentId, loginUser.getUser().id());
        playlistItemService.addContent(playlistId, contentId, loginUser.getUser().id());
        log.info("[플레이리스트] 플레이리스트 콘텐츠 추가 응답 - playlistId = {}, contentId = {}", playlistId, contentId);
        return ResponseEntity.ok().build();
    }
}
