package com.codeit.mopl.batch.tmdb.controller;

import com.codeit.mopl.batch.tmdb.service.TmdbApiService;
import com.codeit.mopl.domain.content.entity.Content;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/tmdb")
public class TmdbTestController {

  private final TmdbApiService tmdbApiService;

  @GetMapping
  public Mono<List<Content>> discoverMovies(
      @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(value = "page", defaultValue = "1") int page
  ) {
    return tmdbApiService.discoverMoviesFromDate(from, page);
  }
}

