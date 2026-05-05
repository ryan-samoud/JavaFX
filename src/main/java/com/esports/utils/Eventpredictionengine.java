package com.esports.utils;

import com.esports.model.Evenement;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * UTILS — EventPredictionEngine.java
 * Rule-based AI that scores an event out of 5 and generates a prediction statement.
 * No external libraries needed.
 */
public class Eventpredictionengine {

    public static class Prediction {
        public final double rating;       // 0.0 – 5.0
        public final String statement;    // French statement
        public final String ratingLabel;  // "Excellent", "Bon", etc.

        public Prediction(double rating, String statement) {
            this.rating    = rating;
            this.statement = statement;
            if      (rating >= 4.5) { ratingLabel = "Excellent";  }
            else if (rating >= 3.5) { ratingLabel = "Bon";  }
            else if (rating >= 2.5) { ratingLabel = "Moyen";  }
            else if (rating >= 1.5) { ratingLabel = "Décevant"; }
            else                    { ratingLabel = "Peu prometteur"; }
        }
    }

    // ── Keyword dictionaries ───────────────────────────────────────

    private static final Map<String, Double> POSITIVE_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, Double> NEGATIVE_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String> THEME_LABELS      = new LinkedHashMap<>();

    static {
        // Positive signals
        POSITIVE_KEYWORDS.put("tournament",    0.5);
        POSITIVE_KEYWORDS.put("tournoi",       0.5);
        POSITIVE_KEYWORDS.put("championship",  0.5);
        POSITIVE_KEYWORDS.put("championnat",   0.5);
        POSITIVE_KEYWORDS.put("gaming",        0.4);
        POSITIVE_KEYWORDS.put("esport",        0.4);
        POSITIVE_KEYWORDS.put("esports",       0.4);
        POSITIVE_KEYWORDS.put("lan",           0.4);
        POSITIVE_KEYWORDS.put("compétition",   0.4);
        POSITIVE_KEYWORDS.put("competition",   0.4);
        POSITIVE_KEYWORDS.put("prix",          0.3);
        POSITIVE_KEYWORDS.put("prize",         0.3);
        POSITIVE_KEYWORDS.put("grand",         0.3);
        POSITIVE_KEYWORDS.put("festival",      0.4);
        POSITIVE_KEYWORDS.put("gala",          0.3);
        POSITIVE_KEYWORDS.put("concert",       0.3);
        POSITIVE_KEYWORDS.put("expo",          0.3);
        POSITIVE_KEYWORDS.put("exhibition",    0.3);
        POSITIVE_KEYWORDS.put("exclusive",     0.3);
        POSITIVE_KEYWORDS.put("exclusif",      0.3);
        POSITIVE_KEYWORDS.put("premium",       0.4);
        POSITIVE_KEYWORDS.put("vip",           0.4);
        POSITIVE_KEYWORDS.put("gratuit",       0.3);
        POSITIVE_KEYWORDS.put("free",          0.2);
        POSITIVE_KEYWORDS.put("open",          0.2);
        POSITIVE_KEYWORDS.put("final",         0.4);
        POSITIVE_KEYWORDS.put("finale",        0.4);
        POSITIVE_KEYWORDS.put("annuel",        0.3);
        POSITIVE_KEYWORDS.put("annual",        0.3);
        POSITIVE_KEYWORDS.put("international", 0.5);
        POSITIVE_KEYWORDS.put("world",         0.5);
        POSITIVE_KEYWORDS.put("nexus",         0.3);

        // Negative signals
        NEGATIVE_KEYWORDS.put("annulé",     0.8);
        NEGATIVE_KEYWORDS.put("cancelled",  0.8);
        NEGATIVE_KEYWORDS.put("test",       0.3);
        NEGATIVE_KEYWORDS.put("placeholder",0.5);
        NEGATIVE_KEYWORDS.put("lorem",      0.5);
        NEGATIVE_KEYWORDS.put("tbd",        0.4);

        // Theme labels for statement generation
        THEME_LABELS.put("tournoi",      "compétitif");
        THEME_LABELS.put("tournament",   "compétitif");
        THEME_LABELS.put("gaming",       "gaming");
        THEME_LABELS.put("esport",       "eSports");
        THEME_LABELS.put("lan",          "LAN");
        THEME_LABELS.put("festival",     "festif");
        THEME_LABELS.put("concert",      "musical");
        THEME_LABELS.put("expo",         "expositionnel");
        THEME_LABELS.put("vip",          "premium");
        THEME_LABELS.put("final",        "final");
        THEME_LABELS.put("finale",       "final");
        THEME_LABELS.put("international","international");
    }

    // ── Main predict method ────────────────────────────────────────

    public static Prediction predict(Evenement e) {
        double score = 2.5; // base score
        List<String> positives = new ArrayList<>();
        List<String> negatives = new ArrayList<>();
        String detectedTheme   = null;

        String text = ((e.getNom()         != null ? e.getNom()         : "") + " " +
                (e.getDescription() != null ? e.getDescription() : "") + " " +
                (e.getLieu()        != null ? e.getLieu()        : "")).toLowerCase();

        // ── Keyword scoring ──
        for (Map.Entry<String, Double> entry : POSITIVE_KEYWORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                score += entry.getValue();
                if (detectedTheme == null && THEME_LABELS.containsKey(entry.getKey()))
                    detectedTheme = THEME_LABELS.get(entry.getKey());
            }
        }
        for (Map.Entry<String, Double> entry : NEGATIVE_KEYWORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                score -= entry.getValue();
                negatives.add(entry.getKey());
            }
        }

        // ── Participant score ──
        int nbr = e.getNbrParticipant();
        if      (nbr >= 500) { score += 0.8; positives.add("très forte affluence attendue"); }
        else if (nbr >= 200) { score += 0.5; positives.add("forte affluence attendue"); }
        else if (nbr >= 50)  { score += 0.2; positives.add("bonne affluence attendue"); }
        else if (nbr > 0)    { score -= 0.1; }
        else                 { score -= 0.3; negatives.add("aucun participant enregistré"); }

        // ── Date proximity score ──
        if (e.getDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), e.getDate());
            if (daysUntil < 0) {
                // Past event
                score -= 0.5;
                negatives.add("événement déjà passé");
            } else if (daysUntil <= 7) {
                score += 0.4;
                positives.add("événement imminent");
            } else if (daysUntil <= 30) {
                score += 0.2;
                positives.add("événement proche");
            } else if (daysUntil > 365) {
                score -= 0.2;
                negatives.add("événement encore lointain");
            }
        } else {
            score -= 0.3;
            negatives.add("date non définie");
        }

        // ── Description quality score ──
        String desc = e.getDescription() != null ? e.getDescription().trim() : "";
        if (desc.length() > 150)     { score += 0.3; positives.add("description détaillée"); }
        else if (desc.length() > 50) { score += 0.1; }
        else if (desc.isEmpty())     { score -= 0.3; negatives.add("aucune description"); }

        // ── Lieu score ──
        String lieu = e.getLieu() != null ? e.getLieu().toLowerCase() : "";
        if (lieu.contains("tunis") || lieu.contains("sfax") || lieu.contains("sousse"))
            score += 0.2;
        if (lieu.isEmpty()) { score -= 0.2; negatives.add("lieu non spécifié"); }

        // ── Clamp to 0–5 ──
        score = Math.max(0.0, Math.min(5.0, score));
        score = Math.round(score * 10.0) / 10.0;

        // ── Generate statement ──
        String statement = generateStatement(score, detectedTheme, positives, negatives, e);

        return new Prediction(score, statement);
    }

    // ── Statement generation ───────────────────────────────────────

    private static String generateStatement(double score, String theme,
                                            List<String> positives, List<String> negatives,
                                            Evenement e) {
        StringBuilder sb = new StringBuilder();

        if (score >= 4.5) {
            sb.append("Cet événement s'annonce exceptionnel ! ");
            if (theme != null) sb.append("Son caractère ").append(theme).append(" et ");
            else sb.append("Ses caractéristiques remarquables et ");
            sb.append("son fort potentiel d'engagement en font une opportunité à ne pas manquer.");
        } else if (score >= 3.5) {
            sb.append("Cet événement présente de bonnes perspectives. ");
            if (theme != null) sb.append("La thématique ").append(theme).append(" attire généralement un public fidèle. ");
            if (!positives.isEmpty()) sb.append("Points forts : ").append(String.join(", ", positives.subList(0, Math.min(2, positives.size())))).append(".");
        } else if (score >= 2.5) {
            sb.append("Cet événement est prometteur mais manque de certains éléments pour se démarquer. ");
            if (!positives.isEmpty()) sb.append("Il bénéficie de ").append(positives.get(0)).append(". ");
            if (!negatives.isEmpty()) sb.append("Cependant, ").append(negatives.get(0)).append(" pourrait freiner son succès.");
        } else if (score >= 1.5) {
            sb.append("Cet événement présente plusieurs points faibles. ");
            if (!negatives.isEmpty()) sb.append("Notamment : ").append(String.join(", ", negatives.subList(0, Math.min(2, negatives.size())))).append(". ");
            sb.append("Des améliorations sont recommandées avant sa tenue.");
        } else {
            sb.append("Cet événement, en l'état actuel, ne semble pas très prometteur. ");
            if (!negatives.isEmpty()) sb.append("Les problèmes identifiés : ").append(String.join(", ", negatives)).append(". ");
            sb.append("Une révision complète est conseillée.");
        }

        // Add participant context
        if (e.getNbrParticipant() > 0) {
            sb.append("\n\n👥 ").append(e.getNbrParticipant())
                    .append(" participant(s) enregistré(s) — ");
            if (e.getNbrParticipant() >= 500)     sb.append("engagement exceptionnel.");
            else if (e.getNbrParticipant() >= 200) sb.append("bon taux d'engagement.");
            else if (e.getNbrParticipant() >= 50)  sb.append("engagement correct.");
            else                                   sb.append("faible taux d'engagement pour l'instant.");
        }

        return sb.toString();
    }
}