package com.codeit.mopl.batch.tmdb.base.service;

import com.codeit.mopl.batch.tmdb.base.dto.TmdbDiscoverResponse;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.search.converter.ContentDocumentMapper;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * TMDB API 호출의 공통 로직을 담은 추상 서비스 Movie와 TV 서비스가 이를 상속받아 구현
 *
 * @param <T> API 응답 아이템 타입 (Movie 또는 TV)
 * @param <R> API 응답 타입 (TmdbDiscoverResponse 구현체)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTmdbApiService<T, R extends TmdbDiscoverResponse<T>> {

  protected final WebClient tmdbWebClient;
  protected final ContentRepository contentRepository;

  // OpenSearch
  private final ContentOsRepository osRepository;
  private final ContentDocumentMapper contentDocumentMapper;

  /**
   * 특정 날짜의 컨텐츠를 조회하고 저장
   *
   * @param from 조회 날짜
   * @param page 페이지 번호
   * @return 저장된 Content 리스트
   */
  public Mono<List<Content>> discoverContentFromDate(LocalDate from, int page) {
    log.info("[TMDB] {} 조회 시작 from = {}, page = {}", getContentType(), from, page);

    Mono<R> responseMono = tmdbWebClient
        .get()
        .uri(uriBuilder -> buildDiscoverUri(uriBuilder, from, page))
        .retrieve()
        .bodyToMono(getResponseClass());

    return responseMono
        .flatMapMany(response ->
            Flux.fromIterable(response.getResults() != null ? response.getResults() : List.of())
        )
        .map(this::mapToContent)
        .publishOn(Schedulers.boundedElastic())
        .map(c -> {
          Content savedContent = contentRepository.save(c);
          osRepository.save(contentDocumentMapper.toDocument(savedContent));
          return savedContent;
        })
        .collectList()
        .doOnSuccess(list ->
            log.info("[TMDB] {} 조회 완료 저장된 컨텐츠 수 = {}", getContentType(), list.size())
        );
  }

  /**
   * 특정 날짜의 컨텐츠를 조회하고 저장 (응답 객체 반환) 첫 페이지 호출 시 totalPages 정보를 함께 반환받기 위해 사용
   *
   * @param from 조회 날짜
   * @param page 페이지 번호
   * @return API 응답 전체
   */
  public Mono<R> discoverContentFromDateWithResponse(LocalDate from, int page) {
    log.info("[TMDB] {} 조회(응답 포함) 시작 from = {}, page = {}", getContentType(), from, page);

    return tmdbWebClient
        .get()
        .uri(uriBuilder -> buildDiscoverUri(uriBuilder, from, page))
        .retrieve()
        .bodyToMono(getResponseClass())
        .flatMap(response -> {
          List<T> results = response.getResults() != null ? response.getResults() : List.of();
          return Flux.fromIterable(results)
              .map(this::mapToContent)
              .publishOn(Schedulers.boundedElastic())
              .map(c -> {
                osRepository.save(contentDocumentMapper.toDocument(c));
                return contentRepository.save(c);
              })
              .collectList()
              .doOnSuccess(saved -> log.info("[TMDB] {} 조회 및 저장 완료 조회 수 = {}",
                  getContentType(), saved.size()))
              .thenReturn(response);
        });
  }

  /**
   * API 호출을 위한 URI 빌드 하위 클래스에서 구체적인 엔드포인트와 파라미터 구현
   *
   * @param builder URI 빌더
   * @param from    조회 날짜
   * @param page    페이지 번호
   * @return 빌드된 URI
   */
  protected abstract URI buildDiscoverUri(UriBuilder builder, LocalDate from, int page);

  /**
   * API 응답 아이템을 Content 엔티티로 변환
   *
   * @param item API 응답 아이템 (Movie 또는 TV)
   * @return Content 엔티티
   */
  protected abstract Content mapToContent(T item);

  /**
   * API 응답 클래스 반환
   *
   * @return 응답 클래스 타입
   */
  protected abstract Class<R> getResponseClass();

  /**
   * 컨텐츠 타입 문자열 반환 (로깅용)
   *
   * @return "영화" 또는 "TV 프로그램"
   */
  protected abstract String getContentType();
}