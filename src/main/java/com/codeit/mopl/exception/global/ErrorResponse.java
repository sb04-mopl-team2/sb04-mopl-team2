package com.codeit.mopl.exception.global;

import java.time.LocalDateTime;
import java.util.Map;


public record ErrorResponse(
    String exceptionName, //발생한 예외 이름
    String message, //오류 메시지
    Map<String, Object> details,
    LocalDateTime timestamp
) {

}
