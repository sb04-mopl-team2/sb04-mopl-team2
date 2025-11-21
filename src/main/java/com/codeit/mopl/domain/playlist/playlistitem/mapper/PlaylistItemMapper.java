package com.codeit.mopl.domain.playlist.playlistitem.mapper;

import com.codeit.mopl.domain.content.dto.response.ContentSummary;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlaylistItemMapper {

    @Mapping(source = "content.id", target ="id")
    @Mapping(source = "content.contentType.type", target = "type")
    @Mapping(source = "content.title", target = "title")
    @Mapping(source = "content.description", target = "description")
    @Mapping(source = "content.thumbnailUrl", target = "thumbnailUrl")
    @Mapping(source = "content.tags", target = "tags")
    @Mapping(source = "content.averageRating", target = "averageRating")
    @Mapping(source = "content.reviewCount", target = "reviewCount")
    ContentSummary toSummary(PlaylistItem playlistItem);
}
