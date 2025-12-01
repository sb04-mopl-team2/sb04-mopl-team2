package com.codeit.mopl.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.codeit.mopl.domain.content.ContentTestFactory;
import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.dto.request.ContentSearchCondition;
import com.codeit.mopl.domain.content.dto.request.ContentSearchRequest;
import com.codeit.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    given(contentMapper.toDto(savedContent)).willReturn(contentDto);

    // when
    ContentDto result = contentService.createContent(request, thumbnail);

    // then
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("범죄도시");
    assertThat(result.thumbnailUrl()).isEqualTo("thumbnailUrl");
  }

  @Test
  @DisplayName("콘텐츠 목록 조회")
  void findContents_Success() {
    // given
    ContentSearchRequest request = new ContentSearchRequest();
    request.setCursor("cursor123");
    request.setLimit(10);
    request.setSortBy("createdAt");
    request.setSortDirection("ASCENDING");

    CursorResponseContentDto mockedResponse =
        new CursorResponseContentDto(
            List.of(),
            "nextCursorValue",
            UUID.randomUUID(),
            true,
            0L,
            "createdAt",
            "ASCENDING"
        );

    given(contentRepository.findContents(any(ContentSearchCondition.class)))
        .willReturn(mockedResponse);

    // when
    CursorResponseContentDto result = contentService.findContents(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.data()).isEmpty();
    assertThat(result.hasNext()).isTrue();
    assertThat(result.sortBy()).isEqualTo("createdAt");
    assertThat(result.sortDirection()).isEqualTo("ASCENDING");
  }

  @Test
  @DisplayName("콘텐츠 단건 조회 성공")
  void findContent_Success() {
    // given
    UUID contentId = UUID.randomUUID();

    Content content = ContentTestFactory.createDefault(ContentType.MOVIE);

    ContentDto contentDto = new ContentDto(
        contentId,
        "movie",
        "테스트 컨텐츠",
        "테스트 설명입니다.",
        "/test-thumbnail.jpg",
        List.of("tag1", "tag2"),
        0.0,
        0,
        0L
    );

    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
    given(contentMapper.toDto(content)).willReturn(contentDto);

    // when
    ContentDto result = contentService.findContent(contentId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(contentId);
    assertThat(result.title()).isEqualTo("테스트 컨텐츠");

    verify(contentRepository).findById(contentId);
    verify(contentMapper).toDto(content);
  }

  @Test
  @DisplayName("콘텐츠 수정 성공")
  void updateContent_Success_NoThumbnail() {
    // given
    UUID contentId = UUID.randomUUID();

    // 기존 엔티티(Factory가 기본값으로 생성)
    Content content = ContentTestFactory.createDefault(ContentType.MOVIE);

    // 요청객체: type 필드 포함
    ContentUpdateRequest request = new ContentUpdateRequest(
        "movie",                    // type
        "수정된 제목",               // title
        "수정된 설명",               // description
        List.of("tagA", "tagB")     // tags
    );

    // 리포지토리에서 엔티티 조회
    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

    // 매퍼가 어떤 watcherCount가 들어와도 expectedDto를 반환하도록 설정
    Long watcherCount = 7L;
    ContentDto expectedDto = new ContentDto(
        contentId,
        "movie",
        "수정된 제목",
        "수정된 설명",
        "/no-thumb.jpg",
        List.of("tagA", "tagB"),
        0.0,
        0,
        watcherCount
    );
    // contentMapper.toDto 호출을 넓게 매칭(anyLong)해서 고정된 DTO 반환
    given(contentMapper.toDto(content)).willReturn(expectedDto);

    // when
    ContentDto result = contentService.updateContent(contentId, request, null); // thumbnail = null

    // then
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("수정된 제목");
    assertThat(result.description()).isEqualTo("수정된 설명");
    assertThat(result.tags()).containsExactly("tagA", "tagB");

    verify(contentRepository).findById(contentId);
    verify(contentMapper).toDto(content);
  }

  @Test
  @DisplayName("콘텐츠 삭제 성공")
  void deleteContent_Success() {
    // given
    UUID contentId = UUID.randomUUID();

    Content content = ContentTestFactory.createDefault(ContentType.MOVIE);

    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

    // when
    contentService.deleteContent(contentId);

    // then
    verify(contentRepository).findById(contentId);
    verify(contentRepository).delete(content);
  }
}