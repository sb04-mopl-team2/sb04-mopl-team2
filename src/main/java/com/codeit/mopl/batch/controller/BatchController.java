package com.codeit.mopl.batch.controller;

import com.codeit.mopl.batch.tmdb.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/batch")
public class BatchController {

  private final MovieService movieService;

  @PostMapping("/init")
  public ResponseEntity<String> initData() {
    movieService.runInitialDataLoad();
    return ResponseEntity.ok("초기 데이터 수집 Job 실행 완료");
  }
}

