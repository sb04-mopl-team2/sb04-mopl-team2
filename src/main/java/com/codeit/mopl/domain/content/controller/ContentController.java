package com.codeit.mopl.domain.content.controller;

import com.codeit.mopl.domain.content.dto.ContentDto;
import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {
  private final ContentService contentService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> createContent(
      @Valid @RequestPart("request") ContentCreateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
  ) {
    ContentDto content = contentService.createContent(request, thumbnail);
    return ResponseEntity.ok(content);
  }
}
