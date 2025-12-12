package com.codeit.mopl.domain.notification.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import java.util.Collections;
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
class NotificationServiceCacheTest {

  @TestConfiguration
  static class TestCacheConfig {
    @Bean
    @Primary
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(
          "notifications:first-page"
      );
    }
  }

  @Autowired
  private NotificationService notificationService;

  @MockitoBean
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("같은 파라미터면 캐시가 적용되어 Repository는 한 번만 호출된다")
  void getNotifications_whenSameParams_shouldUseCache() {
    // given
    UUID userId = UUID.randomUUID();
    String cursor = null;
    UUID idAfter = null;
    int limit = 5;
    SortDirection sortDirection = SortDirection.DESCENDING;
    SortBy sortBy = SortBy.CREATED_AT;

    when(notificationRepository.searchNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    )).thenReturn(Collections.emptyList());

    // when
    notificationService.getNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);
    notificationService.getNotifications(userId, cursor, idAfter, limit, sortDirection, sortBy);

    // then
    verify(notificationRepository, times(1)).searchNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    );
  }
}
