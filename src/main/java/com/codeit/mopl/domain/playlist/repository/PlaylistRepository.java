package com.codeit.mopl.domain.playlist.repository;

import com.codeit.mopl.domain.playlist.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID>,
CustomPlaylistRepository {

}
