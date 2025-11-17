package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistCreateRequest;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.ErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public PlaylistDto createPlaylist(UUID ownerId, PlaylistCreateRequest request) {
        log.info("[플레이리스트] 플레이리스트 생성 시작");
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 유저 검증 실패 - 유저 ID={}", ownerId);
                    return new UserNotFoundException(ErrorCode.USER_NOT_FOUND, Map.of("userId", ownerId));
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

    @Transactional(readOnly = true)
    public CursorResponsePlaylistDto getAllPlaylists(PlaylistSearchCond cond) {
        log.info("[플레이리스트] 플레이리스트 목록 조회 시작");
        List<Playlist> playlists = playlistRepository.findAllByCond(cond);
        int originalSize = playlists.size();

        String nextCursor = null;
        if (playlists.size() > cond.getLimit()) {
            nextCursor = playlists.get(cond.getLimit() - 1).getId().toString();
            playlists = playlists.subList(0, cond.getLimit());
        }

        List<PlaylistDto> playlistDtos =
                playlists.stream().map(playlistMapper::toPlaylistDto).collect(Collectors.toList());

        UUID nextIdAfter = nextCursor != null ? UUID.fromString(nextCursor) : null;
        boolean hasNext = originalSize > cond.getLimit();
        long totalCount = playlistRepository.countAllByCond(cond);
        log.info("[플레이리스트] 플레이리스트 목록 조회 완료 - totalCount = {}", totalCount);
        return new CursorResponsePlaylistDto(
                playlistDtos,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                cond.getSortBy(),
                cond.getSortDirection()
        );
    }
}
