package com.codeit.mopl.batch.sportsdb.service;

import com.codeit.mopl.batch.sportsdb.dto.SportsDbEventResponse;
import com.codeit.mopl.batch.sportsdb.mapper.SportsEventMapper;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.search.converter.ContentDocumentMapper;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsDbApiService {

  @Qualifier("sportsDbWebClient")
  private final WebClient sportsDbWebClient;

  private final SportsEventMapper sportsEventMapper;
  private final ContentRepository contentRepository;

  // OpenSearch
  private final ContentOsRepository osRepository;
  private final ContentDocumentMapper contentDocumentMapper;

  @Value("${sportsdb.api.key}")
  private String API_KEY;

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * 특정 날짜의 축구 경기 정보를 조회하고 저장
   *
   * @param date 조회 날짜
   * @return 저장된 Content 리스트
   */
  public Mono<List<Content>> fetchSoccerEventsByDate(LocalDate date) {
    String formattedDate = date.format(DATE_FORMATTER);
    log.info("[SportsDB] 축구 경기 조회 시작 date = {}", formattedDate);

    Mono<SportsDbEventResponse> responseMono = sportsDbWebClient
        .get()
        .uri(uriBuilder -> buildEventsUri(uriBuilder, formattedDate))
        .retrieve()
        .bodyToMono(SportsDbEventResponse.class);

    return responseMono
        .flatMapMany(response -> {
          if (response.getEvents() == null) {
            log.warn("[SportsDB] 날짜 {}의 경기 데이터가 없습니다.", formattedDate);
            return Flux.empty();
          }
          return Flux.fromIterable(response.getEvents());
        })
        .map(sportsEventMapper::toContent)
        .publishOn(Schedulers.boundedElastic())
        .map(c -> {
          osRepository.save(contentDocumentMapper.toDocument(c));
          return contentRepository.save(c);
        })
        .collectList()
        .doOnSuccess(list ->
            log.info("[SportsDB] 축구 경기 조회 완료 저장된 컨텐츠 수 = {}", list.size())
        )
        .doOnError(error ->
            log.error("[SportsDB] 축구 경기 조회 실패 date = {}", formattedDate, error)
        );
  }

  /**
   * API 호출을 위한 URI 빌드
   *
   * @param builder URI 빌더
   * @param date 조회 날짜 (yyyy-MM-dd)
   * @return 빌드된 URI
   */
  private URI buildEventsUri(UriBuilder builder, String date) {
    return builder
        .path("{apiKey}/eventsday.php")
        .queryParam("d", date)
        .queryParam("s", "Soccer")
        .build(API_KEY);
  }
}