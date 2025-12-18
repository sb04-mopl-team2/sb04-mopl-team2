package com.codeit.mopl.domain.content.repository;

import com.codeit.mopl.domain.content.entity.Content;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom {
  @Modifying
  @Query("UPDATE Content c SET c.watcherCount = c.watcherCount + 1 WHERE c.id = :contentId")
  void incrementWatcherCount(@Param("contentId") UUID contentId);

  @Modifying
  @Query("UPDATE Content c SET c.watcherCount = c.watcherCount - 1 " +
      "WHERE c.id = :contentId AND c.watcherCount > 0")
  void decrementWatcherCount(@Param("contentId") UUID contentId);
}
