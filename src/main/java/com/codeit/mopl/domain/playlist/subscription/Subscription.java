package com.codeit.mopl.domain.playlist.subscription;

import com.codeit.mopl.domain.base.UpdatableEntity;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "playlist_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "subscriber_id"}))
public class Subscription extends UpdatableEntity {

    @ManyToOne
    private Playlist playlist;

    @ManyToOne
    private User subscriber;

    private LocalDateTime subscribedAt;
}
