package com.codeit.mopl.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.codeit.mopl.domain.content.dto.request.ContentSearchRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.watchingsession.service.WatchingSessionService;
import com.codeit.mopl.exception.content.ContentOsStorageException;
import com.codeit.mopl.search.converter.ContentDocumentMapper;
import com.codeit.mopl.search.document.ContentDocument;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class OpenSearchServiceTest {

  @Mock
  private OpenSearchClient client;

  @Mock
  private ContentDocumentMapper contentDocumentMapper;

  @Mock
  private ContentOsRepository osRepository;

  @Mock
  private WatchingSessionService watchingSessionService;
  
  @InjectMocks
  private OpenSearchService openSearchService;

  private UUID contentId;
  private Content content;
  private ContentDto dto;
  private ContentDocument doc;
  private Instant fixedTime;

  @BeforeEach
  public void init() {
    // given
    fixedTime = Instant.now();
    content = new Content();
    contentId = UUID.randomUUID();
    ReflectionTestUtils.setField(content, "createdAt", fixedTime);
    ReflectionTestUtils.setField(content, "id", contentId);

    dto = new ContentDto(
        contentId, null, null, null, null, null,
        null, null, null
    );
    doc = new ContentDocument();
    doc.setId(contentId.toString());
    doc.setCreatedAt(Instant.from(fixedTime));
  }

  @Test
  @DisplayName("콘텐츠 저장 성공")
  public void saveContentDocumentSuccess() {
    // given
    given(contentDocumentMapper.toDocument(content)).willReturn(doc);
    
    // when
    openSearchService.save(content);

    // then
    verify(contentDocumentMapper).toDocument(content);
    verify(osRepository).save(any(ContentDocument.class));
  }

  @Test
  @DisplayName("OpenSearch 문제로 콘텐츠 저장 실패")
  public void saveContentDocumentFailure() {
    // given
    given(contentDocumentMapper.toDocument(content)).willReturn(doc);
    willThrow(ContentOsStorageException.class)
        .given(osRepository).save(any());

    // when & then
    assertThrows(ContentOsStorageException.class, () -> {
      openSearchService.save(content);
    });
    verify(contentDocumentMapper).toDocument(content);
  }

  @Test
  @DisplayName("콘텐츠 삭제 성공")
  public void deleteContentDocumentSuccess() {
    // when
    openSearchService.delete(contentId.toString());

    // then
    verify(osRepository).delete(contentId.toString());
  }

  @Test
  @DisplayName("OpenSearch 문제로 콘텐츠 삭제 실패")
  public void deleteContentDocumentFailure() {
    // given
    willThrow(ContentOsStorageException.class)
        .given(osRepository).delete(any());

    // when & then
    assertThrows(ContentOsStorageException.class, () -> {
      openSearchService.delete(contentId.toString());
    });
  }

  @Test
  @DisplayName("커서를 이용한 조회 성공")
  public void searchContentPaginationSuccess() throws IOException {
    // given
    ContentSearchRequest request = new ContentSearchRequest();
    request.setLimit(2);
    request.setSortBy("rate");
    request.setSortDirection("DESCENDING");

    ContentDocument doc1 = doc;
    ContentDocument doc2 = new ContentDocument();
    ContentDocument doc3 = new ContentDocument();

    UUID uuid1 = UUID.randomUUID();
    Hit<ContentDocument> hit1 = new Hit.Builder<ContentDocument>()
        .id(uuid1.toString())
        .index("content")
        .source(doc1)
        .sort("4.5", uuid1.toString())
        .build();
    UUID uuid2 = UUID.randomUUID();
    Hit<ContentDocument> hit2 = new Hit.Builder<ContentDocument>()
        .id(uuid2.toString())
        .index("content")
        .source(doc2)
        .sort("4.0", uuid2.toString())
        .build();
    UUID uuid3 = UUID.randomUUID();
    Hit<ContentDocument> hit3 = new Hit.Builder<ContentDocument>()
        .id(uuid3.toString())
        .index("content")
        .source(doc3)
        .sort("3.9", uuid3.toString())
        .build();
    SearchResponse<ContentDocument> mockResponse = new SearchResponse.Builder<ContentDocument>()
        .took(10)
        .timedOut(false)
        .shards(s -> s.successful(1).failed(0).total(1))
        .hits(h -> h
            .total(t -> t.value(3).relation(TotalHitsRelation.Eq))
            .hits(List.of(hit1, hit2, hit3))
            // hit 3개 - (limit + 1)
        )
        .build();
    given(client.search(any(SearchRequest.class), eq(ContentDocument.class)))
        .willReturn(mockResponse);

    // when
    CursorResponseContentDto response = openSearchService.search(request);

    // then
    assertThat(response.data()).hasSize(2);
    assertThat(response.nextCursor()).isEqualTo("4.0");
    assertThat(response.nextIdAfter()).isEqualTo(uuid2);
  }
}
