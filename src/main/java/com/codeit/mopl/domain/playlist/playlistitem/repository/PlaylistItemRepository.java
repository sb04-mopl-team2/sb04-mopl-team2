package com.codeit.mopl.domain.playlist.playlistitem.repository;

import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {
}
