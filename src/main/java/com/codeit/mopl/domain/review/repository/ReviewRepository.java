package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.domain.review.entity.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

}
