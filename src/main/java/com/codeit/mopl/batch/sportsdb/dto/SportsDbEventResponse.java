package com.codeit.mopl.batch.sportsdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SportsDbEventResponse {

  private List<Event> events;

  @Getter
  @Setter
  public static class Event {

    private String idEvent;

    @JsonProperty("idAPIfootball")
    private String idApiFootball;

    private String strEvent;

    private String strEventAlternate;

    private String strFilename;

    private String strSport;

    private String idLeague;

    private String strLeague;

    private String strLeagueBadge;

    private String strSeason;

    private String strDescriptionEN;

    private String strHomeTeam;

    private String strAwayTeam;

    private Integer intHomeScore;

    private String intRound;

    private Integer intAwayScore;

    private Integer intSpectators;

    private String strOfficial;

    private String strTimestamp;

    private String dateEvent;

    private String dateEventLocal;

    private String strTime;

    private String strTimeLocal;

    private String strGroup;

    private String idHomeTeam;

    private String strHomeTeamBadge;

    private String idAwayTeam;

    private String strAwayTeamBadge;

    private Integer intScore;

    private Integer intScoreVotes;

    private String strResult;

    private String idVenue;

    private String strVenue;

    private String strCountry;

    private String strCity;

    private String strPoster;

    private String strSquare;

    private String strFanart;

    private String strThumb;

    private String strBanner;

    private String strMap;

    private String strTweet1;

    private String strVideo;

    private String strStatus;

    private String strPostponed;

    private String strLocked;
  }
}