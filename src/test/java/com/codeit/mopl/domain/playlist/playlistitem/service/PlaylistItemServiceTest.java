package com.codeit.mopl.domain.playlist.playlistitem.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaylistItemServiceTest {

    @Test
    @DisplayName("PlaylistItemService가 정상적으로 생성된다")
    void serviceCreation() {
        // when
        PlaylistItemService service = new PlaylistItemService();

        // then
        assertThat(service).isNotNull();
    }
}