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
import com.codeit.mopl.search.ContentESRepository;
import com.codeit.mopl.search.ElasticsearchProxy;
import com.codeit.mopl.search.converter.ContentConverter;
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

  // ES 관련
  private final ElasticsearchProxy proxy;
  private final ContentESRepository contentESRepository;
  private final ContentConverter converter;


  @Transactional
  public ContentDto createContent(@Valid ContentCreateRequest request, MultipartFile thumbnail) {
    log.info("[콘텐츠 생성 시작] title={}", request.title());
    log.debug("[콘텐츠 생성 상세] request={}, hasThumbnail={}", request, thumbnail != null);

    Content content = contentMapper.fromCreateRequest(request);

    String thumbnailUrl = uploadThumbnail(thumbnail);
    content.setThumbnailUrl(thumbnailUrl);

    Content savedContent = contentRepository.save(content);
    ContentDto dto = contentMapper.toDto(savedContent);

    // ES에 저장
    contentESRepository.save(converter.convertToDocument(dto, savedContent.getCreatedAt()));

    log.info("[콘텐츠 생성 완료] id={}, title={}", dto.id(), dto.title());
    return dto;
  }

  @Transactional(readOnly = true)
  public CursorResponseContentDto findContents(ContentSearchRequest request) {
    log.info("[콘텐츠 목록 조회 시작] request={}", request);

//    CursorResponseContentDto response = contentRepository.findContents(request.toCondition());
    CursorResponseContentDto response = proxy.search(request);


    log.info("[콘텐츠 목록 조회 완료] resultCount={}",
        response.data() != null ? response.data().size() : 0);
    return response;
  }

  @Transactional(readOnly = true)
  public ContentDto findContent(UUID contentId) {
    log.info("[콘텐츠 단건 조회 시작] contentId={}", contentId);

    Content content = contentRepository.findById(contentId).orElseThrow(
        () -> {
          log.warn("[콘텐츠 조회 실패] 존재하지 않는 콘텐츠 contentId={}", contentId);
          return new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId));
        }
    );

     ContentDto dto = contentMapper.toDto(content);

    log.info("[콘텐츠 단건 조회 완료] id={}, title={}", dto.id(), dto.title());
    return dto;
  }

  @Transactional
  public ContentDto updateContent(UUID contentId, @Valid ContentUpdateRequest request,
      MultipartFile thumbnail) {
    log.info("[콘텐츠 수정 시작] contentId={}, title={}", contentId, request.title());
    log.debug("[콘텐츠 수정 상세] request={}, hasThumbnail={}", request, thumbnail != null);

    Content content = contentRepository.findById(contentId).orElseThrow(
        () -> {
          log.warn("[콘텐츠 수정 실패] 존재하지 않는 콘텐츠 contentId={}", contentId);
          return new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId));
        }
    );

    content.update(request);

    if (thumbnail != null && !thumbnail.isEmpty()) {
      String thumbnailUrl = uploadThumbnail(thumbnail);
      content.setThumbnailUrl(thumbnailUrl);
      log.debug("[콘텐츠 썸네일 업데이트] contentId={}, thumbnailUrl={}", contentId, thumbnailUrl);
    }
    ContentDto dto = contentMapper.toDto(content);

    // ES에 저장
    contentESRepository.save(converter.convertToDocument(dto, content.getCreatedAt()));

    log.info("[콘텐츠 수정 완료] id={}, title={}", dto.id(), dto.title());
    return dto;
  }

  @Transactional
  public void deleteContent(UUID contentId) {
    log.info("[콘텐츠 삭제 시작] contentId={}", contentId);

    Content content = contentRepository.findById(contentId).orElseThrow(
        () -> {
          log.warn("[콘텐츠 삭제 실패] 존재하지 않는 콘텐츠 contentId={}", contentId);
          return new ContentNotFoundException(ContentErrorCode.CONTENT_NOT_FOUND,
              Map.of("contentId", contentId));
        }
    );

    contentRepository.delete(content);

    // ES에 저장
    contentESRepository.deleteById(String.valueOf(contentId));

    log.info("[콘텐츠 삭제 완료] contentId={}", contentId);
  }

  // Redis로 실시간 세션 관리 메서드 기능 완성 후 추가 구현 예정
  private Long getWatcherCount() {
    return 0L;
  }

  // S3 파일 업로드 이미지 생성 메서드 기능 완성 후 리팩토링 예정
  private String uploadThumbnail(MultipartFile thumbnail) {
    if (thumbnail == null || thumbnail.isEmpty()) {
      log.debug("[썸네일 업로드 스킵] thumbnail이 없음");
      return null;
    }
    String imageUrl = "thumbnailUrl";
    log.debug("[썸네일 업로드 완료] imageUrl={}", imageUrl);
    return imageUrl;
  }
}
