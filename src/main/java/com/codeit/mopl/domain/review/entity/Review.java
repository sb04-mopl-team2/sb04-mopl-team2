package com.codeit.mopl.domain.review.entity;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.base.DeletableEntity;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "reviews")
@NoArgsConstructor
public class Review extends DeletableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, columnDefinition = "uuid")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false, columnDefinition = "uuid")
  private Content content;

  @Column(nullable = false)
  private String text;

  @Column(nullable = false)
  private Double rating;

  private Boolean isDeleted;

@Getter
@Setter
@Entity
@Table(name = "reviews")
@NoArgsConstructor
public class Review extends DeletableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, columnDefinition = "uuid")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false, columnDefinition = "uuid")
  private Content content;

  @Column(nullable = false)
  private String text;

  @Column(nullable = false)
  private Double rating;

  @Column(nullable = false)
  private Boolean isDeleted = false;
}
