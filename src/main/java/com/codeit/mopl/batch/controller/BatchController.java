package com.codeit.mopl.batch.controller;

import com.codeit.mopl.batch.ContentInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/batch")
public class BatchController {

  private final ContentInitService contentInitService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/init")
  public ResponseEntity<String> initData() {
    contentInitService.runInitialDataLoad();
    return ResponseEntity.ok("초기 데이터 수집이 백그라운드에서 시작되었습니다.");
  }
}

