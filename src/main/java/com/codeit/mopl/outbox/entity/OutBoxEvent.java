package com.codeit.mopl.outbox.entity;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.event.entity.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_events")
public class OutBoxEvent extends BaseEntity {
    public static final int MAX_RETRY_COUNT = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private AggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_status", nullable = false)
    private OutBoxStatus outBoxStatus = OutBoxStatus.REQUESTED;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error_message", length = 4000)
    private String lastErrorMessage;

    public OutBoxEvent(EventType eventType, AggregateType aggregateType, UUID aggregateId, String payload) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public void markPublished() {
        this.retryCount = 0;
        this.outBoxStatus = OutBoxStatus.PUBLISHED;
        this.lastErrorMessage = null;
    }

    public void markFailed(String lastErrorMessage) {
        this.retryCount++;
        this.outBoxStatus = OutBoxStatus.FAILED;
        this.lastErrorMessage = lastErrorMessage;
    }

    public void markDead(String lastErrorMessage) {
        this.outBoxStatus = OutBoxStatus.DEAD;
        this.lastErrorMessage = lastErrorMessage;
    }

    public void markRequested() {
        this.retryCount = 0;
        this.outBoxStatus = OutBoxStatus.REQUESTED;
        this.lastErrorMessage = null;
    }
}
