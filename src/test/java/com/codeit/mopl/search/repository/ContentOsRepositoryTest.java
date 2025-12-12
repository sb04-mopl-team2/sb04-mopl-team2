package com.codeit.mopl.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.exception.content.ContentOsStorageException;
import com.codeit.mopl.search.document.ContentDocument;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ContentOsRepositoryTest {

  @Mock
  private OpenSearchClient client;

  @InjectMocks
  private ContentOsRepository contentOsRepository;

  private Content content;
  private ContentDocument doc;

  @BeforeEach
  public void init() {
    content = new Content();
    Instant fixedTime = Instant.now();
    UUID contentId = UUID.randomUUID();
    ReflectionTestUtils.setField(content, "createdAt", fixedTime);
    ReflectionTestUtils.setField(content, "id", contentId);

    doc = new ContentDocument();
    doc.setId(contentId.toString());
    doc.setCreatedAt(Instant.from(fixedTime));
  }

  @Test
  @DisplayName("콘텐츠 저장 성공")
  public void saveContentSuccess() throws IOException {
    // given
    IndexResponse response = mock(IndexResponse.class);
    given(client.index(any(Function.class))).willReturn(response);

    // when
    contentOsRepository.save(doc);

    // then
    verify(client).index(any(Function.class));
  }

  @Test
  @DisplayName("네트워크 오류로 인한 콘텐츠 저장 실패")
  public void saveContentFailure() throws IOException {
    // given
    given(client.index(any(Function.class)))
        .willThrow(IOException.class);

    // when & then
    assertThatThrownBy(() -> contentOsRepository.save(doc))
        .isInstanceOf(ContentOsStorageException.class);
  }

  @Test
  @DisplayName("콘텐츠 아이디로 조회 성공")
  public void findByIdSuccess() throws IOException {
    // given
    GetResponse<ContentDocument> getResponse = mock(GetResponse.class);
    given(getResponse.source()).willReturn(doc);
    given(client.get(any(Function.class), eq(ContentDocument.class))).willReturn(getResponse);

    // when
    ContentDocument res = contentOsRepository.findById(doc.getId());

    // then
    verify(client).get(any(Function.class), eq(ContentDocument.class));
    assertThat(res).isNotNull();
    assertThat(res.getId()).isEqualTo(doc.getId());
  }

  @Test
  @DisplayName("네트워크 오류로 인한 콘텐츠 아이디로 조회 실패")
  public void findByIdFailure() throws IOException {
    // given
    given(client.get(any(Function.class), eq(ContentDocument.class)))
        .willThrow(IOException.class);

    // when & then
    assertThatThrownBy(() -> contentOsRepository.findById(doc.getId()))
        .isInstanceOf(ContentOsStorageException.class);
  }

  @Test
  @DisplayName("콘텐츠 삭제 성공")
  public void deleteByIdSuccess() throws IOException {
    // given
    DeleteResponse response = mock(DeleteResponse.class);
    given(client.delete(any(Function.class))).willReturn(response);

    // when
    contentOsRepository.delete(doc.getId());

    // then
    verify(client).delete(any(Function.class));
  }

  @Test
  @DisplayName("네트워크 오류로 인한 콘텐츠 아이디로 삭제 실패")
  public void deleteByIdFailure() throws IOException {
    // given
    given(client.delete(any(Function.class)))
        .willThrow(IOException.class);

    // when & then
    assertThatThrownBy(() -> contentOsRepository.delete(doc.getId()))
        .isInstanceOf(ContentOsStorageException.class);
  }

  @Test
  @DisplayName("콘텐츠 count 성공")
  public void countTotalContentDocumentsSuccess() throws IOException {
    // given
    CountResponse countResponse = mock(CountResponse.class);
    given(countResponse.count()).willReturn(1L);
    given(client.count(any(Function.class))).willReturn(countResponse);

    // when
    Long count = contentOsRepository.count();

    // then
    verify(client).count(any(Function.class));
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @DisplayName("네트워크 오류로 인한 콘텐츠 count 실패")
  public void countTotalContentDocumentsFailure() throws IOException {
    // given
    given(client.count(any(Function.class)))
        .willThrow(IOException.class);

    // when & then
    assertThatThrownBy(() -> contentOsRepository.count())
        .isInstanceOf(ContentOsStorageException.class);
  }

  @Test
  @DisplayName("콘텐츠 단체 저장 성공")
  public void saveAllDocumentsSuccess() throws IOException {
    // given
    ContentDocument doc1 = doc;
    ContentDocument doc2 = new ContentDocument();
    doc2.setId(UUID.randomUUID().toString());
    List<ContentDocument> docs = List.of(doc1, doc2);
    BulkResponse bulkResponse = mock(BulkResponse.class);
    given(client.bulk(any(Function.class))).willReturn(bulkResponse);
    given(bulkResponse.errors()).willReturn(false);

    // when
    contentOsRepository.saveAll(docs);

    // then
    verify(client).bulk(any(Function.class));
    verify(bulkResponse).errors();
  }

  @Test
  @DisplayName("저장 오류로 인한 콘텐츠 단체 저장 실패")
  public void saveAllDocumentsIndexFailure() throws IOException {
    // given
    ContentDocument doc1 = doc;
    ContentDocument doc2 = new ContentDocument();
    doc2.setId(UUID.randomUUID().toString());
    List<ContentDocument> docs = List.of(doc1, doc2);
    BulkResponse bulkResponse = mock(BulkResponse.class);
    given(client.bulk(any(Function.class))).willReturn(bulkResponse);
    given(bulkResponse.errors()).willReturn(true);

    // when & then
    assertThatThrownBy(() -> contentOsRepository.saveAll(docs))
        .isInstanceOf(ContentOsStorageException.class);
  }

  @Test
  @DisplayName("네트워크 오류로 인한 콘텐츠 단체 저장 실패")
  public void saveAllDocumentsFailure() throws IOException {
    // given
    ContentDocument doc1 = doc;
    ContentDocument doc2 = new ContentDocument();
    doc2.setId(UUID.randomUUID().toString());
    List<ContentDocument> docs = List.of(doc1, doc2);
    given(client.bulk(any(Function.class)))
        .willThrow(IOException.class);

    // when & then
    assertThatThrownBy(() -> contentOsRepository.saveAll(docs))
        .isInstanceOf(ContentOsStorageException.class);
  }
}
