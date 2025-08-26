package com.example.wearscorekeeperjava.core;

import java.util.List;

/**
 * ScoreEngine with:
 * - Classic game scoring (0-15-30-40-Ad-Game)
 * - Set flow with win-by-2 at >=6 games, or tie-break at rules.getTieBreakAt()
 * - Tie-break: first to 7 with 2-point lead (stores raw TB points for 7-6(x) formatting)
 * - Completed sets saved in MatchState.completedSets as SetRecord
 */
public class ScoreEngine {
    private final MatchRules rules;
    private final MatchState state;

    public ScoreEngine(MatchRules rules) {
        this.rules = rules;
        this.state = new MatchState(rules);
        // Ensure clean TB counters and history
        state.setTieBreakLeft(0);
        state.setTieBreakRight(0);
        state.setInTieBreak(false);
        state.getCompletedSets().clear();
    }

    public MatchState getState() { return state; }

    // ----- Public API -----

    public void pointLeft() { point(true); }
    public void pointRight() { point(false); }

    public void toggleServer() { state.setServerLeft(!state.isServerLeft()); }

    /** Resets the whole match (including history of completed sets). */
    public void reset() {
        MatchState s = getState();
        s.getLeft().setPoints(0); s.getLeft().setGames(0); s.getLeft().setSets(0);
        s.getRight().setPoints(0); s.getRight().setGames(0); s.getRight().setSets(0);
        s.setServerLeft(true);
        s.setInTieBreak(false);
        s.setFinished(false);
        s.setTieBreakLeft(0);
        s.setTieBreakRight(0);
        s.getCompletedSets().clear();
    }

    /** Simple compact line, unchanged from your version. */
    public String scoreLineShort() {
        String gamePair = state.getLeft().getGames() + "-" + state.getRight().getGames() + (state.isServerLeft() ? "*" : "");
        return state.getLeft().getSets() + "-" + state.getRight().getSets() + " " + gamePair;
    }

    // ----- Internals -----

    private void point(boolean isLeft) {
        if (state.isFinished()) return;

        SideState left = state.getLeft();
        SideState right = state.getRight();

        if (state.isInTieBreak()) {
            // In TB we count raw points separately to preserve them for 7-6(x) formatting
            if (isLeft) {
                state.setTieBreakLeft(state.getTieBreakLeft() + 1);
            } else {
                state.setTieBreakRight(state.getTieBreakRight() + 1);
            }
            checkTieBreakEnd();
            return;
        }

        // Classic game scoring
        int[] next = isLeft
                ? nextPoint(left.getPoints(), right.getPoints())
                : swap(nextPoint(right.getPoints(), left.getPoints()));
        left.setPoints(next[0]);
        right.setPoints(next[1]);

        if (isGameWon(next[0], next[1])) {
            // Award game to winner
            if (next[0] > next[1]) left.setGames(left.getGames() + 1);
            else right.setGames(right.getGames() + 1);

            // Clear points for next game
            left.setPoints(0);
            right.setPoints(0);

            // Toggle server after a normal game
            state.setServerLeft(!state.isServerLeft());

            // Handle set progression or tie-break start
            endGame(false);
        }
    }

    private int[] swap(int[] p) { return new int[]{p[1], p[0]}; }

    private int[] nextPoint(int pFor, int pAgainst) {
        // Normal increments up to 40
        if (pFor == 0)  return new int[]{15, pAgainst};
        if (pFor == 15) return new int[]{30, pAgainst};
        if (pFor == 30) return new int[]{40, pAgainst};

        // If we have 40 and opponent < 40 -> we win the game
        if (pFor == 40 && pAgainst < 40) return new int[]{100, pAgainst};

        // 40-40 -> we move to Advantage
        if (pFor == 40 && pAgainst == 40) return new int[]{50, 40};  // Adv for scoring side

        // Opponent had Advantage and we score -> back to Deuce (40-40)
        if (pFor == 40 && pAgainst == 50) return new int[]{40, 40};

        // We already had Advantage and we score -> win the game
        if (pFor == 50) return new int[]{100, pAgainst};

        // Default: no change
        return new int[]{pFor, pAgainst};
    }

    private boolean isGameWon(int pL, int pR) {
        return (pL == 100 || pR == 100);
    }

    /**
     * Called after every normal (non-tiebreak) game is won.
     * Decides: start tie-break at the configured score, or finish the set on win-by-2 at >=6 games.
     */
    private void endGame(boolean tiebreakJustEnded) {
        SideState left = state.getLeft();
        SideState right = state.getRight();

        final int needGames = rules.getGamesPerSet(); // typically 6
        final int tieBreakAt = rules.getTieBreakAt(); // typically 6

        // If we just completed a TB, finishSet() was already called there. Nothing else to do.
        if (tiebreakJustEnded) {
            checkMatchFinished();
            return;
        }

        int lg = left.getGames();
        int rg = right.getGames();
        boolean leadOk = Math.abs(lg - rg) >= 2;

        // Start tie-break at (tieBreakAt, tieBreakAt)
        if (lg == tieBreakAt && rg == tieBreakAt) {
            state.setInTieBreak(true);
            state.setTieBreakLeft(0);
            state.setTieBreakRight(0);
            return;
        }

        // Normal set end (win by 2 at >= needGames)
        if ((lg >= needGames || rg >= needGames) && leadOk) {
            finishSet(/*tbL*/ null, /*tbR*/ null);
            checkMatchFinished();
        }
    }

    /**
     * Tie-break: first to 7 with 2-point lead (classic).
     * Stores TB raw points and adds a SetRecord with 7–6 (or 6–7) plus (loserTB) kept for UI.
     */
    private void checkTieBreakEnd() {
        int l = state.getTieBreakLeft();
        int r = state.getTieBreakRight();

        final int winPoints = 7; // could be made rule-driven (e.g., 10-point super TB)
        if ((l >= winPoints || r >= winPoints) && Math.abs(l - r) >= 2) {
            boolean leftWinsTB = l > r;

            // Convert current games to a 7–6 set for the TB winner
            if (leftWinsTB) {
                state.getLeft().setGames(7);
                state.getRight().setGames(6);
            } else {
                state.getLeft().setGames(6);
                state.getRight().setGames(7);
            }

            // Store TB points in the record (both kept; UI can show loser’s in parentheses)
            finishSet(/*tbL*/ l, /*tbR*/ r);

            // Reset TB flags/counters and proceed
            state.setInTieBreak(false);
            state.setTieBreakLeft(0);
            state.setTieBreakRight(0);

            // After a set ends, server typically alternates for next set start; leave as-is here.
            checkMatchFinished();
        }
    }

    /**
     * Adds a SetRecord to completedSets and bumps sets for the winner, then resets games for next set.
     */
    private void finishSet(Integer tbLeft, Integer tbRight) {
        SideState left = state.getLeft();
        SideState right = state.getRight();

        // Save the completed set snapshot
        List<SetRecord> history = state.getCompletedSets();
        history.add(new SetRecord(left.getGames(), right.getGames(), tbLeft, tbRight));

        // Increment sets to the winner
        if (left.getGames() > right.getGames()) {
            left.setSets(left.getSets() + 1);
        } else {
            right.setSets(right.getSets() + 1);
        }

        // Reset games for next set
        left.setGames(0);
        right.setGames(0);
    }

    private void checkMatchFinished() {
        SideState left = state.getLeft();
        SideState right = state.getRight();
        boolean finished = (left.getSets() >= rules.getSetsToWin() || right.getSets() >= rules.getSetsToWin());
        state.setFinished(finished);
    }

}
