package com.codeit.mopl.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@Table(name = "processed_events")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class ProcessedEvent {

  @Id
  @Column(name = "event_id", nullable = false, updatable = false)
  private UUID eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventType eventType;

  @CreatedDate
  @Column(name = "eventTime", updatable = false, nullable = false)
  private LocalDateTime eventTime;

  public ProcessedEvent(UUID id, EventType eventType) {
    this.eventId = id;
    this.eventType = eventType;
  }
}
