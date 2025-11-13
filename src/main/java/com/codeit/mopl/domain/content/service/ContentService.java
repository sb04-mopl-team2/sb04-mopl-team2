package com.codeit.mopl.domain.content.service;

import com.codeit.mopl.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentService {
  private final ContentRepository contentRepository;
}
