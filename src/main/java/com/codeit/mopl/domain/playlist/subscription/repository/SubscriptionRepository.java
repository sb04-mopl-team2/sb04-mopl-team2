package com.codeit.mopl.domain.playlist.subscription.repository;

import com.codeit.mopl.domain.playlist.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsBySubscriberIdAndPlaylistId(UUID subscriberId, UUID playlistId);

    Optional<Subscription> findBySubscriberIdAndPlaylistId(UUID subscriberId, UUID playlistId);

    List<Subscription> findByPlaylistId(UUID playlistId);
}
