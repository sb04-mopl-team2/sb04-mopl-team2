package com.codeit.mopl.batch.tmdb.tvseries.mapper;

import com.codeit.mopl.batch.tmdb.tvseries.TvGenre;
import com.codeit.mopl.batch.tmdb.tvseries.dto.TmdbDiscoverTvResponse.TvShow;
import com.codeit.mopl.domain.content.entity.Content;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TmdbTvMapper {

  String DEFAULT_THUMBNAIL_URL = "https://buly.kr/BIVulPE";

  String BASE_THUMBNAIL_URL = "https://image.tmdb.org/t/p/w500";

  @Mapping(source = "name", target = "title")
  @Mapping(source = "overview", target = "description")
  @Mapping(source = "posterPath", target = "thumbnailUrl", qualifiedByName = "buildThumbnailUrl")
  @Mapping(source = "genreIds", target = "tags", qualifiedByName = "mapGenresToTags")
  @Mapping(target = "contentType", expression = "java(com.codeit.mopl.domain.content.entity.ContentType.TV)")
  @Mapping(target = "averageRating", ignore = true)
  @Mapping(target = "reviewCount", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "watcherCount", ignore = true)
  Content toContent(TvShow tvShow);

  @Named("mapGenresToTags")
  default List<String> mapGenresToTags(List<Integer> genreIds) {
    if (genreIds == null) return List.of();
    return genreIds.stream()
        .map(TvGenre::fromId)
        .map(TvGenre::getTag)
        .collect(Collectors.toList());
  }

  @Named("buildThumbnailUrl")
  default String buildThumbnailUrl(String posterPath) {
    if (posterPath == null || posterPath.isEmpty()) {
      return DEFAULT_THUMBNAIL_URL;
    }
    return BASE_THUMBNAIL_URL + posterPath;
  }
}