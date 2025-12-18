package com.codeit.mopl.domain.content.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import com.codeit.mopl.domain.content.dto.request.ContentUpdateRequest;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

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

  @Column(columnDefinition = "TEXT")
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
