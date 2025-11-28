package com.codeit.mopl.domain.content.controller;

import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.dto.request.ContentSearchRequest;
import com.codeit.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.service.ContentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {

  private final ContentService contentService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> createContent(
      @Valid @RequestPart("request") ContentCreateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
  ) {
    log.info("[콘텐츠 생성 요청] title={}, hasThumbnail={}",
        request.title(), thumbnail != null);

    ContentDto content = contentService.createContent(request, thumbnail);

    log.info("[콘텐츠 생성 응답] id={}, title={}", content.id(), content.title());
    return ResponseEntity.status(HttpStatus.CREATED).body(content);
  }

  @GetMapping
  public CursorResponseContentDto findContents(@Valid @ModelAttribute ContentSearchRequest request) {
    log.info("[콘텐츠 목록 조회 요청] request={}", request);

    CursorResponseContentDto response = contentService.findContents(request);

    log.info("[콘텐츠 목록 조회 응답] resultCount={}",
        response.data() != null ? response.data().size() : 0);
    return response;
  }

  @GetMapping("/{contentId}")
  public ResponseEntity<ContentDto> findContent(@PathVariable UUID contentId) {
    log.info("[콘텐츠 단건 조회 요청] contentId={}", contentId);

    ContentDto content = contentService.findContent(contentId);

    log.info("[콘텐츠 단건 조회 응답] contentId={}, title={}",
        content.id(), content.title());
    return ResponseEntity.ok(content);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping(value = "/{contentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> updateContent(
      @PathVariable UUID contentId,
      @Valid @RequestPart("request") ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
  ) {
    log.info("[콘텐츠 수정 요청] contentId={}, title={}, hasThumbnail={}",
        contentId, request.title(), thumbnail != null);

    ContentDto content = contentService.updateContent(contentId, request, thumbnail);

    log.info("[콘텐츠 수정 응답] id={}, title={}", content.id(), content.title());
    return ResponseEntity.ok(content);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{contentId}")
  public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId) {
    log.info("[콘텐츠 삭제 요청] contentId={}", contentId);

    contentService.deleteContent(contentId);

    log.info("[콘텐츠 삭제 응답] contentId={}", contentId);
    return ResponseEntity.noContent().build();
  }
}
