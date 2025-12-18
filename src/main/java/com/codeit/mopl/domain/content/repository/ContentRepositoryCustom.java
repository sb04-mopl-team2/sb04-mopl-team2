package com.codeit.mopl.domain.content.repository;

import com.codeit.mopl.domain.content.dto.request.ContentSearchCondition;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;

public interface ContentRepositoryCustom {
  CursorResponseContentDto findContents(ContentSearchCondition condition);
}