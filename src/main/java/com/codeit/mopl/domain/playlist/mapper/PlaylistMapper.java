package com.codeit.mopl.domain.playlist.mapper;

import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.mapper.PlaylistItemMapper;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, PlaylistItemMapper.class})
public interface PlaylistMapper {

    @Mapping(source = "user", target = "owner")
    @Mapping(source = "playlistItems", target = "contents")
    PlaylistDto toPlaylistDto(Playlist entity);

}
