package com.codeit.mopl.domain.content.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContentTypeTest {

    @Test
    @DisplayName("movie 타입으로 ContentType을 생성한다")
    void fromType_Movie() {
        // when
        ContentType result = ContentType.fromType("movie");

        // then
        assertThat(result).isEqualTo(ContentType.MOVIE);
        assertThat(result.getType()).isEqualTo("movie");
    }

    @Test
    @DisplayName("tvSeries 타입으로 ContentType을 생성한다")
    void fromType_TvSeries() {
        // when
        ContentType result = ContentType.fromType("tvSeries");

        // then
        assertThat(result).isEqualTo(ContentType.TV);
        assertThat(result.getType()).isEqualTo("tvSeries");
    }

    @Test
    @DisplayName("sport 타입으로 ContentType을 생성한다")
    void fromType_Sport() {
        // when
        ContentType result = ContentType.fromType("sport");

        // then
        assertThat(result).isEqualTo(ContentType.SPORT);
        assertThat(result.getType()).isEqualTo("sport");
    }

    @ParameterizedTest
    @CsvSource({
        "movie, MOVIE",
        "tvSeries, TV",
        "sport, SPORT"
    })
    @DisplayName("모든 유효한 타입으로 ContentType을 생성할 수 있다")
    void fromType_AllValidTypes(String type, String expected) {
        // when
        ContentType result = ContentType.fromType(type);

        // then
        assertThat(result.name()).isEqualTo(expected);
    }

    @Test
    @DisplayName("유효하지 않은 타입으로 생성 시 예외가 발생한다")
    void fromType_InvalidType() {
        // when & then
        assertThatThrownBy(() -> ContentType.fromType("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 콘텐츠 타입입니다")
            .hasMessageContaining("invalid");
    }

    @Test
    @DisplayName("null 타입으로 생성 시 예외가 발생한다")
    void fromType_NullType() {
        // when & then
        assertThatThrownBy(() -> ContentType.fromType(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("빈 문자열로 생성 시 예외가 발생한다")
    void fromType_EmptyString() {
        // when & then
        assertThatThrownBy(() -> ContentType.fromType(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 콘텐츠 타입입니다");
    }

    @Test
    @DisplayName("대소문자가 정확하지 않으면 예외가 발생한다")
    void fromType_CaseSensitive() {
        // when & then
        assertThatThrownBy(() -> ContentType.fromType("MOVIE"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ContentType.fromType("Movie"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("모든 ContentType은 고유한 type 값을 가진다")
    void allTypesHaveUniqueValues() {
        // when
        String movieType = ContentType.MOVIE.getType();
        String tvType = ContentType.TV.getType();
        String sportType = ContentType.SPORT.getType();

        // then
        assertThat(movieType).isNotEqualTo(tvType);
        assertThat(movieType).isNotEqualTo(sportType);
        assertThat(tvType).isNotEqualTo(sportType);
    }
}