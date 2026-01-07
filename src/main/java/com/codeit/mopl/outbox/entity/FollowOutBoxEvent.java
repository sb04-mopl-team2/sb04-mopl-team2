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
@Table(name = "follow_outbox_events")
public class FollowOutBoxEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "follow_id", nullable = false)
    private UUID followId;

    @Column(name = "followee_id", nullable = false)
    private UUID followeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_status", nullable = false)
    private OutBoxStatus outBoxStatus = OutBoxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public static FollowOutBoxEvent increase(UUID followId, UUID followeeId) {
        FollowOutBoxEvent e = new FollowOutBoxEvent();
        e.eventType = EventType.FOLLOWER_INCREASE;
        e.followId = followId;
        e.followeeId = followeeId;
        e.outBoxStatus = OutBoxStatus.PENDING;
        e.retryCount = 0;
        return e;
    }

    public static FollowOutBoxEvent decrease(UUID followId, UUID followeeId) {
        FollowOutBoxEvent e = new FollowOutBoxEvent();
        e.eventType = EventType.FOLLOWER_DECREASE;
        e.followId = followId;
        e.followeeId = followeeId;
        e.outBoxStatus = OutBoxStatus.PENDING;
        e.retryCount = 0;
        return e;
    }

    public void markPublished() {
        this.outBoxStatus = OutBoxStatus.PUBLISHED;
    }

    public void markFailed() {
        this.retryCount++;
        this.outBoxStatus = OutBoxStatus.FAILED;
    }
}
