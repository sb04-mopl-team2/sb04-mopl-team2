package com.codeit.mopl.exception.content;

import com.codeit.mopl.exception.global.ErrorCodeInterface;
import org.springframework.http.HttpStatus;

public enum ContentErrorCode implements ErrorCodeInterface {
  CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),
  INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "유효한 콘텐츠 이미지 파일이 아닙니다."),
  INVALID_CONTENT_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 콘텐츠 ID입니다."),
  INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 콘텐츠 타입입니다."),
  INVALID_SORT_BY(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬 기준입니다."),
  INVALID_SORT_DIRECTION(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬 방향입니다."),

  // OpenSearch
  SEARCH_ENGINE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "검색 엔진 연동 중 오류가 발생했습니다."),
  SEARCH_ENGINE_INDEXING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠 인덱싱 중 일부 문서 저장에 실패했습니다."),
  CONTENT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "(OS) 콘텐츠를 찾을 수 없습니다.");


  private final HttpStatus status;
  private final String message;

  ContentErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  @Override
  public String getName() {
    return this.name();
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