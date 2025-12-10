package com.codeit.mopl.domain.playlist.playlistitem.service;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.notification.template.NotificationMessage;
import com.codeit.mopl.domain.notification.template.NotificationTemplate;
import com.codeit.mopl.domain.notification.template.context.DirectMessageContext;
import com.codeit.mopl.domain.notification.template.context.PlaylistContentAddedContext;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.playlistitem.repository.PlaylistItemRepository;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.entity.Subscription;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.exception.content.ContentErrorCode;
import com.codeit.mopl.exception.content.ContentNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistItemNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.PlaylistUpdateForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PlaylistItemService {

    private final PlaylistItemRepository playlistItemRepository;
    private final PlaylistRepository playlistRepository;
    private final ContentRepository contentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    public void addContent(UUID playlistId, UUID contentId, UUID ownerId) {
        log.info("[플레이리스트] 플레이리스트에 콘텐츠 추가 시작 - playlistId = {}", playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트에 콘텐츠 추가 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}", playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });
        Content content = contentRepository.findById(contentId)
                .orElseThrow(()-> {
                    log.warn("[플레이리스트] 플레이리스트에 콘텐츠 추가 실패 - 콘텐츠가 존재하지 않음 - contentId = {}", contentId);
                    return new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
                });

        if (!ownerId.equals(playlist.getUser().getId())) {
            log.warn("[플레이리스트] 플레이리스트 콘텐츠 추가 실패 - 플레이리스트 변경 권한 없음 - userId = {}", ownerId);
            throw new PlaylistUpdateForbiddenException(playlistId);
        }
        PlaylistItem playlistItem = new PlaylistItem(playlist, content);
        log.info("[플레이리스트] 플레이리스트에 콘텐츠 추가 완료 - playlistId = {}, contentId = {}", playlistId, contentId);
        playlistItemRepository.save(playlistItem);

        // 구독자들에게 알림 생성함 (동기 처리)
        List<Subscription> subscriptions = subscriptionRepository.findByPlaylistId(playlistId);

        PlaylistContentAddedContext ctx =
            new PlaylistContentAddedContext(playlist.getTitle(), content.getTitle());

        NotificationTemplate template = NotificationTemplate.PLAYLIST_CONTENT_ADDED;
        NotificationMessage message = template.build(ctx);

        for (Subscription subscription : subscriptions) {
            UUID subscriberId = subscription.getSubscriber().getId();

            // 본인 플레이리스트에 추가한 경우 알림 제외
            if (!subscriberId.equals(ownerId)) {
                notificationService.createNotification(
                    subscriberId,
                    message.title(),
                    message.content(),
                    Level.INFO
                );
            }
        }
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
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트에서 콘텐츠 삭제 실패 - 해당 콘텐츠가 플레이리스트 내 존재하지 않음 - playlistId = {}, contentId = {}",
                            playlistId, contentId);
                    return PlaylistItemNotFoundException.withId(contentId);
                });
        playlistItemRepository.delete(playlistItem);
        log.info("[플레이리스트] 플레이리스트에서 콘텐츠 삭제 완료 - playlistId = {}, contentId = {}", playlistId, contentId);
    }
}
