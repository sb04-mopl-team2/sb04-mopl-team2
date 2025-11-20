package com.codeit.mopl.domain.playlist.subscription.repository;

import com.codeit.mopl.domain.playlist.subscription.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsBySubscriberIdAndPlaylistId(UUID subscriberId, UUID playlistId);
}
