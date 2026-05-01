package com.esports.service;

import java.util.*;

/**
 * Service de prédiction de prix par régression linéaire multiple.
 * Entraîné en Java pur — aucune dépendance externe.
 * Gradient descent : 15 000 epochs, learning rate 0.001
 */
public class PrixPredicteurService {

    // ══════════════════════════════════════════════════════════════════════════
    //  MODÈLE INTERNE
    // ══════════════════════════════════════════════════════════════════════════

    private static class Modele {
        double[] weights;
        double   bias;
        Modele(int nbFeatures) {
            weights = new double[nbFeatures];
            bias    = 0;
        }
    }

    private static final int    EPOCHS = 15000;
    private static final double LR     = 0.001;

    // ══════════════════════════════════════════════════════════════════════════
    //  MAPS DE SCORES
    // ══════════════════════════════════════════════════════════════════════════

    private static final Map<String, Double> GPU_SCORES = new LinkedHashMap<>();
    private static final Map<String, Double> CPU_SCORES = new LinkedHashMap<>();
    private static final Map<String, Double> MARQUE_SCORES = new LinkedHashMap<>();

    static {
        // GPU
        GPU_SCORES.put("RTX 4090",      10.0);
        GPU_SCORES.put("RTX 4080",       9.5);
        GPU_SCORES.put("RTX 4070 Ti",    9.0);
        GPU_SCORES.put("RTX 4070",       8.5);
        GPU_SCORES.put("RTX 4060 Ti",    8.0);
        GPU_SCORES.put("RTX 4060",       7.5);
        GPU_SCORES.put("RTX 3090 Ti",    9.2);
        GPU_SCORES.put("RTX 3090",       9.0);
        GPU_SCORES.put("RTX 3080 Ti",    8.8);
        GPU_SCORES.put("RTX 3080",       8.5);
        GPU_SCORES.put("RTX 3070 Ti",    8.0);
        GPU_SCORES.put("RTX 3070",       7.5);
        GPU_SCORES.put("RTX 3060 Ti",    7.0);
        GPU_SCORES.put("RTX 3060",       6.5);
        GPU_SCORES.put("RTX 3050",       6.0);
        GPU_SCORES.put("RX 7900 XTX",    9.5);
        GPU_SCORES.put("RX 7900 XT",     9.0);
        GPU_SCORES.put("RX 7800 XT",     8.0);
        GPU_SCORES.put("RX 7700 XT",     7.5);
        GPU_SCORES.put("RX 7600",        7.0);
        GPU_SCORES.put("RX 6800 XT",     8.5);
        GPU_SCORES.put("RX 6700 XT",     7.5);
        GPU_SCORES.put("RX 6600",        6.5);
        GPU_SCORES.put("GTX 1660 Ti",    5.5);
        GPU_SCORES.put("GTX 1650",       4.0);
        GPU_SCORES.put("Intégré",        2.0);

        // CPU
        CPU_SCORES.put("i9-14900K",      10.0);
        CPU_SCORES.put("i9-13900K",       9.8);
        CPU_SCORES.put("i7-14700K",       9.0);
        CPU_SCORES.put("i7-13700K",       8.8);
        CPU_SCORES.put("i7-12700K",       8.5);
        CPU_SCORES.put("i5-13600K",       8.0);
        CPU_SCORES.put("i5-12600K",       7.5);
        CPU_SCORES.put("i5-12400",        7.0);
        CPU_SCORES.put("i3-12100",        4.5);
        CPU_SCORES.put("Ryzen 9 7950X",  10.0);
        CPU_SCORES.put("Ryzen 9 7900X",   9.5);
        CPU_SCORES.put("Ryzen 9 5900X",   9.0);
        CPU_SCORES.put("Ryzen 7 7700X",   8.5);
        CPU_SCORES.put("Ryzen 7 5800X",   8.0);
        CPU_SCORES.put("Ryzen 7 5700X",   7.5);
        CPU_SCORES.put("Ryzen 5 7600X",   7.5);
        CPU_SCORES.put("Ryzen 5 5600X",   7.0);
        CPU_SCORES.put("Ryzen 5 5600",    6.5);
        CPU_SCORES.put("Ryzen 3 4100",    4.0);

        // MARQUES
        MARQUE_SCORES.put("Razer",        9.5);
        MARQUE_SCORES.put("Alienware",    9.5);
        MARQUE_SCORES.put("Logitech",     9.0);
        MARQUE_SCORES.put("ASUS ROG",     9.0);
        MARQUE_SCORES.put("Secretlab",    9.0);
        MARQUE_SCORES.put("SteelSeries",  8.5);
        MARQUE_SCORES.put("Corsair",      8.5);
        MARQUE_SCORES.put("HyperX",       8.0);
        MARQUE_SCORES.put("MSI",          8.0);
        MARQUE_SCORES.put("Gigabyte",     7.5);
        MARQUE_SCORES.put("Cooler Master",7.5);
        MARQUE_SCORES.put("ASUS",         7.5);
        MARQUE_SCORES.put("Acer",         7.0);
        MARQUE_SCORES.put("HP",           6.5);
        MARQUE_SCORES.put("Dell",         6.5);
        MARQUE_SCORES.put("Lenovo",       6.5);
        MARQUE_SCORES.put("DXRacer",      7.0);
        MARQUE_SCORES.put("Herman Miller",9.0);
        MARQUE_SCORES.put("AndaSeat",     7.0);
        MARQUE_SCORES.put("IKEA",         5.0);
        MARQUE_SCORES.put("Xbox",         8.0);
        MARQUE_SCORES.put("PlayStation",  8.5);
        MARQUE_SCORES.put("8BitDo",       7.0);
        MARQUE_SCORES.put("NoName",       3.0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATASETS — features → prix DT
    // ══════════════════════════════════════════════════════════════════════════

    // PC Gaming : {gpu_score, ram_go, cpu_score, stockage_to, ecran_score}
    private static final double[][] PC_X = {
            {10, 64, 10,  4, 4}, {9.5, 32, 9.8, 2, 3}, {9,  32,  9,  2, 2},
            {8.5,32,  8.8,2, 2}, {8,  16,  8.5, 1, 1},  {7.5,16, 8,  1, 1},
            {7,  16,  7.5,1, 0}, {6.5,16,  7,   1, 0},  {6,  8,  6.5,0.5,0},
            {5.5, 8,  6,  0.5,0},{4,   8,  4.5, 0.5,0}, {3,   4,  4,  0.5,0},
            {2,   4,  4,  0.5,0},{9.2, 64, 9,   4, 4},  {8.8, 32, 8.8,2, 3}
    };
    private static final double[] PC_Y = {
            22000,16000,12000,10000,7000,5500,4500,3500,2800,2200,1800,1400,1000,20000,14000
    };

    // Clavier : {switch_score, rgb, sans_fil, taille(1-5), marque_score}
    private static final double[][] CLAVIER_X = {
            {9.5,1,1,5,9.5},{9,1,1,4,9},{8.5,1,1,4,8.5},{8,1,0,4,8.5},
            {7.5,1,1,3,8},  {7,1,0,3,9},{6.5,1,0,3,7.5},{6,0,0,2,7},
            {5,0,0,2,6.5},  {4,0,0,1,5},{9,1,1,5,9.5},  {8,1,1,4,9}
    };
    private static final double[] CLAVIER_Y = {
            700,580,480,420,320,280,220,160,120,80,650,500
    };

    // Souris : {dpi_score, rgb, sans_fil, poids(1-5), marque_score}
    private static final double[][] SOURIS_X = {
            {10,1,1,2,9.5},{9,1,1,2,9},{8.5,1,1,3,8.5},{8,1,0,3,8.5},
            {7.5,1,1,3,8}, {7,1,0,2,9},{6.5,1,0,3,7.5},{6,0,0,3,7},
            {5,0,0,4,6.5}, {4,0,0,4,5},{9,1,1,2,9.5},  {8,1,1,3,9}
    };
    private static final double[] SOURIS_Y = {
            600,480,380,320,260,220,180,140,100,50,550,420
    };

    // Casque : {son_score, micro, sans_fil, surround, marque_score}
    private static final double[][] CASQUE_X = {
            {10,1,1,1,9.5},{9,1,1,1,9},{8.5,1,1,1,8.5},{8,1,0,0,8.5},
            {7.5,1,1,1,8}, {7,1,0,0,9},{6.5,1,0,0,7.5},{6,0,0,0,7},
            {5,0,0,0,6.5}, {4,0,0,0,5},{9,1,1,1,9.5},  {8,1,1,0,9}
    };
    private static final double[] CASQUE_Y = {
            800,650,520,440,360,300,240,180,130,80,720,560
    };

    // Écran : {taille_score, resolution_score, hz_score, dalle_score, marque_score}
    // resolution: 1080p=1,1440p=2,4K=3,5K=4,8K=5
    // dalle: TN=1,VA=2,IPS=3,OLED=4,Mini-LED=5
    private static final double[][] ECRAN_X = {
            {9,5,9,4,9},{8.5,4,8,4,9},{8,3,8,3,8.5},{7.5,3,7,3,8},
            {7,2,6,3,8},{6.5,2,5,2,7.5},{6,1,5,2,7},{5,1,4,1,6.5},
            {8,4,9,5,9.5},{7,3,6,4,8.5},{6,2,5,3,7},{5,1,4,2,6}
    };
    private static final double[] ECRAN_Y = {
            5000,3800,2800,2200,1800,1400,1100,800,4500,2600,1300,700
    };

    // Chaise : {confort, materiau, rgb, inclinaison, marque_score}
    private static final double[][] CHAISE_X = {
            {10,10,1,10,9},{9,9,1,9,9},{8.5,8,0,8,8.5},{8,8,1,8,8.5},
            {7.5,7,0,7,8},{7,7,1,7,7},{6.5,6,0,6,7},{6,5,0,5,5},
            {5,5,0,5,6.5},{4,4,0,4,5},{9,9,1,9,9.5},{8,8,0,8,9}
    };
    private static final double[] CHAISE_Y = {
            6000,4500,3200,2800,2200,1800,1400,900,700,500,5500,3800
    };

    // Maillot : {qualite, taille_score(XS=1..XXL=6), edition_score(1-4), marque_score}
    private static final double[][] MAILLOT_X = {
            {10,4,4,9.5},{9,4,3,9},{8.5,4,2,8.5},{8,3,2,8},
            {7.5,3,1,7.5},{7,3,1,7},{6.5,2,1,6.5},{6,2,1,5},
            {5,2,1,4},{4,1,1,3},{9,4,4,9.5},{8,3,2,8.5}
    };
    private static final double[] MAILLOT_Y = {
            700,550,420,340,280,220,180,140,100,60,650,380
    };

    // Tapis : {taille_score, epaisseur, rgb, materiau}
    private static final double[][] TAPIS_X = {
            {10,9,1,9},{9,8,1,8.5},{8.5,7,1,8},{8,7,0,7.5},
            {7.5,6,1,7},{7,6,0,6.5},{6,5,0,6},{5,4,0,5},
            {4,3,0,4},{3,2,0,3},{9,8,1,9},{8,7,1,8}
    };
    private static final double[] TAPIS_Y = {
            600,480,380,300,240,190,150,110,70,30,520,340
    };

    // Manette : {confort, sans_fil, vibration, compatibilite(1-5), marque_score}
    private static final double[][] MANETTE_X = {
            {10,1,1,5,9.5},{9,1,1,5,9},{8.5,1,1,4,8.5},{8,1,1,3,8.5},
            {7.5,1,1,3,8},{7,0,1,2,8.5},{6.5,0,1,2,7.5},{6,0,0,1,7},
            {5,0,0,1,6.5},{4,0,0,1,5},{9,1,1,4,9.5},{8,1,1,3,9}
    };
    private static final double[] MANETTE_Y = {
            800,650,520,440,360,300,240,180,150,80,720,560
    };

    // ══════════════════════════════════════════════════════════════════════════
    //  MODÈLES ENTRAÎNÉS (lazy init au premier appel)
    // ══════════════════════════════════════════════════════════════════════════

    private Modele modelePc;
    private Modele modeleClavier;
    private Modele modeleSouris;
    private Modele modeleCasque;
    private Modele modeleEcran;
    private Modele modeleChaise;
    private Modele modeleMaillot;
    private Modele modeleTapis;
    private Modele modeleManette;

    public PrixPredicteurService() {
        // Entraînement de tous les modèles au démarrage
        modelePc      = entrainer(PC_X,      PC_Y);
        modeleClavier = entrainer(CLAVIER_X, CLAVIER_Y);
        modeleSouris  = entrainer(SOURIS_X,  SOURIS_Y);
        modeleCasque  = entrainer(CASQUE_X,  CASQUE_Y);
        modeleEcran   = entrainer(ECRAN_X,   ECRAN_Y);
        modeleChaise  = entrainer(CHAISE_X,  CHAISE_Y);
        modeleMaillot = entrainer(MAILLOT_X, MAILLOT_Y);
        modeleTapis   = entrainer(TAPIS_X,   TAPIS_Y);
        modeleManette = entrainer(MANETTE_X, MANETTE_Y);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GRADIENT DESCENT
    // ══════════════════════════════════════════════════════════════════════════

    private Modele entrainer(double[][] X, double[] Y) {
        int n = X.length;
        int f = X[0].length;
        Modele m = new Modele(f);

        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            double[] dw = new double[f];
            double   db = 0;
            for (int i = 0; i < n; i++) {
                double pred = predire(X[i], m);
                double err  = pred - Y[i];
                for (int j = 0; j < f; j++) dw[j] += err * X[i][j];
                db += err;
            }
            for (int j = 0; j < f; j++) m.weights[j] -= LR * dw[j] / n;
            m.bias -= LR * db / n;
        }
        return m;
    }

    private double predire(double[] x, Modele m) {
        double result = m.bias;
        for (int i = 0; i < x.length; i++) result += m.weights[i] * x[i];
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ARRONDIS INTELLIGENTS
    // ══════════════════════════════════════════════════════════════════════════

    private double arrondiPc(double prix) {
        double r = Math.round(prix / 500.0) * 500.0;
        return Math.max(r, 1000);
    }

    private double arrondi50(double prix) {
        return Math.round(prix / 50.0) * 50.0;
    }

    private double arrondi20(double prix) {
        return Math.round(prix / 20.0) * 20.0;
    }

    private double arrondi10(double prix) {
        return Math.round(prix / 10.0) * 10.0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS — résolution / dalle → score
    // ══════════════════════════════════════════════════════════════════════════

    private double resolutionScore(String resolution) {
        return switch (resolution) {
            case "1080p" -> 1;
            case "1440p" -> 2;
            case "4K"    -> 3;
            case "5K"    -> 4;
            case "8K"    -> 5;
            default      -> 2;
        };
    }

    private double dalleScore(String dalle) {
        return switch (dalle) {
            case "TN"      -> 1;
            case "VA"      -> 2;
            case "IPS"     -> 3;
            case "OLED"    -> 4;
            case "Mini-LED"-> 5;
            default        -> 3;
        };
    }

    private double tailleScore(String taille) {
        return switch (taille) {
            case "XS"  -> 1;
            case "S"   -> 2;
            case "M"   -> 3;
            case "L"   -> 4;
            case "XL"  -> 5;
            case "XXL" -> 6;
            default    -> 3;
        };
    }

    private double editionScore(String edition) {
        return switch (edition) {
            case "Standard"  -> 1;
            case "Collector" -> 2;
            case "Limited"   -> 3;
            case "Signed"    -> 4;
            default          -> 1;
        };
    }

    private double gpuScore(String gpu) {
        return GPU_SCORES.getOrDefault(gpu, 5.0);
    }

    private double cpuScore(String cpu) {
        return CPU_SCORES.getOrDefault(cpu, 5.0);
    }

    private double marqueScore(String marque) {
        return MARQUE_SCORES.getOrDefault(marque, 5.0);
    }

    private double ecranTypeScore(String type) {
        return switch (type) {
            case "Non inclus" -> 0;
            case "FHD"        -> 1;
            case "QHD"        -> 2;
            case "4K OLED"    -> 3;
            case "4K Mini-LED"-> 4;
            default           -> 0;
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  API PUBLIQUE — méthodes de prédiction
    // ══════════════════════════════════════════════════════════════════════════

    public double predirePcGaming(String gpu, double ram, String cpu,
                                  double stockage, String typeEcran) {
        double[] x = {gpuScore(gpu), ram, cpuScore(cpu), stockage, ecranTypeScore(typeEcran)};
        return arrondiPc(predire(x, modelePc));
    }

    public double predireClavier(double switchScore, double rgb, double sansFil,
                                 double taille, String marque) {
        double[] x = {switchScore, rgb, sansFil, taille, marqueScore(marque)};
        return arrondi20(Math.max(predire(x, modeleClavier), 50));
    }

    public double predireSouris(double dpiScore, double rgb, double sansFil,
                                double poids, String marque) {
        double[] x = {dpiScore, rgb, sansFil, poids, marqueScore(marque)};
        return arrondi10(Math.max(predire(x, modeleSouris), 30));
    }

    public double predireCasque(double sonScore, double micro, double sansFil,
                                double surround, String marque) {
        double[] x = {sonScore, micro, sansFil, surround, marqueScore(marque)};
        return arrondi20(Math.max(predire(x, modeleCasque), 50));
    }

    public double predireEcran(double tailleScore, String resolution, double hzScore,
                               String dalle, String marque) {
        double[] x = {tailleScore, resolutionScore(resolution), hzScore,
                dalleScore(dalle), marqueScore(marque)};
        return arrondi50(Math.max(predire(x, modeleEcran), 200));
    }

    public double predireChaise(double confort, double materiau, double rgb,
                                double inclinaison, String marque) {
        double[] x = {confort, materiau, rgb, inclinaison, marqueScore(marque)};
        return arrondi50(Math.max(predire(x, modeleChaise), 300));
    }

    public double predireMaillot(double qualite, String taille, String edition, String marque) {
        double[] x = {qualite, tailleScore(taille), editionScore(edition), marqueScore(marque)};
        return arrondi10(Math.max(predire(x, modeleMaillot), 30));
    }

    public double predireTapis(double tailleScore, double epaisseur, double rgb, double materiau) {
        double[] x = {tailleScore, epaisseur, rgb, materiau};
        return arrondi10(Math.max(predire(x, modeleTapis), 20));
    }

    public double predireManette(double confort, double sansFil, double vibration,
                                 double compatibilite, String marque) {
        double[] x = {confort, sansFil, vibration, compatibilite, marqueScore(marque)};
        return arrondi10(Math.max(predire(x, modeleManette), 50));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LISTES POUR LES COMBOS
    // ══════════════════════════════════════════════════════════════════════════

    public List<String> getGpuList() {
        return new ArrayList<>(GPU_SCORES.keySet());
    }

    public List<String> getCpuList() {
        return new ArrayList<>(CPU_SCORES.keySet());
    }

    public List<String> getMarqueList() {
        return new ArrayList<>(MARQUE_SCORES.keySet());
    }
}