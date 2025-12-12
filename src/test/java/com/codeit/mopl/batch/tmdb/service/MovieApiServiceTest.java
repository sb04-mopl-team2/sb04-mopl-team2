package com.codeit.mopl.batch.tmdb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;

import com.codeit.mopl.batch.tmdb.movie.dto.TmdbDiscoverMovieResponse;
import com.codeit.mopl.batch.tmdb.movie.mapper.TmdbMovieMapper;
import com.codeit.mopl.batch.tmdb.movie.service.MovieApiService;
import com.codeit.mopl.domain.content.ContentTestFactory;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.search.converter.ContentDocumentMapper;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.time.LocalDate;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MovieApiServiceTest {

  private MockWebServer mockWebServer;

  @Mock
  private TmdbMovieMapper movieMapper;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentOsRepository osRepository;

  @Mock
  private ContentDocumentMapper contentDocumentMapper;

  @InjectMocks
  private MovieApiService movieApiService;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WebClient webClient = WebClient.builder()
        .baseUrl(mockWebServer.url("/").toString())
        .build();

    movieApiService = new MovieApiService(webClient, contentRepository, movieMapper, osRepository, contentDocumentMapper);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  @DisplayName("콘텐츠 수집을 위한 api 호출 검증")
  void discoverContentFromDate() {
    // given
    String mockJson = """
            {
              "page": 1,
              "total_pages": 1,
              "total_results": 1,
              "results": [
                {
                  "adult": false,
                  "backdrop_path": "/sampleA.jpg",
                  "genre_ids": [12, 14],
                  "id": 101,
                  "original_language": "en",
                  "original_title": "Original A",
                  "overview": "Overview A",
                  "popularity": 123.4,
                  "poster_path": "/posterA.jpg",
                  "release_date": "2025-11-20",
                  "title": "Movie A",
                  "video": false,
                  "vote_average": 7.8,
                  "vote_count": 1000
                }
              ]
            }
            """;

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(mockJson)
        .addHeader("Content-Type", "application/json"));

    Content expectedContent = ContentTestFactory.createDefault(ContentType.MOVIE);

    given(movieMapper.toContent(any(TmdbDiscoverMovieResponse.Movie.class)))
        .willReturn(expectedContent);
    given(contentRepository.save(expectedContent)).willReturn(expectedContent);

    // when
    Mono<List<Content>> result = movieApiService.discoverContentFromDate(LocalDate.now(), 1);

    // then
    StepVerifier.create(result)
        .assertNext(list -> {
          assertThat(list).hasSize(1);
          Content content = list.get(0);
          assertThat(content.getTitle()).isEqualTo(expectedContent.getTitle());
          assertThat(content.getDescription()).isEqualTo(expectedContent.getDescription());
          assertThat(content.getContentType()).isEqualTo(expectedContent.getContentType());
        })
        .verifyComplete();
  }
}