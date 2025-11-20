package com.codeit.mopl.exception.playlist;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaylistErrorCode implements ErrorCodeInterface {
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이리스트가 존재하지 않습니다."),
    PLAYLIST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "플레이리스트 변경 권한이 없는 유저입니다.");

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
