package com.codeit.mopl.domain.playlist.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import com.codeit.mopl.domain.playlist.playlistitem.PlaylistItem;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "playlist")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Playlist extends UpdatableEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PlaylistItem> playlistItems = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(nullable = false)
    private long subscriberCount;

    @Column(nullable = false)
    private boolean subscribedByMe;
}
