package com.codeit.mopl.domain.watchingsession.repository;

import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID>, CustomWatchingSessionRepository {

  Optional<WatchingSession> findByUserId(UUID userId);

  Long countByContentId(UUID contentId);

  void deleteByContentId(UUID contentId);

  void deleteByUserId(UUID userId);
}
