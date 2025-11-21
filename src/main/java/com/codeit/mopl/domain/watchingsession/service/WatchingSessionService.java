package com.codeit.mopl.domain.watchingsession.service;

import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.domain.watchingsession.mapper.WatchingSessionMapper;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.exception.watchingsession.WatchingSessionErrorCode;
import com.codeit.mopl.exception.watchingsession.ContentNotFoundException;
import com.codeit.mopl.exception.watchingsession.WatchingSessionNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.mopl.exception.user.UserNotFoundException;

/*
   Controller에서 불려지는 조회용 함수들
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final WatchingSessionMapper watchingSessionMapper;
  private final ContentRepository contentRepository;

  @Transactional(readOnly = true)
  public WatchingSessionDto getByUserId(UUID userId) {
    log.info("[실시간 세션] 서비스: 사용자 ID로 시청 세션 조회 시작. userId = {}", userId);
    WatchingSession watchingSession = watchingSessionRepository.findByUserId(userId)
        .orElseThrow(() -> {
          log.warn("해당 userId의 실시간 세션을 찾을 수 없음 userId = {}", userId);
          return new WatchingSessionNotFoundException(
              WatchingSessionErrorCode.WATCHING_SESSION_NOT_FOUND,
              Map.of("userId",userId)
          );
        });
    log.info("[실시간 세션] 서비스: 사용자 시청 세션 조회 및 DTO 변환 완료. userId = {}", userId);
    return watchingSessionMapper.toDto(watchingSession);
  }

  @Transactional(readOnly = true)
  public CursorResponseWatchingSessionDto getWatchingSessions(
      UUID userId,
      UUID contentId,
      String watcherNameLike,
      String cursor,
      UUID idAfter,
      Integer limit,
      SortDirection sortDirection,
      SortBy sortBy
  ) {
    log.info(
        "[실시간 세션] 서비스: 특정 콘텐츠의 시청 세션 목록 조회 시작. contentId = {}, userId = {}",
        contentId, userId
    );
    if (!contentRepository.existsById(contentId)) {
      throw new ContentNotFoundException(
          WatchingSessionErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
    }
    int effectiveLimit = (limit != null) ? limit : 20;
    int internalLimit = effectiveLimit + 1;
    List<WatchingSession> watchingSessions = watchingSessionRepository.findWatchingSessions(
        userId,
        contentId,
        watcherNameLike,
        cursor,
        idAfter,
        internalLimit, // for cursor
        sortDirection,
        sortBy);
    long totalCount = watchingSessionRepository.getWatcherCount(
        userId,
        contentId,
        watcherNameLike
    );

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = watchingSessions.size() > effectiveLimit;
    if (hasNext) {
      // get extra & get nextCursor, nextIdAfter
      WatchingSession lastWatchingSession = watchingSessions.get(effectiveLimit);
      nextCursor = lastWatchingSession.getCreatedAt().toString();
      nextIdAfter = lastWatchingSession.getId();
      // remove the extra
      watchingSessions.remove(effectiveLimit);
    }

    log.info(
        "[실시간 세션] 서비스: 특정 콘텐츠의 시청 세션 목록 조회 완료. contentId = {}", contentId
    );
    return new CursorResponseWatchingSessionDto(
        watchingSessions.stream()
            .map(watchingSessionMapper::toDto).toList(),
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortBy,
        sortDirection
    );
  }
}
