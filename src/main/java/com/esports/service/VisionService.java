package com.esports.service;

import com.esports.model.Produit;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class VisionService {

    // ---------------------------------------------------------------
    // Imagga API — gratuit 1000 req/mois sans CB
    // Créer un compte sur https://imagga.com pour obtenir ces valeurs
    // ---------------------------------------------------------------
    private static final String IMAGGA_KEY    = "acc_855653444bac5bf";            // <-- remplacer
    private static final String IMAGGA_SECRET = "54ffc11e17cf7d92fe47a2d0835509f9"; // <-- remplacer
    private static final String IMAGGA_URL    = "https://api.imagga.com/v2/tags";

    // Score minimum Imagga (0 à 100)
    private static final double SCORE_MIN = 30.0;

    // Dictionnaire EN → mots-clés FR pour les produits esports
    private static final Map<String, List<String>> TRADUCTIONS = new LinkedHashMap<>();
    static {
        TRADUCTIONS.put("headphones",         List.of("casque", "audio", "headset"));
        TRADUCTIONS.put("headset",             List.of("casque", "audio", "headphones"));
        TRADUCTIONS.put("keyboard",            List.of("clavier", "gaming"));
        TRADUCTIONS.put("computer keyboard",   List.of("clavier", "mecanique", "gaming"));
        TRADUCTIONS.put("mouse",               List.of("souris", "gaming"));
        TRADUCTIONS.put("computer mouse",      List.of("souris", "optique", "gaming"));
        TRADUCTIONS.put("mousepad",            List.of("tapis", "souris", "mousepad"));
        TRADUCTIONS.put("mouse pad",           List.of("tapis", "souris"));
        TRADUCTIONS.put("jersey",              List.of("maillot", "equipe", "esport"));
        TRADUCTIONS.put("sports uniform",      List.of("maillot", "uniforme", "tenue"));
        TRADUCTIONS.put("clothing",            List.of("vetement", "maillot", "tenue"));
        TRADUCTIONS.put("t-shirt",             List.of("tshirt", "maillot", "vetement"));
        TRADUCTIONS.put("chair",               List.of("chaise", "siege", "fauteuil"));
        TRADUCTIONS.put("gaming chair",        List.of("chaise", "gaming", "siege"));
        TRADUCTIONS.put("gamepad",             List.of("manette", "controller", "jeu"));
        TRADUCTIONS.put("game controller",     List.of("manette", "controller"));
        TRADUCTIONS.put("joystick",            List.of("joystick", "manette"));
        TRADUCTIONS.put("microphone",          List.of("micro", "microphone", "audio"));
        TRADUCTIONS.put("monitor",             List.of("ecran", "moniteur", "display"));
        TRADUCTIONS.put("computer monitor",    List.of("ecran", "moniteur", "gaming"));
        TRADUCTIONS.put("display device",      List.of("ecran", "moniteur"));
        TRADUCTIONS.put("webcam",              List.of("webcam", "camera", "streaming"));
        TRADUCTIONS.put("camera",              List.of("camera", "webcam"));
        TRADUCTIONS.put("speaker",             List.of("enceinte", "audio", "son"));
        TRADUCTIONS.put("loudspeaker",         List.of("enceinte", "audio"));
        TRADUCTIONS.put("controller",          List.of("manette", "controller"));
        TRADUCTIONS.put("computer hardware",   List.of("gaming", "peripherique", "pc"));
        TRADUCTIONS.put("peripheral",          List.of("peripherique", "gaming", "pc"));
        TRADUCTIONS.put("electronic device",   List.of("gaming", "electronique", "tech"));
        TRADUCTIONS.put("technology",          List.of("tech", "gaming", "high tech"));
        TRADUCTIONS.put("gadget",              List.of("gaming", "tech", "accessoire"));
        TRADUCTIONS.put("cap",                 List.of("casquette", "hat", "bonnet"));
        TRADUCTIONS.put("hat",                 List.of("casquette", "bonnet"));
        TRADUCTIONS.put("glasses",             List.of("lunettes"));
        TRADUCTIONS.put("eyewear",             List.of("lunettes", "gaming"));
        TRADUCTIONS.put("bag",                 List.of("sac", "sacoche", "backpack"));
        TRADUCTIONS.put("backpack",            List.of("sac", "sacoche", "dos"));
        TRADUCTIONS.put("desk",                List.of("bureau", "desk", "table"));
        TRADUCTIONS.put("computer desk",       List.of("bureau", "gaming", "desk"));
    }

    // -------------------------------------------------------------------------
    // Appel Imagga API
    // -------------------------------------------------------------------------

    public List<String> analyserImage(File imageFile) {
        List<String> labels = new ArrayList<>();
        try {
            System.out.println("[VISION] Analyse: " + imageFile.getName());

            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Credentials Basic Auth
            String credentials = Base64.getEncoder()
                    .encodeToString((IMAGGA_KEY + ":" + IMAGGA_SECRET).getBytes());

            // Corps multipart avec l'image encodée en base64
            String boundary = "----ImaggaBoundary" + System.currentTimeMillis();
            String body = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"image_base64\"\r\n\r\n"
                    + base64Image + "\r\n"
                    + "--" + boundary + "--\r\n";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(IMAGGA_URL))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[VISION] Status: " + response.statusCode());

            if (response.statusCode() == 200) {
                labels = parseLabelsImagga(response.body());
                System.out.println("[VISION] Labels retenus (confidence >= " + SCORE_MIN + "): " + labels);
            } else {
                System.out.println("[VISION] ERREUR API Imagga: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("[VISION] ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
        return labels;
    }

    // -------------------------------------------------------------------------
    // Parsing réponse Imagga
    // Format : {"result":{"tags":[{"confidence":99.5,"tag":{"en":"keyboard","fr":"..."}},...]}}
    // -------------------------------------------------------------------------

    private List<String> parseLabelsImagga(String json) {
        List<String> labels = new ArrayList<>();
        try {
            // Découper sur chaque "en": pour récupérer les labels anglais
            String[] parts = json.split("\"en\":");
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim();
                if (!part.startsWith("\"")) continue;

                // Extraire le label
                int end = part.indexOf("\"", 1);
                if (end < 1) continue;
                String label = part.substring(1, end).toLowerCase().trim();
                if (label.isBlank() || label.length() < 3) continue;

                // La confidence est dans le bloc AVANT "en": (Imagga : confidence -> tag -> en)
                String before = parts[i - 1];
                double confidence = extraireConfidence(before);

                if (confidence < SCORE_MIN) {
                    System.out.println("[VISION] Label ignore (confidence "
                            + String.format("%.1f", confidence) + "): " + label);
                    continue;
                }

                if (!labels.contains(label)) {
                    labels.add(label);
                    System.out.println("[VISION] Label accepte (confidence "
                            + String.format("%.1f", confidence) + "): " + label);
                }
            }
        } catch (Exception e) {
            System.out.println("[VISION] ERREUR parsing Imagga: " + e.getMessage());
            e.printStackTrace();
        }
        return labels;
    }

    /**
     * Cherche "confidence": X.XX dans la fin du fragment précédant le label.
     * Imagga place confidence avant le champ "en" dans chaque objet tag.
     */
    private double extraireConfidence(String fragment) {
        try {
            int idx = fragment.lastIndexOf("\"confidence\":");
            if (idx == -1) return 0.0;
            String apres = fragment.substring(idx + 13).trim();
            StringBuilder nb = new StringBuilder();
            for (char c : apres.toCharArray()) {
                if (Character.isDigit(c) || c == '.') nb.append(c);
                else if (!nb.isEmpty()) break;
            }
            return nb.isEmpty() ? 0.0 : Double.parseDouble(nb.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    // -------------------------------------------------------------------------
    // Matching produits avec scoring pondéré
    // -------------------------------------------------------------------------

    public List<Produit> chercherProduitsParLabels(List<String> labelsOriginaux, List<Produit> tousProduits) {
        if (labelsOriginaux.isEmpty()) return new ArrayList<>();

        List<String> labelsEtendus = enrichirLabels(labelsOriginaux);
        System.out.println("[VISION] Labels etendus pour matching: " + labelsEtendus);

        Map<Produit, Integer> scores = new LinkedHashMap<>();

        for (Produit p : tousProduits) {
            String nomLower  = normaliser(p.getNom());
            String descLower = p.getDescription() != null ? normaliser(p.getDescription()) : "";

            int score = 0;
            for (String label : labelsEtendus) {
                String l = normaliser(label);
                if (l.length() < 3) continue;

                if (nomLower.contains(l))                           score += 3;
                if (descLower.contains(l))                          score += 2;
                if (l.contains(nomLower) && nomLower.length() > 3)  score += 3;
            }

            if (score > 0) {
                scores.put(p, score);
                System.out.println("[VISION] Produit score: " + p.getNom() + " -> " + score + " pts");
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Produit, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> enrichirLabels(List<String> labels) {
        List<String> enrichis = new ArrayList<>(labels);
        for (String label : labels) {
            String lLower = label.toLowerCase();
            TRADUCTIONS.forEach((en, frList) -> {
                if (lLower.contains(en) || en.contains(lLower)) {
                    frList.forEach(fr -> {
                        if (!enrichis.contains(fr)) enrichis.add(fr);
                    });
                }
            });
        }
        return enrichis;
    }

    private String normaliser(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
                .replace("à", "a").replace("â", "a").replace("ä", "a")
                .replace("ù", "u").replace("û", "u").replace("ü", "u")
                .replace("ô", "o").replace("ö", "o")
                .replace("î", "i").replace("ï", "i")
                .replace("ç", "c")
                .replace("-", " ").replace("_", " ")
                .trim();
    }
}