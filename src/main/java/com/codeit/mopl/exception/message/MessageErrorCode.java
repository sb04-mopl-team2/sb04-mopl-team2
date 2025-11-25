package com.codeit.mopl.exception.message;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MessageErrorCode implements ErrorCodeInterface {
    CONVERSATION_ALREADY_EXIST(HttpStatus.CONFLICT,"동일한 채팅방이 이미 존재합니다."),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND,"해당 채팅방이 존재하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}
