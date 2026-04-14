package com.esports.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * MODEL — Entité Équipe.
 */
public class Team {

    private final IntegerProperty id;
    private final StringProperty  name;
    private final StringProperty  tag;       // ex: "NXS"
    private final StringProperty  game;
    private final IntegerProperty wins;
    private final IntegerProperty losses;
    private final StringProperty  logoPath;
    private final ObservableList<Player> players;

    public Team(int id, String name, String tag, String game, int wins, int losses, String logoPath) {
        this.id       = new SimpleIntegerProperty(id);
        this.name     = new SimpleStringProperty(name);
        this.tag      = new SimpleStringProperty(tag);
        this.game     = new SimpleStringProperty(game);
        this.wins     = new SimpleIntegerProperty(wins);
        this.losses   = new SimpleIntegerProperty(losses);
        this.logoPath = new SimpleStringProperty(logoPath);
        this.players  = FXCollections.observableArrayList();
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int    getId()                 { return id.get(); }
    public IntegerProperty idProperty()  { return id; }

    public String getName()               { return name.get(); }
    public void   setName(String v)       { name.set(v); }
    public StringProperty nameProperty()  { return name; }

    public String getTag()                { return tag.get(); }
    public void   setTag(String v)        { tag.set(v); }
    public StringProperty tagProperty()   { return tag; }

    public String getGame()               { return game.get(); }
    public void   setGame(String v)       { game.set(v); }
    public StringProperty gameProperty()  { return game; }

    public int    getWins()               { return wins.get(); }
    public void   setWins(int v)          { wins.set(v); }
    public IntegerProperty winsProperty() { return wins; }

    public int    getLosses()             { return losses.get(); }
    public void   setLosses(int v)        { losses.set(v); }
    public IntegerProperty lossesProperty() { return losses; }

    public String getLogoPath()            { return logoPath.get(); }
    public StringProperty logoPathProperty() { return logoPath; }

    public ObservableList<Player> getPlayers() { return players; }

    /** Calcule le win-rate en % */
    public double getWinRate() {
        int total = wins.get() + losses.get();
        return total == 0 ? 0 : (wins.get() * 100.0 / total);
    }

    @Override
    public String toString() { return "[" + tag.get() + "] " + name.get(); }
}
