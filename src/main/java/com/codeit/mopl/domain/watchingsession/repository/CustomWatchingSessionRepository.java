package com.codeit.mopl.domain.watchingsession.repository;

import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import java.util.List;
import java.util.UUID;

// querydsl로 수행할 메서드
public interface CustomWatchingSessionRepository {

  List<WatchingSession> findWatchingSessions(
      UUID contentId,
      String watcherNameLike,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      SortBy sortBy
  );

  long getWatcherCount(
      UUID contentId,
      String watcherNameLike
  );

}
