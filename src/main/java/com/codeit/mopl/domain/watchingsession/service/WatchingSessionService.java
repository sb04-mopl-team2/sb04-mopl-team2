package com.codeit.mopl.domain.watchingsession.service;

import com.codeit.mopl.domain.content.dto.response.ContentSummary;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSessionChange;
import com.codeit.mopl.domain.watchingsession.entity.enums.ChangeType;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.domain.watchingsession.mapper.WatchingSessionMapper;
import com.codeit.mopl.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.mopl.exception.content.ContentErrorCode;
import com.codeit.mopl.exception.content.ContentNotFoundException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.watchingsession.WatchingSessionErrorCode;
import com.codeit.mopl.exception.watchingsession.WatchingSessionNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final UserRepository userRepository;

  /*
    조회용 함수들
   */

  @Transactional(readOnly = true)
  public WatchingSessionDto getByUserId(UUID userId) {
    log.info("[실시간 세션] 서비스: 사용자 ID로 시청 세션 조회 시작. userId = {}", userId);
    Optional<WatchingSession> watchingSession = watchingSessionRepository.findByUserId(userId);
    if (watchingSession.isEmpty()) return null;
    log.info("[실시간 세션] 서비스: 사용자 시청 세션 조회 및 DTO 변환 완료. userId = {}", userId);
    return watchingSessionMapper.toDto(watchingSession.get());
  }

  @Transactional(readOnly = true)
  public CursorResponseWatchingSessionDto getWatchingSessions(
      UUID contentId,
      String watcherNameLike,
      String cursor,
      UUID idAfter,
      Integer limit,
      SortDirection sortDirection,
      SortBy sortBy
  ) {
    log.info(
        "[실시간 세션] 서비스: 특정 콘텐츠의 시청 세션 목록 조회 시작. contentId = {}, watcherNameLike = {}",
        contentId, watcherNameLike
    );
    if (!contentRepository.existsById(contentId)) {
      throw new ContentNotFoundException(
          ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId));
    }
    int effectiveLimit = (limit != null) ? limit : 20;
    int internalLimit = effectiveLimit + 1;
    List<WatchingSession> watchingSessions = watchingSessionRepository.findWatchingSessions(
        contentId,
        watcherNameLike,
        cursor,
        idAfter,
        internalLimit, // for cursor
        sortDirection,
        sortBy);
    long totalCount = watchingSessionRepository.getWatcherCount(
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
    CursorResponseWatchingSessionDto response = new CursorResponseWatchingSessionDto(
        watchingSessions.stream().map(watchingSessionMapper::toDto).toList(),
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortBy.getType(),
        sortDirection
    );
    log.info(
        "[실시간 세션] 서비스: 특정 콘텐츠의 시청 세션 목록 조회 완료. contentId = {}, watcherNameLike = {}, items = {}",
        contentId, watcherNameLike, response.data().size()
    );
    return response;
  }

    /*
      웹소켓용 이벤크 기반 함수들
   */
  @Transactional
  public WatchingSessionChange joinSession(UUID userId, UUID contentId) {
    WatchingSession session = ensureSessionExists(userId, contentId);

    Long watcherCount = watchingSessionRepository.countByContentId(
        contentId);

    return getWatchingSessionChange(
        session,
        session.getUser(),
        ChangeType.JOIN,
        watcherCount);
  }

  @Transactional
  public WatchingSession ensureSessionExists(UUID userId, UUID contentId) {
    Content content = contentRepository.findById(contentId)
        .orElseThrow(() -> new ContentNotFoundException(
            ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(
            UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    Optional<WatchingSession> existingSession = watchingSessionRepository
        .findByUserIdAndContentId(userId, contentId);

    // 웹소켓 조회용
    if (existingSession.isPresent()) {
      WatchingSession session = existingSession.get();
      session.getContent().getTags().size();
      session.getUser();
      session.setUpdatedAt(LocalDateTime.now());
      log.info("[실시간 세션] 서비스: 세션이 이미 존재합니다 - sessionId = {}", session.getId());
      return session;
    }

    // GET 메소드로 인해 새로 생성
    watchingSessionRepository.deleteByUserId(userId);
    watchingSessionRepository.flush();

    WatchingSession watchingSession = new WatchingSession();
    watchingSession.setUser(user);
    watchingSession.setContent(content);
    WatchingSession saved = watchingSessionRepository.save(watchingSession);
    saved.getContent().getTags().size();
    log.info("[실시간 세션] 서비스: 새로운 세션 생성 - sessionId = {}, title = {}, tagsNum = {}",
        saved.getId(), saved.getContent().getTitle(), saved.getContent().getTags().size());

    return saved;
  }

  @Transactional
  public WatchingSessionChange leaveSession(UUID userId, UUID watchingSessionId, UUID contentId) {
    WatchingSession watchingSession = watchingSessionRepository.findById(watchingSessionId)
        .orElseThrow(() -> new WatchingSessionNotFoundException(
            WatchingSessionErrorCode.WATCHING_SESSION_NOT_FOUND, Map.of("watchingSessionId", watchingSessionId))
        );
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(
            UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    watchingSession.getContent().getTags().size();
    watchingSession.getUser();
    watchingSessionRepository.deleteById(watchingSessionId);
    long watcherCount = watchingSessionRepository.countByContentId(contentId);

    // payload
    return getWatchingSessionChange(
        watchingSession, user, ChangeType.LEAVE, watcherCount );
  }

  public WatchingSessionChange getWatchingSessionChange(
      WatchingSession savedWatchingSession, User user, ChangeType changeType, Long watcherCount) {
    Content content = savedWatchingSession.getContent();

    return new WatchingSessionChange(
        changeType,
        new WatchingSessionDto(
            savedWatchingSession.getId(),
            savedWatchingSession.getCreatedAt(),
            new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getProfileImageUrl()
            ),
            new ContentSummary(
                content.getId(),
                content.getContentType().getType(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                content.getTags(),
                content.getAverageRating(),
                content.getReviewCount()
            )
        ),
        watcherCount
    );
  }

}
