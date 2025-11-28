package com.codeit.mopl.exception.content;

import java.util.Map;

public class InvalidSortByException extends ContentException {

  public InvalidSortByException(Map<String, Object> details) {
    super(ContentErrorCode.INVALID_SORT_BY, details);
  }
}