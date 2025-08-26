package com.example.wearscorekeeperjava.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime state for a match.
 * Holds per-point/game/set counters, server, tie-break flags/counters, and the list of completed sets.
 *
 * NOTE: This class expects a SideState class in the same package with points/games/sets + getters/setters:
 *   public final class SideState {
 *       private int points, games, sets;
 *       public int getPoints(){...} public void setPoints(int v){...}
 *       public int getGames(){...}  public void setGames(int v){...}
 *       public int getSets(){...}   public void setSets(int v){...}
 *   }
 * If you don't already have it, tell me and I'll provide one.
 */
public class MatchState {

    private final MatchRules rules;

    // Sides
    private final SideState left = new SideState();
    private final SideState right = new SideState();

    // Serving / flow
    private boolean serverLeft = true;
    private boolean inTieBreak = false;
    private boolean finished = false;

    // Tie-break raw points for current set (only used when inTieBreak == true)
    private int tieBreakLeft = 0;
    private int tieBreakRight = 0;

    // Completed sets history (for UI like "6-0 7-6(5)")
    private final List<SetRecord> completedSets = new ArrayList<>();

    public MatchState(MatchRules rules) {
        this.rules = rules;
    }

    // --- Accessors ---

    public MatchRules getRules() {
        return rules;
    }

    public SideState getLeft() {
        return left;
    }

    public SideState getRight() {
        return right;
    }

    public boolean isServerLeft() {
        return serverLeft;
    }

    public void setServerLeft(boolean serverLeft) {
        this.serverLeft = serverLeft;
    }

    public boolean isInTieBreak() {
        return inTieBreak;
    }

    public void setInTieBreak(boolean inTieBreak) {
        this.inTieBreak = inTieBreak;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int getTieBreakLeft() {
        return tieBreakLeft;
    }

    public void setTieBreakLeft(int tieBreakLeft) {
        this.tieBreakLeft = tieBreakLeft;
    }

    public int getTieBreakRight() {
        return tieBreakRight;
    }

    public void setTieBreakRight(int tieBreakRight) {
        this.tieBreakRight = tieBreakRight;
    }

    public List<SetRecord> getCompletedSets() {
        return completedSets;
    }
}
