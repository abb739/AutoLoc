package tn.esprit.autoloc.service;

/**
 * Contrôles communs aux services. La validation déclarative (@NotBlank…) viendra au Chapitre 10.
 */
final class ValidationUtils {

    private static final String CHAMP_OBLIGATOIRE = "Le champ « %s » est obligatoire";

    private ValidationUtils() {
    }

    static void exigerTexte(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(String.format(CHAMP_OBLIGATOIRE, champ));
        }
    }

    static void exigerValeur(Object valeur, String champ) {
        if (valeur == null) {
            throw new IllegalArgumentException(String.format(CHAMP_OBLIGATOIRE, champ));
        }
    }
}
