package com.codeit.mopl.domain.review.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@EnableCaching
@ActiveProfiles("test")
class ReviewServiceCacheTest {

  @TestConfiguration
  static class TestCacheConfig {
    @Bean
    @Primary
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(
          "review:first-page"
      );
    }
  }

  @Autowired
  private ReviewService reviewService;

  @MockitoBean
  private NotificationRepository notificationRepository;

  @MockitoBean
  private ReviewRepository reviewRepository;

  @Test
  @DisplayName("같은 파라미터면 캐시가 적용되어 Repository는 한 번만 호출된다")
  void getReviews_whenSameParams_shouldUseCache() {
    // given
    UUID contentId = UUID.randomUUID();
    String cursor = null;
    UUID idAfter = null;
    int limit = 10;
    SortDirection sortDirection = SortDirection.DESCENDING;
    ReviewSortBy sortBy = ReviewSortBy.createdAt;

    when(reviewRepository.searchReview(
        eq(contentId),
        eq(cursor),
        eq(idAfter),
        eq(limit),
        eq(sortDirection),
        eq(sortBy)
    )).thenReturn(List.of());

    // when
    reviewService.findReviews(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    );reviewService.findReviews(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    verify(reviewRepository, times(1)).searchReview(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    );

  }
}
