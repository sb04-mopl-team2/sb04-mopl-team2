package com.codeit.mopl.domain.playlist.mapper;

import com.codeit.mopl.domain.base.FrontendKstOffsetAdjuster;
import com.codeit.mopl.domain.playlist.dto.PlaylistCachedDto;
import com.codeit.mopl.domain.playlist.dto.PlaylistDto;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.playlistitem.mapper.PlaylistItemMapper;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, PlaylistItemMapper.class, FrontendKstOffsetAdjuster.class})
public interface PlaylistMapper {

    @Mapping(source = "cached.updatedAt", target = "updatedAt", qualifiedByName = "adjustForFrontend")
    PlaylistDto toPlaylistDto(PlaylistCachedDto cached, boolean subscribedByMe);

    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "user", target = "owner")
    @Mapping(source = "playlistItems", target = "contents")
    PlaylistCachedDto toCachedDto(Playlist entity);
}
