package com.codeit.mopl.outbox.entity;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.event.entity.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follow_outbox_events")
public class FollowOutBoxEvent extends BaseEntity {
    public static final int MAX_RETRY_COUNT = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "follow_id", nullable = false)
    private UUID followId;

    @Column(name = "followee_id", nullable = false)
    private UUID followeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_status", nullable = false)
    private OutBoxStatus outBoxStatus = OutBoxStatus.REQUESTED;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    public static FollowOutBoxEvent increase(UUID followId, UUID followeeId) {
        FollowOutBoxEvent e = new FollowOutBoxEvent();
        e.eventType = EventType.FOLLOWER_INCREASE;
        e.followId = followId;
        e.followeeId = followeeId;
        e.outBoxStatus = OutBoxStatus.REQUESTED;
        e.retryCount = 0;
        return e;
    }

    public static FollowOutBoxEvent decrease(UUID followId, UUID followeeId) {
        FollowOutBoxEvent e = new FollowOutBoxEvent();
        e.eventType = EventType.FOLLOWER_DECREASE;
        e.followId = followId;
        e.followeeId = followeeId;
        e.outBoxStatus = OutBoxStatus.REQUESTED;
        e.retryCount = 0;
        return e;
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
