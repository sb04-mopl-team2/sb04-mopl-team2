package com.codeit.mopl.event.entity;

import com.codeit.mopl.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@Table(name = "processed_events")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class ProcessedEvent extends BaseEntity {

  @Column(name = "event_id", unique = true, nullable = false, updatable = false)
  private UUID eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventType eventType;

  public ProcessedEvent(UUID id, EventType eventType) {
    this.eventId = id;
    this.eventType = eventType;
  }
}
