package com.codeit.mopl.domain.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.content.dto.request.ContentCreateRequest;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.service.ContentService;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(ContentController.class)
@Import(TestSecurityConfig.class)
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContentService contentService;

    @Test
    @DisplayName("관리자는 콘텐츠를 생성할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void createContent_Success() throws Exception {
        // given
        List<String> tags = Arrays.asList("액션", "스릴러");
        ContentCreateRequest request = new ContentCreateRequest(
            "movie",
            "범죄도시",
            "액션 영화",
            tags
        );

        MockMultipartFile thumbnail = new MockMultipartFile(
            "thumbnail",
            "thumbnail.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image".getBytes()
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

        ContentDto responseDto = new ContentDto(
            UUID.randomUUID(),
            "movie",
            "범죄도시",
            "액션 영화",
            "thumbnail-url",
            tags,
            0.0,
            0,
            0L
        );

        when(contentService.createContent(any(ContentCreateRequest.class), any(MultipartFile.class)))
            .thenReturn(responseDto);

        // when & then
        mockMvc.perform(multipart("/api/contents")
                .file(requestPart)
                .file(thumbnail)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("범죄도시"))
            .andExpect(jsonPath("$.type").value("movie"))
            .andExpect(jsonPath("$.thumbnailUrl").value("thumbnail-url"));

        verify(contentService).createContent(any(ContentCreateRequest.class), any(MultipartFile.class));
    }

    @Test
    @DisplayName("썸네일 없이 콘텐츠를 생성할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void createContent_WithoutThumbnail() throws Exception {
        // given
        List<String> tags = Arrays.asList("드라마");
        ContentCreateRequest request = new ContentCreateRequest(
            "tvSeries",
            "테스트 드라마",
            "드라마 설명",
            tags
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

        ContentDto responseDto = new ContentDto(
            UUID.randomUUID(),
            "tvSeries",
            "테스트 드라마",
            "드라마 설명",
            null,
            tags,
            0.0,
            0,
            0L
        );

        when(contentService.createContent(any(ContentCreateRequest.class), eq(null)))
            .thenReturn(responseDto);

        // when & then
        mockMvc.perform(multipart("/api/contents")
                .file(requestPart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("테스트 드라마"))
            .andExpect(jsonPath("$.thumbnailUrl").isEmpty());
    }

    @Test
    @DisplayName("일반 사용자는 콘텐츠를 생성할 수 없다")
    @WithMockUser(roles = "USER")
    void createContent_Forbidden() throws Exception {
        // given
        List<String> tags = Arrays.asList("액션");
        ContentCreateRequest request = new ContentCreateRequest(
            "movie",
            "테스트",
            "설명",
            tags
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

        // when & then
        mockMvc.perform(multipart("/api/contents")
                .file(requestPart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isForbidden());
    }
}