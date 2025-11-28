package com.codeit.mopl.search.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbstractDocument {
  private UUID id;
}
