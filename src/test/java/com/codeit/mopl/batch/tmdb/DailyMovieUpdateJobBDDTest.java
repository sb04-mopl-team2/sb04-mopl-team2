package com.codeit.mopl.batch.tmdb;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.codeit.mopl.batch.tmdb.config.MovieStepConfig;
import com.codeit.mopl.batch.tmdb.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.batch.tmdb.service.TmdbApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatStatus;
import reactor.core.publisher.Mono;

class MovieTaskletTest {

  private TmdbApiService tmdbApiService;
  private MovieStepConfig stepConfig;

  @BeforeEach
  void setUp() {
    // 외부 API를 모킹
    tmdbApiService = mock(TmdbApiService.class);
    stepConfig = new MovieStepConfig(tmdbApiService, null, null);
  }

  @Test
  @DisplayName("일일 영화 업데이트 Tasklet이 정상적으로 실행되는지 확인")
  void dailyMovieUpdateTasklet_shouldRunSuccessfully() throws Exception {
    // given
    given(tmdbApiService.discoverMoviesFromDateWithResponse(any(), anyInt()))
        .willReturn(Mono.just(new TmdbDiscoverMovieResponse()));

    // when
    RepeatStatus status = stepConfig.dailyMovieUpdateTasklet().execute(null, null);

    // then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
  }
}
