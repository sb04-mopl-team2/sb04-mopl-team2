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
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    return new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", ownerId));
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

        // 빈 리스트에 대한 체크
        if (playlists.isEmpty()) {
            log.info("[플레이리스트] 플레이리스트 목록 조회 완료 - 결과 없음");
            return new CursorResponsePlaylistDto(
                    new ArrayList<>(),
                    null,
                    null,
                    false,
                    0L,
                    cond.getSortBy(),
                    cond.getSortDirection()
            );
        }
        int originalSize = playlists.size();
        boolean hasNext = originalSize > cond.getLimit();

        // findAllByCond에서 limit + 1로 조회했으므로 hasNext가 true 이면 마지막 항목 제거
        List<Playlist> resultPlaylists = hasNext ? playlists.subList(0, cond.getLimit()) : playlists;

        Playlist lastPlaylist = resultPlaylists.get(resultPlaylists.size() - 1);
        String nextCursor = hasNext ? lastPlaylist.getCreatedAt().toString() : null;
        UUID nextIdAfter = hasNext ? lastPlaylist.getId() : null;

        List<PlaylistDto> playlistDtos =
                resultPlaylists.stream().map(playlistMapper::toPlaylistDto).collect(Collectors.toList());

        long totalCount = playlistRepository.countAllByCond(cond.withoutCursor());
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

    @Transactional(readOnly = true)
    public PlaylistDto getPlaylist(UUID playlistId) {
        log.info("[플레이리스트] 플레이리스트 단건 조회 시작 - playlistId = {}", playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트 조회 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });
        log.info("[플레이리스트] 플레이리스트 단건 조회 완료 - playlistId = {}", playlistId);
        return playlistMapper.toPlaylistDto(playlist);
    }
}
