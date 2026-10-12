package tn.esprit.autoloc.service;

import java.util.List;
import tn.esprit.autoloc.domain.Paiement;

/**
 * Lecture seule : un paiement est créé, modifié ou supprimé à travers son Contrat
 * (composition, cascade ALL + orphanRemoval).
 */
public interface IPaiementService {

    Paiement findById(Long id);

    List<Paiement> findAll();
}
