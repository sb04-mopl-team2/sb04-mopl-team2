package com.codeit.mopl.exception.batch;

import com.codeit.mopl.exception.global.MoplException;
import java.util.Map;

public class BatchException extends MoplException {
  public BatchException(BatchErrorCode batchErrorCode, Map<String, Object> details) {
    super(batchErrorCode, details);
  }
}