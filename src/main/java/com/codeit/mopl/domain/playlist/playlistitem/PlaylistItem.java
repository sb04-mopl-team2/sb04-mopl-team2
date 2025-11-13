package com.codeit.mopl.domain.playlist.playlistitem;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "playlist_item")
public class PlaylistItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    private Content content;
}
