package com.codeit.mopl.domain.playlist.controller;

import co.elastic.clients.elasticsearch.security.get_token.AuthenticatedUser;
import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.service.PlaylistService;
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
            @AuthenticationPrincipal LoginUser loginUser,  //인증 부분 완성되면 import할 예정입니다.
            @Valid @RequestBody PlaylistCreateRequest request ) {
        PlaylistDto response = playlistService.createPlaylist(loginUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
