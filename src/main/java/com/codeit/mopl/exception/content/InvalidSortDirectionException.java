package com.codeit.mopl.exception.content;

import java.util.Map;

public class InvalidSortDirectionException extends ContentException {

  public InvalidSortDirectionException(Map<String, Object> details) {
    super(ContentErrorCode.INVALID_SORT_DIRECTION, details);
  }
}