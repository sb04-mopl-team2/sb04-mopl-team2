package com.codeit.mopl.domain.review.service;

import com.codeit.mopl.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository reviewRepository;
}
