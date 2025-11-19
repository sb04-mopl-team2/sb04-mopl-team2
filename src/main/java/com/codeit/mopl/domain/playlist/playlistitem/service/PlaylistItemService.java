package com.codeit.mopl.domain.playlist.playlistitem.service;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.playlistitem.mapper.PlaylistItemMapper;
import com.codeit.mopl.domain.playlist.playlistitem.repository.PlaylistItemRepository;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistItemService {

    private final PlaylistItemRepository playlistItemRepository;
    private final PlaylistItemMapper playlistItemMapper;
    private final PlaylistRepository playlistRepository;
    private final ContentRepository contentRepository;

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
        playlistItemRepository.save(playlistItem);
    }
}
