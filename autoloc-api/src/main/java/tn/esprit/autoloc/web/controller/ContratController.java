package tn.esprit.autoloc.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.esprit.autoloc.domain.Contrat;
import tn.esprit.autoloc.service.IContratService;

@Tag(name = "Contrats", description = "Gestion des contrats de location (et de leurs paiements par cascade)")
@RestController
@RequestMapping("/contrats")
@RequiredArgsConstructor
public class ContratController {

    private final IContratService contratService;

    @Operation(summary = "Lister les contrats")
    @ApiResponse(responseCode = "200", description = "Liste des contrats")
    @GetMapping
    public List<Contrat> findAll() {
        return contratService.findAll();
    }

    @Operation(summary = "Consulter un contrat")
    @ApiResponse(responseCode = "200", description = "Contrat trouvé")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @GetMapping("/{id}")
    public Contrat findById(@Parameter(description = "Identifiant") @PathVariable Long id) {
        return contratService.findById(id);
    }

    @Operation(summary = "Créer un contrat")
    @ApiResponse(responseCode = "201", description = "Contrat créé")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Contrat create(@RequestBody Contrat contrat) {
        return contratService.create(contrat);
    }

    @Operation(summary = "Modifier un contrat")
    @ApiResponse(responseCode = "200", description = "Contrat modifié")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @PutMapping("/{id}")
    public Contrat update(@Parameter(description = "Identifiant") @PathVariable Long id,
                          @RequestBody Contrat contrat) {
        return contratService.update(id, contrat);
    }

    @Operation(summary = "Supprimer un contrat")
    @ApiResponse(responseCode = "204", description = "Contrat supprimé")
    @ApiResponse(responseCode = "404", description = "Contrat introuvable")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@Parameter(description = "Identifiant") @PathVariable Long id) {
        contratService.deleteById(id);
    }
}
