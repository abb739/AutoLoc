package tn.esprit.autoloc.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.esprit.autoloc.domain.Reservation;
import tn.esprit.autoloc.service.IReservationService;

@Tag(name = "Réservations", description = "Gestion des réservations")
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final IReservationService reservationService;

    @Operation(summary = "Lister les reservations")
    @ApiResponse(responseCode = "200", description = "Liste des reservations")
    @GetMapping
    public List<Reservation> findAll() {
        return reservationService.findAll();
    }

    @Operation(summary = "Consulter une réservation")
    @ApiResponse(responseCode = "200", description = "Réservation trouvée")
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    @GetMapping("/{id}")
    public Reservation findById(@Parameter(description = "Identifiant") @PathVariable Long id) {
        return reservationService.findById(id);
    }

    @Operation(summary = "Créer une réservation")
    @ApiResponse(responseCode = "201", description = "Réservation créée")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation create(@RequestBody Reservation reservation) {
        return reservationService.create(reservation);
    }

    @Operation(summary = "Modifier une réservation")
    @ApiResponse(responseCode = "200", description = "Réservation modifiée")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    @PutMapping("/{id}")
    public Reservation update(@Parameter(description = "Identifiant") @PathVariable Long id,
                              @RequestBody Reservation reservation) {
        return reservationService.update(id, reservation);
    }

    @Operation(summary = "Supprimer une réservation")
    @ApiResponse(responseCode = "204", description = "Réservation supprimée")
    @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@Parameter(description = "Identifiant") @PathVariable Long id) {
        reservationService.deleteById(id);
    }
}
