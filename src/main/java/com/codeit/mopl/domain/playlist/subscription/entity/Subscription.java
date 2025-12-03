package com.codeit.mopl.domain.playlist.subscription.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "playlist_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "subscriber_id"}))
@Builder
public class Subscription extends UpdatableEntity {

    @ManyToOne
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne
    private User subscriber;

    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;
}
