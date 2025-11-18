package com.codeit.mopl.exception.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mopl.exception.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("MoplException을 처리하고 적절한 응답을 반환한다")
    void handleMoplException() {
        // given
        ErrorCode errorCode = com.codeit.mopl.exception.user.ErrorCode.USER_NOT_FOUND;
        UserNotFoundException exception = new UserNotFoundException(errorCode);

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMoplException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo(errorCode.getCode());
    }

    @Test
    @DisplayName("RuntimeException을 처리하고 500 응답을 반환한다")
    void handleRuntimeException() {
        // given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("IllegalArgumentException을 처리하고 400 응답을 반환한다")
    void handleIllegalArgumentException() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // when
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Invalid argument");
    }
}