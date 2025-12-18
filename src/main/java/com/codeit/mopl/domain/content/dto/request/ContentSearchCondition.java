package com.codeit.mopl.domain.content.dto.request;

import com.codeit.mopl.domain.content.entity.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContentSearchCondition {
  private String typeEqual;
  private String keywordLike;
  private List<String> tagsIn;
  private String cursor;
  private UUID idAfter;
  private int limit;
  private SortDirection sortDirection;
  private SortBy sortBy;
}
