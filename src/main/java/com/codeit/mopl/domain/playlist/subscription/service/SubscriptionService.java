package com.codeit.mopl.domain.playlist.subscription.service;

import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import com.codeit.mopl.domain.playlist.subscription.Subscription;
import com.codeit.mopl.domain.playlist.subscription.repository.SubscriptionRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.playlist.PlaylistNotFoundException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionNotFoundException;
import com.codeit.mopl.exception.playlist.subscription.SubscriptionSelfProhibitedException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    public void subscribe(UUID playlistId, UUID subscriberId) {
        LocalDateTime subscribedAt = LocalDateTime.now();
        log.info("[플레이리스트] 플레이리스트 구독 처리 시작 - playlistId = {}", playlistId);

        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트 구독 처리 실패 - 플레이리스트가 존재하지 않음 - playlistId = {}",playlistId);
                    return PlaylistNotFoundException.withId(playlistId);
                });

        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> {
                    log.warn("[플레이리스트] 플레이리스트 구독 처리 실패 - userId = {}", subscriberId);
                    return new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", subscriberId));
                });

        if (playlist.getUser().getId().equals(subscriberId)) {
            log.warn("[플레이리스트] 플레이리스트 구독 처리 실패 - 본인의 플레이리스트는 구독할 수 없음 - playlistId = {}", playlistId);
            throw SubscriptionSelfProhibitedException.withId(playlistId,subscriberId);
        }

        if (subscriptionRepository.existsBySubscriberIdAndPlaylistId(subscriberId,playlistId)) {
            log.warn("[플레이리스트] 플레이리스트 구독 처리 실패 - 이미 구독 내역이 존재함 - playlistId = {}, subscriberId = {}", playlistId, subscriberId);
            throw SubscriptionSelfProhibitedException.withId(playlistId,subscriberId);
        }

        Subscription subscription = new Subscription(playlist, subscriber, subscribedAt);
        log.info("[플레이리스트] 플레이리스트 구독 처리 완료 - playlistId = {}, subscriberId = {}", playlistId, subscriberId);
        subscriptionRepository.save(subscription);
    }

    public void unsubscribe(UUID playlistId, UUID subscriberId) {
        log.info("[플레이리스트] 플레이리스트 구독 취소 처리 시작 - playlistId = {}, subscriberId = {}", playlistId, subscriberId);

        Subscription subscription = subscriptionRepository.findBySubscriberIdAndPlaylistId(subscriberId,playlistId)
                        .orElseThrow(()-> {
                            log.warn("[플레이리스트] 플레이리스트 구독 취소 처리 실패 - 구독 내역이 존재하지 않음 - playlistId = {}", playlistId);
                            return SubscriptionNotFoundException.withId(subscriberId, playlistId);
                        });
        log.info("[플레이리스트] 플레이리스트 구독 취소 처리 완료 - playlistId = {}, subscriberId = {}", playlistId, subscriberId);
        subscriptionRepository.delete(subscription);
    }
}
