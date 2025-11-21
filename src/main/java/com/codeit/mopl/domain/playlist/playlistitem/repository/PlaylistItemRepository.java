package com.codeit.mopl.domain.playlist.playlistitem.repository;

import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, UUID> {
    Optional<PlaylistItem> findByPlaylistIdAndContentId(UUID playlistId, UUID contentId);
}
