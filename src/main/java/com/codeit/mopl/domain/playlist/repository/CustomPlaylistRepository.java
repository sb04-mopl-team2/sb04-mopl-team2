package com.codeit.mopl.domain.playlist.repository;

import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;

import java.util.List;

public interface CustomPlaylistRepository {

    List<Playlist> findAllByCond(PlaylistSearchCond cond);

}
