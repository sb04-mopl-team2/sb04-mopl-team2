package com.codeit.mopl.exception.global;

import org.springframework.http.HttpStatus;

public interface ErrorCodeInterface {
    String getName();
    String getMessage();
    HttpStatus getStatus();
}
