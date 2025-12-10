package com.codeit.mopl.search;

import com.codeit.mopl.exception.content.ContentErrorCode;
import com.codeit.mopl.exception.content.ContentOsStorageException;
import com.codeit.mopl.search.document.ContentDocument;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.CountResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentOsRepository {

  private final OpenSearchClient client;

  public void save(ContentDocument doc) {
    try {
      client.index(i -> i
          .index("content")
          .id(doc.getId())
          .document(doc)
      );
    } catch (IOException e) {
      throw new ContentOsStorageException(
          ContentErrorCode.SEARCH_ENGINE_ERROR,
          Map.of("contentId", doc.getId())
      );
    }
  }

  public ContentDocument findById(String id) {
    try {
      return client.get(g -> g
              .index("content")
              .id(id),
          ContentDocument.class
      ).source();
    } catch (IOException e) {
      throw new ContentOsStorageException(
          ContentErrorCode.SEARCH_ENGINE_ERROR,
          Map.of("contentId", id)
      );

    }
  }

  public void delete(String id) {
    try {
      client.delete(d -> d
          .index("content")
          .id(id)
      );
    } catch (IOException e) {
      throw new ContentOsStorageException(
          ContentErrorCode.SEARCH_ENGINE_ERROR,
          Map.of("contentId", id)
      );

    }
  }

  public long count() {
    CountResponse response = null;
    try {
      response = client.count(c -> c
          .index("content")
      );
    } catch (IOException e) {
      throw new ContentOsStorageException(
          ContentErrorCode.SEARCH_ENGINE_ERROR,
          Map.of()
      );
    }
    return response.count();
  }

  public void saveAll(List<ContentDocument> docs) {
    try {
      client.bulk(b -> {
        b.index("content");
        for (ContentDocument doc : docs) {
          b.operations(op -> op
              .index(idx -> idx
                  .id(doc.getId())
                  .document(doc)
              )
          );
        }
        return b;
      });
    } catch (IOException e) {
      throw new ContentOsStorageException(
          ContentErrorCode.SEARCH_ENGINE_ERROR,
          Map.of()
      );
    }
  }

}
