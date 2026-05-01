package com.esports.model;

public class ParticipantRanking {
    private String playerName;
    private int matchesPlayed;
    private int wins;
    private int draws;
    private int losses;
    private int goalsFor;
    private int goalsAgainst;

    public ParticipantRanking(String playerName) {
        this.playerName = playerName;
    }

    public void addMatchPlayed() { this.matchesPlayed++; }
    public void addWin() { this.wins++; }
    public void addDraw() { this.draws++; }
    public void addLoss() { this.losses++; }
    public void addGoalsFor(int g) { this.goalsFor += g; }
    public void addGoalsAgainst(int g) { this.goalsAgainst += g; }

    public String getPlayerName() { return playerName; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }

    public int getPoints() { return (wins * 3) + draws; }
    public int getGoalDifference() { return goalsFor - goalsAgainst; }

    @Override
    public String toString() {
        return playerName + " : " + getPoints() + " pts";
    }
}
