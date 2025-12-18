package com.codeit.mopl.batch.tmdb.movie.dto;

import com.codeit.mopl.batch.tmdb.base.dto.TmdbDiscoverResponse;
import com.codeit.mopl.batch.tmdb.movie.dto.TmdbDiscoverMovieResponse.Movie;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmdbDiscoverMovieResponse implements TmdbDiscoverResponse<Movie> {

  private int page;

  private List<Movie> results;

  @JsonProperty("total_pages")
  private int totalPages;

  @JsonProperty("total_results")
  private int totalResults;

  @Getter
  @Setter
  public static class Movie {

    private boolean adult;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("genre_ids")
    private List<Integer> genreIds;

    private int id;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;

    private double popularity;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("release_date")
    private String releaseDate;

    private String title;

    private boolean video;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("vote_count")
    private int voteCount;
  }
}