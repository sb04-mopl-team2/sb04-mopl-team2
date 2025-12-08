package com.codeit.mopl.batch.tmdb.base.dto;

import java.util.List;

public interface TmdbDiscoverResponse<T> {

  int getPage();

  List<T> getResults();

  int getTotalPages();

  int getTotalResults();
}
