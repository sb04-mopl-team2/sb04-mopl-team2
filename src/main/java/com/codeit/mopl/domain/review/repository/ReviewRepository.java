package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReviewRepository extends JpaRepository<Review, UUID>, CustomReviewRepository {

  long countByContentIdAndIsDeleted(UUID contentId, Boolean isDeleted);

  Optional<Review> findByUserAndContent(User user, Content content);

  Optional<Review> findByUserAndContentAndIsDeletedFalse(User user, Content content);
}

