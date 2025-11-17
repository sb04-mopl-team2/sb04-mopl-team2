package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final PlaylistMapper playlistMapper;

    public PlaylistService(UserRepository userRepository, PlaylistRepository playlistRepository, PlaylistMapper playlistMapper) {
        this.userRepository = userRepository;
        this.playlistRepository = playlistRepository;
        this.playlistMapper = playlistMapper;
    }

    @Transactional(readOnly = true)
    public CursorResponsePlaylistDto getAllPlaylists(PlaylistSearchCond cond) {
        return null;
    }

    public PlaylistDto createPlaylist(UUID ownerId, PlaylistCreateRequest request) {
        log.info("[플레이리스트] 플레이리스트 생성 시작");
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 유저 검증 실패 - 유저 ID={}", ownerId);
                    return new EntityNotFoundException("커스텀 예외로 대체 예정");
                });

        Playlist playlist = Playlist.builder()
                .user(user)
                .title(request.title())
                .description(request.description())
                .playlistItems(new ArrayList<>()) // 빈 리스트 생성
                .subscriberCount(0)
                .subscribedByMe(false)
                .build();

        Playlist saved = playlistRepository.save(playlist);
        log.info("[플레이리스트] 플레이리스트 생성 완료 - 플레이리스트 제목 = {}", saved.getTitle());
        return playlistMapper.toPlaylistDto(saved);
    }
}
