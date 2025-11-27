package com.codeit.mopl.event.entity;

import com.codeit.mopl.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "processed_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processed_events_event_id_type",
                        columnNames = {"event_id", "event_type"}
                )
        })
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class ProcessedEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    public ProcessedEvent(UUID id, EventType eventType) {
        this.eventId = id;
        this.eventType = eventType;
    }
}
