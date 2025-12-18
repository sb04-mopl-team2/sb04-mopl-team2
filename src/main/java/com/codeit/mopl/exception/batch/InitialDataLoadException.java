package com.codeit.mopl.exception.batch;

import java.util.Map;

public class InitialDataLoadException extends BatchException {

  public InitialDataLoadException(BatchErrorCode batchErrorCode, Map<String, Object> details) {
    super(batchErrorCode, details);
  }
}
