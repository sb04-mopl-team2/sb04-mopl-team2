package com.codeit.mopl.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentMapper contentMapper;

  @InjectMocks
  private ContentService contentService;

  @Test
  @DisplayName("콘텐츠 수동생성")
  void createContent_Success() {
    // given
    List<String> tags = Arrays.asList("액션", "스릴러");
    ContentCreateRequest request = new ContentCreateRequest(
        "movie",
        "범죄도시",
        "액션 영화",
        tags
    );

    MultipartFile thumbnail = mock(MultipartFile.class);
    given(thumbnail.isEmpty()).willReturn(false);

    Content content = new Content();
    content.setTitle("범죄도시");
    content.setContentType(ContentType.MOVIE);

    Content savedContent = new Content();
    savedContent.setTitle("범죄도시");
    savedContent.setThumbnailUrl("thumbnailUrl");

    ContentDto contentDto = new ContentDto(
        UUID.randomUUID(),
        "movie",
        "범죄도시",
        "액션 영화",
        "thumbnailUrl",
        tags,
        0.0,
        0,
        0L
    );

    given(contentMapper.fromCreateRequest(request)).willReturn(content);
    given(contentRepository.save(any(Content.class))).willReturn(savedContent);
    given(contentMapper.toDto(savedContent, 0L)).willReturn(contentDto);

    // when
    ContentDto result = contentService.createContent(request, thumbnail);

    // then
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("범죄도시");
    assertThat(result.thumbnailUrl()).isEqualTo("thumbnailUrl");
  }
}