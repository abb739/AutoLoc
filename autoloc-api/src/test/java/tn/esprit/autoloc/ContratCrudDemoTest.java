package tn.esprit.autoloc;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.autoloc.domain.Contrat;
import tn.esprit.autoloc.domain.ModePaiement;
import tn.esprit.autoloc.domain.Paiement;
import tn.esprit.autoloc.repository.IContratRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ContratCrudDemoTest {

    private static final Logger log = LoggerFactory.getLogger(ContratCrudDemoTest.class);

    @Autowired
    private IContratRepository contratRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void crudContrat() {
        step("1. save(contrat) : id null, 2 paiements ajoutes par addPaiement");
        Contrat contrat = contratRepository.save(contrat("100.00", "200.00"));
        sync();
        Long id = contrat.getIdContrat();
        step("   -> id attribue par MySQL : " + id);
        assertThat(id).isNotNull();

        step("2. findById(" + id + ")");
        Contrat lu = contratRepository.findById(id).orElseThrow();
        step("3. getPaiements() : chargement LAZY au premier acces");
        step("   -> " + lu.getPaiements().size() + " paiements");
        assertThat(lu.getPaiements()).hasSize(2);

        step("4. removePaiement(p) puis save(contrat) : orphanRemoval");
        lu.removePaiement(lu.getPaiements().get(0));
        contratRepository.save(lu);
        sync();
        assertThat(contratRepository.findById(id).orElseThrow().getPaiements()).hasSize(1);

        step("5. findById(-1) : Optional vide, present = " + contratRepository.findById(-1L).isPresent());
        assertThat(contratRepository.findById(-1L)).isEmpty();

        step("6. deleteById(" + id + ") : cascade REMOVE sur les paiements");
        contratRepository.deleteById(id);
        sync();
        assertThat(contratRepository.existsById(id)).isFalse();

        step("7. deleteById(-1) : aucune exception (Spring Data 3)");
        contratRepository.deleteById(-1L);
        sync();
        step("Fin : transaction annulee (rollback), aucune donnee conservee en base");
    }

    private void sync() {
        entityManager.flush();
        entityManager.clear();
    }

    private static void step(String message) {
        log.info(">>> {}", message);
    }

    private static Contrat contrat(String... montants) {
        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now());
        contrat.setMontantTotal(new BigDecimal("300.00"));
        contrat.setValide(true);
        for (String montant : montants) {
            Paiement paiement = new Paiement();
            paiement.setMontant(new BigDecimal(montant));
            paiement.setDatePaiement(LocalDate.now());
            paiement.setModePaiement(ModePaiement.CARTE);
            contrat.addPaiement(paiement);
        }
        return contrat;
    }
}
