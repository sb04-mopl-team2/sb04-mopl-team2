package com.codeit.mopl.domain.playlist.controller;

import co.elastic.clients.elasticsearch.security.get_token.AuthenticatedUser;
import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
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

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;

    @GetMapping
    public ResponseEntity<CursorResponsePlaylistDto> getPlaylists(@Validated PlaylistSearchCond request) {
        CursorResponsePlaylistDto response = playlistService.getAllPlaylists(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            @Valid @RequestBody PlaylistCreateRequest request ) {
        log.info("[플레이리스트] 플레이리스트 생성 요청 - userId = {}", loginUser.getUser().id());
        PlaylistDto response = playlistService.createPlaylist(loginUser.getUser().id(), request);
        log.info("[플레이리스트] 플레이리스트 생성 응답 - playlistId = {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
