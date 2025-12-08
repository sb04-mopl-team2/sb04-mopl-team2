package com.codeit.mopl.search;

import com.codeit.mopl.search.document.ContentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentESRepository extends ElasticsearchRepository<ContentDocument, String> {

}
