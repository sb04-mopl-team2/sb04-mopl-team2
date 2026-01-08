package com.codeit.mopl.exception.outbox;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OutBoxErrorCode implements ErrorCodeInterface {
    OUTBOX_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "OutBox 이벤트를 찾을 수 없습니다."),
    EVENT_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 직렬화에 실패했습니다."),
    EVENT_DESERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 역직렬화에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    OutBoxErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getName() {
        return this.name();
    }
}
