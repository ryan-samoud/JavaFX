package com.esports.service;

import com.esports.model.Jeu;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class GameCsvExportService {

    public void exportGames(List<Jeu> games, File outputFile) throws Exception {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file is required.");
        }

        StringBuilder csv = new StringBuilder();
        csv.append("id,nom,categorie,mode,tranche_age,nb_joueurs,note,image,description\n");

        if (games != null) {
            for (Jeu g : games) {
                String categorie = g.getCategorie() != null ? safe(g.getCategorie().getNomCategorie()) : "";
                csv.append(g.getId()).append(',')
                        .append(escape(safe(g.getNom()))).append(',')
                        .append(escape(categorie)).append(',')
                        .append(escape(safe(g.getMode()))).append(',')
                        .append(g.getTrancheAge()).append(',')
                        .append(g.getNbJoueurs()).append(',')
                        .append(g.getNote()).append(',')
                        .append(escape(safe(g.getImage()))).append(',')
                        .append(escape(safe(g.getDescription())))
                        .append('\n');
            }
        }

        Files.writeString(outputFile.toPath(), csv.toString(), StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}
