package com.codeit.mopl.domain.playlist.playlistitem.service;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.playlistitem.mapper.PlaylistItemMapper;
import com.codeit.mopl.domain.playlist.playlistitem.repository.PlaylistItemRepository;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.event.event.PlaylistContentAddedEvent;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistItemService {

    private final PlaylistItemRepository playlistItemRepository;
    private final PlaylistRepository playlistRepository;
    private final ContentRepository contentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void addContent(UUID playlistId, UUID contentId, UUID ownerId) {
        log.info("[플레이리스트] 플레이리스트에 콘텐츠 추가 시작 - playlistId = {}", playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트에 콘텐츠 추가 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });
        Content content = contentRepository.findById(contentId)
                .orElseThrow(()-> new EntityNotFoundException("커스텀 예외 추가되면 대체 예정"));

        if (!ownerId.equals(playlist.getUser().getId())) {
            log.warn("[플레이리스트] 플레이리스트 콘텐츠 추가 실패 실패 - 플레이리스트 변경 권한 없음 - userId = {}", ownerId);
            throw new PlaylistUpdateForbiddenException(playlistId);
        }
        PlaylistItem playlistItem = new PlaylistItem(playlist, content);
        log.info("[플레이리스트] 플레이리스트에 콘텐츠 추가 완료 - playlistId = {}, contentId = {}", playlistId, contentId);
        eventPublisher.publishEvent(new PlaylistContentAddedEvent(playlistId, contentId, ownerId));
        playlistItemRepository.save(playlistItem);
    }

    public void deleteContent(UUID playlistId, UUID contentId, UUID requestUserId) {
        log.info("[플레이리스트] 플레이리스트에서 콘텐츠 삭제 시작 - playlistId = {}", playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트에서 콘텐츠 삭제 중 조회 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });

        if (!requestUserId.equals(playlist.getUser().getId())) {
            log.warn("[플레이리스트] 플레이리스트에서 콘텐츠 삭제 실패 - 권한 없음 - userId = {}", requestUserId);
            throw new PlaylistUpdateForbiddenException(playlistId);
        }
        PlaylistItem playlistItem = playlistItemRepository
                .findByPlaylistIdAndContentId(playlistId, contentId)
                .orElseThrow(() -> new EntityNotFoundException("해당 콘텐츠가 이 플레이리스트에 존재하지 않습니다"));
        playlistItemRepository.delete(playlistItem);
        log.info("[플레이리스트] 플레이리스트에서 콘텐츠 삭제 완료 - playlistId = {}, contentId = {}", playlistId, contentId);
    }
}
