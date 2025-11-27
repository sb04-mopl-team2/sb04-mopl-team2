package com.codeit.mopl.event.repository;

import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

  Optional<ProcessedEvent> findByEventIdAndEventType(@NotNull UUID eventId, EventType eventType);
  boolean existsByEventIdAndEventType(@NotNull UUID eventId, EventType eventType);
}

