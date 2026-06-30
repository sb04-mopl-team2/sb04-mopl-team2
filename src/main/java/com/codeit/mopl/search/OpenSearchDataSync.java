package com.codeit.mopl.search;


import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.search.converter.ContentDocumentMapper;
import com.codeit.mopl.search.document.ContentDocument;
import com.codeit.mopl.search.repository.ContentOsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
//@Profile("prod")
@Component
@RequiredArgsConstructor
public class OpenSearchDataSync implements ApplicationRunner {

  private final ContentRepository repository;
  private final ContentOsRepository esRepository;
  private final ContentMapper contentMapper;
  private final ContentDocumentMapper documentMapper;

  private static final int PAGE_SIZE = 500;

  @Transactional(readOnly = true)
  @Override
  public void run(ApplicationArguments args)  {
    log.info("[ElasticsearchDataSync] 콘텐츠 데이터 동기화 시작.");

    long count = repository.count();
    long esCount = esRepository.count();
    // es에 이미 데이터 존재하면 동기화 안함
    if (esCount > 0) {
      log.info("[ElasticsearchDataSync] Index에 document가 존재해서 init 스킵. documents = {}", esCount);
      return;
    }
    int totalPages = (int) Math.ceil((double) count / PAGE_SIZE);
    for (int i = 0; i < totalPages; i++) {
      Pageable pageable = PageRequest.of(i, PAGE_SIZE); // 페이지 + 아이템 수
      Page<Content> pages = repository.findAll(pageable);
      List<ContentDocument> contents = pages.getContent().stream()
          .map(documentMapper::toDocument)
          .toList();

      esRepository.saveAll(contents);
      log.info("[ElasticsearchDataSync] 배치 {}/{} (저장된 아이템 갯수 = {}) 동기화.",
          i + 1, totalPages, contents.size());
    }
    log.info("[ElasticsearchDataSync] 콘텐츠 데이터 동기화 완료. 총 저장된 갯수 = {}", esRepository.count());
  }
}
