package com.codeit.mopl.domain.watchingsession.service;

import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.domain.watchingsession.mapper.WatchingSessionMapper;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
   Controller에서 불려지는 조회용 함수들
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final WatchingSessionMapper watchingSessionMapper;

  @Transactional(readOnly = true)
  public WatchingSessionDto getByUserId(UUID userId) {
    log.info("[실시간 세션] 서비스: 사용자 ID로 시청 세션 조회 시작. userId = {}", userId);
    WatchingSession watchingSession = watchingSessionRepository.findByUserId(userId)
        .orElseThrow(() -> {
          log.warn("해당 유저를 찾을 수 없음 userId = {}", userId);
          return new UsernameNotFoundException("userId not found");
        });
    log.info("[실시간 세션] 서비스: 사용자 시청 세션 조회 및 DTO 변환 완료. userId = {}", userId);
    return watchingSessionMapper.toDto(watchingSession);
  }

  // TODO : contentSummary 관련 코드 수정 (대문자)
  @Transactional(readOnly = true)
  public CursorResponseWatchingSessionDto getWatchingSessions(
      UUID userId,
      UUID contentId,
      String watcherNameLike,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy
  ) {
    log.info(
        "[실시간 세션] 서비스: 특정 콘텐츠의 시청 세션 목록 조회 시작. contentId = {}, userId = {}",
        contentId, userId
    );
    int internalLimit = limit + 1;
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
        contentId,
        userId,
        watcherNameLike
    );

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = watchingSessions.size() > limit;
    if (hasNext) {
      // get extra & get nextCursor, nextIdAfter
      WatchingSession lastWatchingSession = watchingSessions.get(limit);
      nextCursor = lastWatchingSession.getCreatedAt().toString();
      nextIdAfter = lastWatchingSession.getId();
      // remove the extra
      watchingSessions.remove(limit);
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
