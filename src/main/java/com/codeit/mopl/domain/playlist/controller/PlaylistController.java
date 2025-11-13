package com.codeit.mopl.domain.playlist.controller;

import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public Playlist createPlaylist(@Valid @RequestBody PlaylistCreateRequest request ) {
        
    }

}
