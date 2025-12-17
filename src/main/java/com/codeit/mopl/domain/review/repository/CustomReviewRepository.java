package com.codeit.mopl.domain.review.repository;


import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.base.SortDirection;
import java.util.List;
import java.util.UUID;

public interface CustomReviewRepository {

  List<Review> searchReview(
      UUID contentId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      ReviewSortBy sortBy);
}
