package com.codeit.mopl.domain.content.service;

import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.dto.request.ContentSearchRequest;
import com.codeit.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.exception.content.ContentErrorCode;
import com.codeit.mopl.exception.content.ContentNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

  private final ContentRepository contentRepository;

  private final ContentMapper contentMapper;

  //콘텐츠 수동등록
  @Transactional
  public ContentDto createContent(@Valid ContentCreateRequest request, MultipartFile thumbnail) {
    log.info("[콘텐츠] 콘텐츠 생성 서비스 시작 title = {}", request.title());

    Content content = contentMapper.fromCreateRequest(request);

    String thumbnailUrl = uploadThumbnail(thumbnail);
    content.setThumbnailUrl(thumbnailUrl);

    Content saveContent = contentRepository.save(content);

    Long watcherCount = getWatcherCount();

    ContentDto dto = contentMapper.toDto(saveContent, watcherCount);
    log.info("[콘텐츠] 콘텐츠 서비스 완료 id = {}, title = {}", dto.id(), dto.title());
    return dto;
  }

  //콘텐츠 목록조회
  @Transactional(readOnly = true)
  public CursorResponseContentDto findContents(ContentSearchRequest request) {
    return contentRepository.findContents(request.toCondition());
  }

  //콘텐츠 단건조회
  @Transactional(readOnly = true)
  public ContentDto findContent(UUID contentId) {
    Content content = contentRepository.findById(contentId).orElseThrow(
        () ->  new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId))
    );

    Long watcherCount = getWatcherCount();
    return contentMapper.toDto(content, watcherCount);
  }

  //콘텐츠 수정
  @Transactional
  public ContentDto updateContent(UUID contentId, @Valid ContentUpdateRequest request, MultipartFile thumbnail) {
    log.info("[콘텐츠] 콘텐츠 수정 서비스 시작 contentId = {}, title = {}", contentId, request.title());

    Content content = contentRepository.findById(contentId).orElseThrow(
        () -> new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId))
    );

    content.update(request);

    if (thumbnail != null && !thumbnail.isEmpty()) {
      String thumbnailUrl = uploadThumbnail(thumbnail);
      content.setThumbnailUrl(thumbnailUrl);
    }

    Long watcherCount = getWatcherCount();

    ContentDto dto = contentMapper.toDto(content, watcherCount);
    log.info("[콘텐츠] 콘텐츠 수정 서비스 완료 id = {}, title = {}", dto.id(), dto.title());
    return dto;
  }

  //redis로 실시간 세션 관리 매서드 기능 완성후 추가 구현예정
  private Long getWatcherCount() {
    return 0L;
  }

  // S3 파일 업로드 이미지 생성메서드 기능 완성 후 리팩토링 예정
  private String uploadThumbnail(MultipartFile thumbnail) {
    if (thumbnail == null || thumbnail.isEmpty()) {
      return null;
    }

    String imageUrl = "thumbnailUrl";
    return imageUrl;
  }
}
