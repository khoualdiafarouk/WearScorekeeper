package com.example.wearscorekeeperjava.core;

public class SideState {
    private int points;
    private int games;
    private int sets;

    public SideState() { this(0,0,0); }
    public SideState(int points, int games, int sets) {
        this.points = points; this.games = games; this.sets = sets;
    }

    public int getPoints() { return points; }
    public int getGames() { return games; }
    public int getSets() { return sets; }

    public void setPoints(int v) { points = v; }
    public void setGames(int v) { games = v; }
    public void setSets(int v) { sets = v; }
}