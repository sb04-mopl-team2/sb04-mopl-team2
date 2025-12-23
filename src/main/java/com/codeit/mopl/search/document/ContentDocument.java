package com.codeit.mopl.search.document;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentDocument extends AbstractDocument {
  private String type;
  private String title;
  private String description;
  private String thumbnailUrl;
  private List<String> tags = new ArrayList<>();
  private Double averageRating = 0.0;
  private Integer reviewCount = 0;
}
