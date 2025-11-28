package com.codeit.mopl.batch.tmdb.controller;

import com.codeit.mopl.batch.tmdb.service.MovieService;
import com.codeit.mopl.batch.tmdb.service.TmdbApiService;
import com.codeit.mopl.domain.content.entity.Content;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/batch")
public class BatchController {

  private final TmdbApiService tmdbApiService;
  private final MovieService movieService;

  @PostMapping("/init")
  public ResponseEntity<String> initData() {
    movieService.runInitialDataLoad();
    return ResponseEntity.ok("초기 데이터 수집 Job 실행 완료");
  }


  @GetMapping("/movies")
  public Mono<List<Content>> getMovies(
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(value = "page", defaultValue = "1") @Min(1) int page
  ) {
    return tmdbApiService.discoverMoviesFromDate(from, page);
  }
}

