package com.codeit.mopl.batch.sportsdb.mapper;

import com.codeit.mopl.batch.sportsdb.dto.SportsDbEventResponse;
import com.codeit.mopl.domain.content.entity.Content;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SportsEventMapper {

  @Mapping(source = "strEvent", target = "title")
  @Mapping(source = "event", target = "description", qualifiedByName = "buildDescription")
  @Mapping(source = "strThumb", target = "thumbnailUrl", qualifiedByName = "buildThumbnailUrl")
  @Mapping(source = "event", target = "tags", qualifiedByName = "buildTags")
  @Mapping(target = "contentType", expression = "java(com.codeit.mopl.domain.content.entity.ContentType.SPORTS)")
  @Mapping(target = "averageRating", constant = "0.0")
  @Mapping(target = "reviewCount", constant = "0")
  @Mapping(target = "watcherCount", constant = "0")
  @Mapping(target = "updatedAt", ignore = true)
  Content toContent(SportsDbEventResponse.Event event);

  @Named("buildDescription")
  default String buildDescription(SportsDbEventResponse.Event event) {
    // strDescriptionEN이 있으면 사용, 없으면 기본 정보로 구성
    if (event.getStrDescriptionEN() != null && !event.getStrDescriptionEN().isEmpty()) {
      return event.getStrDescriptionEN();
    }

    // 기본 정보: 리그명 + 날짜
    StringBuilder sb = new StringBuilder();
    if (event.getStrLeague() != null) {
      sb.append(event.getStrLeague());
    }
    if (event.getDateEvent() != null) {
      if (!sb.isEmpty()) sb.append(" - ");
      sb.append(event.getDateEvent());
    }

    return !sb.isEmpty() ? sb.toString() : "경기 정보";
  }

  @Named("buildTags")
  default List<String> buildTags(SportsDbEventResponse.Event event) {
    List<String> tags = new ArrayList<>();

    // 스포츠 종목
    if (event.getStrSport() != null) {
      tags.add(event.getStrSport());
    }

    // 리그명
    if (event.getStrLeague() != null) {
      tags.add(event.getStrLeague());
    }

    // 국가
    if (event.getStrCountry() != null) {
      tags.add(event.getStrCountry());
    }

    //경기장
    if(event.getStrVenue() != null){
      tags.add(event.getStrVenue());
    }

    return tags;
  }

  @Named("buildThumbnailUrl")
  default String buildThumbnailUrl(String strThumb) {
    if (strThumb == null || strThumb.isEmpty()) {
      return "https://buly.kr/BIVulPE";
    }
    return strThumb;
  }
}