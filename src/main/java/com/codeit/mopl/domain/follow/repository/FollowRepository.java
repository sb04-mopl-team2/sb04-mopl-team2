package com.codeit.mopl.domain.follow.repository;

import com.codeit.mopl.domain.follow.entity.Follow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  List<Follow> findByFolloweeId(UUID ownerId);
}
