package tn.esprit.autoloc.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReservation;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReservation statut;

    // Côté propriétaire : clé étrangère client_id_client
    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    // Unidirectionnelle : clé étrangère vehicule_id_vehicule
    @ManyToOne(fetch = FetchType.LAZY)
    private Vehicule vehicule;

    // Côté inverse du @OneToOne : le contrat suit le cycle de vie de la réservation
    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Contrat contrat;

    // Setter personnalisé (remplace celui de Lombok) pour garder les deux côtés synchronisés
    public void setContrat(Contrat contrat) {
        this.contrat = contrat;
        if (contrat != null) {
            contrat.setReservation(this);
        }
    }
}
