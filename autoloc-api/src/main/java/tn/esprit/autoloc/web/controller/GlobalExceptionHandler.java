package tn.esprit.autoloc.web.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tn.esprit.autoloc.exception.ResourceNotFoundException;

/**
 * Traduit les exceptions des services en réponses HTTP (corps JSON : statut, message, horodatage).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record Erreur(int statut, String message, LocalDateTime horodatage) {
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Erreur> handleNotFound(ResourceNotFoundException ex) {
        return reponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Erreur> handleBadRequest(IllegalArgumentException ex) {
        return reponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Contrainte d'unicité ou clé étrangère violée (email déjà utilisé, agence encore référencée…)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erreur> handleConflict(DataIntegrityViolationException ex) {
        return reponse(HttpStatus.CONFLICT, "La donnée envoyée viole une contrainte d'intégrité");
    }

    private static ResponseEntity<Erreur> reponse(HttpStatus statut, String message) {
        // Fuseau explicite (SonarQube S8688) : celui du serveur
        Erreur erreur = new Erreur(statut.value(), message, LocalDateTime.now(ZoneId.systemDefault()));
        return ResponseEntity.status(statut).body(erreur);
    }
}
