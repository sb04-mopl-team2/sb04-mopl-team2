package com.codeit.mopl.batch.tmdb.tvseries.dto;

import com.codeit.mopl.batch.tmdb.base.dto.TmdbDiscoverResponse;
import com.codeit.mopl.batch.tmdb.tvseries.dto.TmdbDiscoverTvResponse.TvShow;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmdbDiscoverTvResponse implements TmdbDiscoverResponse<TvShow> {

  private int page;

  private List<TvShow> results;

  @JsonProperty("total_pages")
  private int totalPages;

  @JsonProperty("total_results")
  private int totalResults;

  @Getter
  @Setter
  public static class TvShow {

    private boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    private int id;

    @JsonProperty("origin_country")
    private List<String> originCountry;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_name")
    private String originalName;

    private String overview;

    private double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    private String name;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("vote_count")
    private int voteCount;
  }
}