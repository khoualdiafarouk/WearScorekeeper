package com.example.wearscorekeeperjava.core;

public class MatchRules {
    private final SportType sport;
    private final int setsToWin;
    private final int gamesPerSet;
    private final int tieBreakAt;
    private final boolean superTieBreakFinalSet;
    private final boolean goldenPoint;

    public MatchRules(SportType sport) { this(sport, 2, 6, 6, false, false); }

    public MatchRules(SportType sport, int setsToWin, int gamesPerSet, int tieBreakAt,
                      boolean superTieBreakFinalSet, boolean goldenPoint) {
        this.sport = sport;
        this.setsToWin = setsToWin;
        this.gamesPerSet = gamesPerSet;
        this.tieBreakAt = tieBreakAt;
        this.superTieBreakFinalSet = superTieBreakFinalSet;
        this.goldenPoint = goldenPoint;
    }

    public SportType getSport() { return sport; }
    public int getSetsToWin() { return setsToWin; }
    public int getGamesPerSet() { return gamesPerSet; }
    public int getTieBreakAt() { return tieBreakAt; }
    public boolean isSuperTieBreakFinalSet() { return superTieBreakFinalSet; }
    public boolean isGoldenPoint() { return goldenPoint; }
}