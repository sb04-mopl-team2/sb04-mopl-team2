package com.codeit.mopl.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.codeit.mopl.domain.content.dto.request.ContentSearchRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.entity.SortDirection;
import com.codeit.mopl.search.converter.ContentConverter;
import com.codeit.mopl.search.document.ContentDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchProxy {

  private final ContentESRepository contentRepository;
  private final ElasticsearchOperations operations;
  private final ContentConverter converter;
  private final ElasticsearchClient client;


//  request=ContentSearchRequest {
//    typeEqual     = movie
//    keywordLike   = 예시
//    tagsIn        = null
//    cursor        = null
//    idAfter       = null
//    limit         = 20
//    sort          = rate (DESCENDING)
//  }

  public CursorResponseContentDto search(ContentSearchRequest request) {
    log.info("[콘텐츠 목록 조회 시작 -  ES] request={}", request);

    Query boolQuery = boolQueryBuilder(request);
    Sort sort = buildSort(request.getSortBy(), request.getSortDirection());
    NativeQueryBuilder nativeQuery = NativeQuery.builder()
        .withQuery(boolQuery)
        .withSort(sort)
        .withMaxResults(request.getLimit() + 1) // 커서를 위해 하나 더 가져오기
        ;

    if (request.getSortBy() != null && request.getIdAfter() != null) {
      Object sortValue = parseCursor(request.getCursor(), request.getSortBy());
      // 커서 존재하면 쿼리에 적용
      nativeQuery.withSearchAfter(List.of(
          sortValue, // 값
          request.getIdAfter().toString() // UUID
        )
      );
    }

    NativeQuery query = nativeQuery.build();


      SearchHits<ContentDocument> searchHits = operations.search(query, ContentDocument.class);
      List<SearchHit<ContentDocument>> hits = searchHits.getSearchHits();
      boolean hasNext = hits.size() > request.getLimit();

      List<ContentDto> data = searchHits.stream()
          .map(d -> converter.convertToDto(d.getContent()))
          .limit(request.getLimit())
          .toList();

      String nextCursor = null;
      UUID nextIdAfter = null;
      if (hasNext) {
        SearchHit<ContentDocument> lastHit = hits.get(request.getLimit() - 1);
        List<Object> sortValues = lastHit.getSortValues();
        nextCursor = String.valueOf(sortValues.get(0));
        nextIdAfter = UUID.fromString(String.valueOf(sortValues.get(1)));
      }

      CursorResponseContentDto response = new CursorResponseContentDto(
          data,
          nextCursor,
          nextIdAfter,
          hasNext,
          searchHits.getTotalHits(),
          request.getSortBy(),
          request.getSortDirection()
      );
      log.info("[콘텐츠 목록 조회 완료 - ES] resultCount={}", data.size());
      return response;

  }

  // 예: cursor = 9.2, sortBy = "rating"
  private Object parseCursor(String cursor, String sortBy) {
    return switch(sortBy) {
      case "rate" -> Double.valueOf(cursor);
      case "watcherCount" -> Integer.valueOf(cursor);
      // createdAt
      default -> cursor;
    };
  }

  private Query boolQueryBuilder(ContentSearchRequest request) {
    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
    List<Query> mustQueries = new ArrayList<>();

    // typeEqual
    if (request.getTypeEqual() != null) {
        String value = ContentType.fromType(request.getTypeEqual()).name();
        boolQueryBuilder.filter(f -> f.term(t -> t
            .field("contentType")
            .value(value)
        ));
    }

    // 키워드
    if (request.getKeywordLike() != null && !request.getKeywordLike().isBlank()) {
      mustQueries.add(new MultiMatchQuery.Builder()
          .fields("title")
          .query(request.getKeywordLike())
          .type(TextQueryType.BoolPrefix)
          .build()
          ._toQuery()
      );
    }

    boolQueryBuilder.must(mustQueries);
    return boolQueryBuilder.build()._toQuery();
  }

  private Sort buildSort(String sortBy, String direction) {
    Direction dir = (direction.equals(SortDirection.ASCENDING.toString())
        ? Direction.ASC
        : Direction.DESC
    );
    String fieldName = switch (sortBy) {
      case "rate" -> "averageRating";
      case "watcherCount" -> "watcherCount";
      // createdAt를 디폴트값으로
      default -> "createdAt";
    };

    return Sort.by(
        new Order(dir, fieldName),
        new Order(Direction.ASC, "id") // 타이 브레이커
    );
  }
}
