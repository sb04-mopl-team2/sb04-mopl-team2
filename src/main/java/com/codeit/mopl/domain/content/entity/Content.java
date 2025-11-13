package com.codeit.mopl.domain.content.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Content extends UpdatableEntity {

  @NotNull
  @Column(nullable = false)
  private String title;

  @NotBlank
  @Column(nullable = false)
  private String description;

  private String thumbnailUrl;

  @ElementCollection
  @CollectionTable(name = "content_tags", joinColumns = @JoinColumn(name = "content_id"))
  @Column(name = "tag")
  private List<String> tags = new ArrayList<>();

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContentType contentType;

  @Column(columnDefinition = "DOUBLE DEFAULT 0.0")
  private Double averageRating = 0.0;

  @Column(columnDefinition = "INTEGER DEFAULT 0")
  private Integer reviewCount = 0;
}
