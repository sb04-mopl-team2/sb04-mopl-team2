package com.codeit.mopl.domain.playlist.service;

import com.codeit.mopl.domain.playlist.repository.PlaylistRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlaylistServiceTest {

    @Mock private PlaylistRepository playlistRepository;
    @InjectMocks private PlaylistService playlistService;

}
