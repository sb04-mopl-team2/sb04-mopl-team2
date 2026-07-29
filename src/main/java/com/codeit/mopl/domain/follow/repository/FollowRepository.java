package com.codeit.mopl.domain.follow.repository;

import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    List<Follow> findByFolloweeId(UUID ownerId);

    @Query("SELECT f FROM Follow f WHERE f.followStatus = :followStatus")
    List<Follow> findByStatus(@Param("followStatus") FollowStatus followStatus);
}
