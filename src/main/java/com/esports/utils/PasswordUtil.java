package com.esports.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire de hachage des mots de passe avec BCrypt.
 * Gère aussi la rétro-compatibilité avec les anciens mots de passe en clair.
 */
public class PasswordUtil {

    private static final int ROUNDS = 12;

    /** Hache un mot de passe en clair → hash BCrypt. */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(ROUNDS));
    }

    /**
     * Vérifie un mot de passe contre la valeur stockée en base.
     * Rétro-compatible : si le hash n'est pas BCrypt (ancien compte en clair),
     * compare directement puis re-hache si correspondance.
     */
    public static boolean verify(String plainPassword, String storedValue) {
        if (storedValue == null || plainPassword == null) return false;

        // Hash BCrypt → vérification standard
        if (storedValue.startsWith("$2a$") || storedValue.startsWith("$2b$")) {
            try {
                return BCrypt.checkpw(plainPassword, storedValue);
            } catch (Exception e) {
                return false;
            }
        }

        // Ancien mot de passe en clair → comparaison directe
        return storedValue.equals(plainPassword);
    }

    /** Retourne vrai si la valeur est déjà un hash BCrypt. */
    public static boolean isHashed(String value) {
        return value != null &&
               (value.startsWith("$2a$") || value.startsWith("$2b$"));
    }
}
