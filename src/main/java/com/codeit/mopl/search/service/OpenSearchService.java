package com.codeit.mopl.search.service;

import com.codeit.mopl.domain.content.dto.request.ContentSearchRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.entity.SortDirection;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.search.converter.ContentConverter;
import com.codeit.mopl.search.document.ContentDocument;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSearchService {

  private final OpenSearchClient client;
  private final ContentMapper contentMapper;
  private final ContentConverter converter;
  private final ContentOsRepository osRepository;

  public void save(Content content) {
    ContentDto dto = contentMapper.toDto(content);
    osRepository.save(converter.convertToDocument(dto, content.getCreatedAt()));
  }

  public void delete(String contentId) {
    osRepository.delete(contentId);
  }

  public CursorResponseContentDto search(ContentSearchRequest request) {
    log.info("[콘텐츠 목록 조회 시작 -  OpenSearch] request={}", request);

    Query boolQuery = boolQueryBuilder(request);
    SearchRequest.Builder builder = new SearchRequest.Builder()
        .index("content")
        .query(boolQuery)
        .size(request.getLimit() + 1);
    addSort(builder, request);


    if (request.getSortBy() != null && request.getIdAfter() != null) {
      Object sortValue = parseCursor(request.getCursor(), request.getSortBy());
      List<String> searchAfterList= new ArrayList<>();
      searchAfterList.add(sortValue.toString());
      searchAfterList.add(request.getIdAfter().toString());
      // 커서 존재하면 쿼리에 적용
      builder.searchAfter(searchAfterList);
    }
    SearchResponse<ContentDocument> res = null;
    try {
      res = client.search(builder.build(), ContentDocument.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    List<Hit<ContentDocument>> hits = res.hits().hits();
    boolean hasNext = hits.size() > request.getLimit();

    List<ContentDto> data = hits.stream()
        .map(d -> converter.convertToDto(d.source()))
        .limit(request.getLimit())
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext) {
      Hit<ContentDocument> lastHit = hits.get(request.getLimit() - 1);
      List<String> sortValues = lastHit.sort();
      nextCursor = String.valueOf(sortValues.get(0));
      nextIdAfter = UUID.fromString(String.valueOf(sortValues.get(1)));
    }

    CursorResponseContentDto response = new CursorResponseContentDto(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        res.hits().total() == null ? 0 : res.hits().total().value(),
        request.getSortBy(),
        request.getSortDirection()
    );
    log.info("[콘텐츠 목록 조회 완료 - OpenSearch] resultCount={}", data.size());
    return response;
  }

  // ================================= private 헬퍼 메서드들 =================================
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
            .value(FieldValue.of(value))
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

  private void addSort(SearchRequest.Builder builder, ContentSearchRequest req) {
    String sortByString = switch (req.getSortBy()) {
      case "rate" -> "averageRating";
      case "watcherCount" -> "watcherCount";
      default -> "createdAt";
    };

    boolean asc = req.getSortDirection().equals(SortDirection.ASCENDING.toString());

    builder.sort(s -> s.field(f -> f.field(sortByString).order(asc ? SortOrder.Asc : SortOrder.Desc)));
    // 타이 브레이커
    builder.sort(s -> s.field(f -> f.field("id").order(SortOrder.Asc)));
  }
}
