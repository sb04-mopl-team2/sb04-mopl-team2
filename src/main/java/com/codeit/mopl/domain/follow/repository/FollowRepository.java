package com.codeit.mopl.domain.follow.repository;

import com.codeit.mopl.domain.follow.entity.Follow;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    List<Follow> findByFolloweeId(UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Follow f WHERE f.id = :id")
    Optional<Follow> findByIdForUpdate(@Param("id") UUID followId);
}
