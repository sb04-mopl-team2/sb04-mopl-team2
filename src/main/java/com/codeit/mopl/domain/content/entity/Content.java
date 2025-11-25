package com.codeit.mopl.domain.content.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "contents")
public class Content extends UpdatableEntity {

  @NotNull
  @Column(nullable = false)
  private String title;

  @NotNull
  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  private String thumbnailUrl;

  @ElementCollection
  @CollectionTable(name = "contents_tags", joinColumns = @JoinColumn(name = "content_id"))
  @Column(name = "tag")
  private List<String> tags = new ArrayList<>();

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false)
  private ContentType contentType;

  @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
  private Double averageRating = 0.0;

  @Column(columnDefinition = "INTEGER DEFAULT 0")
  private Integer reviewCount = 0;

  @Column(columnDefinition = "INTEGER DEFAULT 0")
  private Integer watcherCount = 0;

  public void update(ContentUpdateRequest request) {
    this.title = request.title();
    this.description = request.description();
    this.tags = new ArrayList<>(request.tags());
    this.contentType = ContentType.fromType(request.type());
  }
}
