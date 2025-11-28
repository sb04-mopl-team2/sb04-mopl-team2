package com.codeit.mopl.domain.content.dto.request;

import com.codeit.mopl.domain.content.entity.SortBy;
import com.codeit.mopl.domain.content.entity.SortDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSearchRequest {

  private String typeEqual;
  private String keywordLike;
  private List<String> tagsIn;
  private String cursor;
  private UUID idAfter;

  @Min(1)
  private int limit;

  @NotNull
  private String sortDirection;

  @NotNull
  private String sortBy;

  public ContentSearchCondition toCondition() {
    return ContentSearchCondition.builder()
        .typeEqual(typeEqual)
        .keywordLike(keywordLike)
        .tagsIn(tagsIn)
        .cursor(cursor)
        .idAfter(idAfter)
        .limit(limit)
        .sortDirection(SortDirection.fromValue(sortDirection))
        .sortBy(SortBy.fromValue(sortBy))
        .build();
  }
}