package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.playlist.dto.*;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.mapper.PlaylistMapper;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.PlayListCreateEvent;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final PlaylistMapper playlistMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionRepository subscriptionRepository;

    private final StringRedisTemplate stringRedisTemplate;
    private static final String PLAYLIST_DERAIL = "playlist:detail";


    public PlaylistDto createPlaylist(UUID ownerId, PlaylistCreateRequest request) {
        log.info("[플레이리스트] 플레이리스트 생성 시작");
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 유저 검증 실패 - userId = {}", ownerId);
                    return new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", ownerId));
                });

        Playlist playlist = Playlist.builder()
            .user(user)
            .title(request.title())
            .description(request.description())
            .playlistItems(new ArrayList<>())
            .subscriberCount(0)
            .build();

        Playlist saved = playlistRepository.save(playlist);
        eventPublisher.publishEvent(new PlayListCreateEvent(saved.getId(), ownerId, saved.getTitle()));
        PlaylistCachedDto cached = playlistMapper.toCachedDto(saved);
        log.info("[플레이리스트] 플레이리스트 생성 완료 - 플레이리스트 제목 = {}", cached.title());
        return playlistMapper.toPlaylistDto(cached, false);
    }

    @Transactional(readOnly = true)
    public CursorResponsePlaylistDto getAllPlaylists(UUID loginUserId,PlaylistSearchCond cond) {
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
            resultPlaylists.stream()
                    .map(playlist -> {
                        PlaylistCachedDto cached = playlistMapper.toCachedDto(playlist);
                        boolean subscribed = subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlist.getId());
                        return playlistMapper.toPlaylistDto(cached, subscribed);
                    })
                    .collect(Collectors.toList());

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
    @Cacheable(
            cacheNames = PLAYLIST_DERAIL,
            key = "#playlistId"
    )
    public PlaylistCachedDto getPlaylistCached(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(()-> {
                    log.warn("[플레이리스트 캐시] 플레이리스트 조회 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });
        return playlistMapper.toCachedDto(playlist);
    }

    @Transactional(readOnly = true)
    public PlaylistDto getPlaylist(UUID loginUserId,UUID playlistId) {
        log.info("[플레이리스트] 플레이리스트 단건 조회 시작 - playlistId = {}", playlistId);
        PlaylistCachedDto cached = getPlaylistCached(playlistId);
        boolean subscribed = subscriptionRepository.existsBySubscriberIdAndPlaylistId(loginUserId, playlistId);

        log.info("[플레이리스트] 플레이리스트 단건 조회 완료 - playlistId = {}", playlistId);
        return playlistMapper.toPlaylistDto(cached, subscribed);
    }

    @CacheEvict(value = PLAYLIST_DERAIL,
            key = "#playlistId")
    public PlaylistDto updatePlaylist(UUID requestUserId, UUID playlistId, PlaylistUpdateRequest request) {
        log.info("[플레이리스트] 플레이리스트 정보 수정 시작 - playlistId = {}", playlistId);

        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> {
                log.warn("[플레이리스트] 플레이리스트 정보 수정 중 조회 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                return PlaylistNotFoundException.withId(playlistId);
            });
        if (!requestUserId.equals(playlist.getUser().getId())) {
            log.warn("[플레이리스트] 플레이리스트 정보 수정 실패 - 권한 없음 - userId = {}", requestUserId);
            throw new PlaylistUpdateForbiddenException(playlistId);
        }

        playlist.update(request.title(), request.description());
        PlaylistCachedDto cached = playlistMapper.toCachedDto(playlist);
        log.info("[플레이리스트] 플레이리스트 정보 수정 완료 - playlistId = {}", playlistId);
        return playlistMapper.toPlaylistDto(cached, false);
    }

    @CacheEvict(value = PLAYLIST_DERAIL,
            key = "#playlistId")
    public void deletePlaylist(UUID playlistId, UUID requestUserId) {
        log.info("[플레이리스트] 플레이리스트 삭제 시작 - playlistId = {}", playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> {
                log.warn("[플레이리스트] 플레이리스트 삭제 중 조회 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                return PlaylistNotFoundException.withId(playlistId);
            });
        if (!requestUserId.equals(playlist.getUser().getId())) {
            log.warn("[플레이리스트] 플레이리스트 삭제 실패 - 권한 없음 - userId = {}", requestUserId);
            throw new PlaylistUpdateForbiddenException(playlistId);
        }

        subscriptionRepository.deleteByPlaylistId(playlistId);
        playlistRepository.deleteById(playlistId);
        log.info("[플레이리스트] 플레이리스트 삭제 완료 - playlistId = {}", playlistId);
    }
}