package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.playlist.dto.CursorResponsePlaylistDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaylistService {

    @Transactional(readOnly = true)
    public CursorResponsePlaylistDto getAllPlaylists(PlaylistSearchCond cond) {
        return null;
    }

}
