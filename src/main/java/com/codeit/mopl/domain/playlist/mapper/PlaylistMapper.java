package com.codeit.mopl.domain.playlist.mapper;

import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    //매핑 추가할 예정
    PlaylistDto toPlaylistDto(Playlist entity);
}
