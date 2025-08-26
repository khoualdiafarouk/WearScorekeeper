package com.example.wearscorekeeperjava.core;

/**
 * Immutable snapshot of a completed set.
 * If a tie-break decided the set, tieBreakLeft/right hold the raw TB points.
 * Otherwise they are null.
 */
public class SetRecord {
    private final int leftGames;
    private final int rightGames;
    private final Integer tieBreakLeft;   // null if no TB
    private final Integer tieBreakRight;  // null if no TB

    public SetRecord(int leftGames, int rightGames, Integer tieBreakLeft, Integer tieBreakRight) {
        this.leftGames = leftGames;
        this.rightGames = rightGames;
        this.tieBreakLeft = tieBreakLeft;
        this.tieBreakRight = tieBreakRight;
    }

    public int getLeftGames() {
        return leftGames;
    }

    public int getRightGames() {
        return rightGames;
    }

    public Integer getTieBreakLeft() {
        return tieBreakLeft;
    }

    public Integer getTieBreakRight() {
        return tieBreakRight;
    }
}
