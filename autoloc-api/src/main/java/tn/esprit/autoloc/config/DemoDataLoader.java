package tn.esprit.autoloc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tn.esprit.autoloc.domain.CategorieVehicule;
import tn.esprit.autoloc.domain.StatutVehicule;
import tn.esprit.autoloc.domain.Vehicule;
import tn.esprit.autoloc.repository.VehiculeRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Insère quelques véhicules de démonstration au démarrage, uniquement avec le profil "dev"
 * et seulement si la table est vide (ddl-auto=update conserve les données entre deux lancements).
 */
@Component
@Profile("dev")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final VehiculeRepository vehiculeRepository;

    public DemoDataLoader(VehiculeRepository vehiculeRepository) {
        this.vehiculeRepository = vehiculeRepository;
    }

    @Override
    public void run(String... args) {
        if (vehiculeRepository.count() > 0) {
            log.info("Données de démonstration déjà présentes, aucune insertion");
            return;
        }

        List<Vehicule> vehicules = List.of(
                vehicule("123 TU 4567", "Peugeot", "208", CategorieVehicule.CITADINE, "85.00"),
                vehicule("201 TU 1350", "Volkswagen", "Passat", CategorieVehicule.BERLINE, "140.00"),
                vehicule("215 TU 9012", "Kia", "Sportage", CategorieVehicule.SUV, "180.00"));

        vehiculeRepository.saveAll(vehicules);
        log.info("{} véhicules de démonstration insérés", vehicules.size());
    }

    private static Vehicule vehicule(String immatriculation, String marque, String modele,
                                     CategorieVehicule categorie, String tarifJournalier) {
        Vehicule vehicule = new Vehicule();
        vehicule.setImmatriculation(immatriculation);
        vehicule.setMarque(marque);
        vehicule.setModele(modele);
        vehicule.setCategorie(categorie);
        vehicule.setTarifJournalier(new BigDecimal(tarifJournalier));
        vehicule.setStatut(StatutVehicule.DISPONIBLE);
        return vehicule;
    }
}
