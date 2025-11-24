package com.codeit.mopl.domain.content;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import java.util.List;
import lombok.Builder;

public class ContentTestFactory {

  @Builder(builderMethodName = "testBuilder")
  private static Content buildInternal(
      String title,
      String description,
      ContentType type,
      String thumbnailUrl,
      List<String> tags,
      Double averageRating,
      Integer reviewCount
  ) {
    Content content = new Content();
    content.setTitle(title);
    content.setDescription(description);
    content.setContentType(type);
    content.setThumbnailUrl(thumbnailUrl);
    content.setTags(tags);
    content.setAverageRating(averageRating);
    content.setReviewCount(reviewCount);
    return content;
  }

  public static Content createDefault(ContentType type) {
    return ContentTestFactory.testBuilder()
        .title("테스트 컨텐츠")
        .description("테스트 설명입니다.")
        .type(type)
        .thumbnailUrl("/test-thumbnail.jpg")
        .tags(List.of("tag1", "tag2"))
        .averageRating(0.0)
        .reviewCount(0)
        .build();
  }
}
