package com.codeit.mopl.search;

import com.codeit.mopl.search.document.ContentDocument;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.CountResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentOsRepository {

  private final OpenSearchClient client;

  public void save(ContentDocument doc) throws IOException {
    client.index(i -> i
        .index("content")
        .id(doc.getId())
        .document(doc)
    );
  }

  public ContentDocument findById(String id) throws IOException {
    return client.get(g -> g
            .index("content")
            .id(id),
        ContentDocument.class
    ).source();
  }

  public void delete(String id) throws IOException {
    client.delete(d -> d
        .index("content")
        .id(id)
    );
  }

  public long count() throws IOException {
    CountResponse response = client.count(c -> c
        .index("content")
    );
    return response.count();
  }

  public void saveAll(List<ContentDocument> docs) throws IOException {
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
  }

}
