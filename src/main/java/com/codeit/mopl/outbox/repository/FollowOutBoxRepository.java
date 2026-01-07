package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.outbox.entity.OutBoxStatus;
import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FollowOutBoxRepository extends JpaRepository<FollowOutBoxEvent, UUID> {

    List<FollowOutBoxEvent> findTop100ByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus outBoxStatus);
}
