# Stratégie de fetch et de cascade — AutoLoc

Note technique — Atelier 2 (Associations JPA : cascade et fetch)

## Principes retenus

- **Tout en LAZY.** Les `@ManyToOne` et `@OneToOne` (EAGER par défaut) sont forcés en `LAZY` ; les `@OneToMany` et `@ManyToMany` (LAZY par défaut) le restent. Charger une entité ne déclenche donc qu'une requête sur sa propre table ; les objets liés ne sont chargés que si le code y accède.
- **Cascade uniquement pour la composition.** Une opération n'est propagée que si l'enfant ne peut pas exister sans son parent ou doit être enregistré avec lui. Aucune cascade n'est posée côté `@ManyToOne` : un enfant ne gère jamais le cycle de vie de son parent.
- **Côté propriétaire = côté qui porte la clé étrangère.** C'est toujours le `@ManyToOne` ; le côté `@OneToMany` utilise `mappedBy` et ne crée aucune colonne.
- **Pas de `@Data`.** Les entités utilisent `@Getter`/`@Setter` ciblés, pour éviter un `toString`/`hashCode` qui parcourrait les deux côtés d'une association bidirectionnelle (StackOverflowError).
- **Méthodes utilitaires.** Les associations bidirectionnelles avec cascade exposent des méthodes (`addPaiement`, `removePaiement`, `addReservation`, `addMaintenance`, `addEquipement`, `removeEquipement`, `setContrat`) qui mettent à jour les deux côtés en même temps. Sans cela, l'objet Java et la base peuvent diverger : seul le côté propriétaire est lu par Hibernate pour écrire la clé étrangère.

## Tableau des associations

| Association | Mapping (côté propriétaire) | Fetch | Cascade | Justification |
|---|---|---|---|---|
| Contrat → Paiement | `@OneToMany(mappedBy = "contrat")` / `@ManyToOne` dans **Paiement** | LAZY | ALL + orphanRemoval | Un paiement n'existe que rattaché à son contrat (composition). Enregistrer le contrat enregistre ses paiements, supprimer le contrat supprime ses paiements, et retirer un paiement de la liste le supprime en base. |
| Agence → Vehicule | `@OneToMany(mappedBy = "agence")` / `@ManyToOne` dans **Vehicule** | LAZY | Aucune | Un véhicule survit à son agence : il peut être transféré vers une autre agence. Une cascade REMOVE ferait disparaître une partie du parc avec l'agence. |
| Agence → Employe | `@OneToMany(mappedBy = "agence")` / `@ManyToOne` dans **Employe** | LAZY | Aucune | Un employé peut être muté. Son dossier (et son historique d'activité) ne doit pas dépendre de l'existence d'une agence. |
| Vehicule ↔ Equipement | `@ManyToMany` dans **Vehicule** + `@JoinTable(vehicule_equipement)` ; `mappedBy = "equipements"` dans Equipement | LAZY | Aucune | Les équipements (GPS, siège bébé…) forment un catalogue partagé entre véhicules. REMOVE supprimerait un équipement encore utilisé par d'autres véhicules ; PERSIST est inutile car le catalogue est géré séparément. Utilisation d'un `Set` : pas de doublon, et Hibernate peut supprimer une seule ligne de la table de jointure (avec une `List`, il supprime toutes les lignes du véhicule puis les réinsère à chaque modification). |
| Client → Reservation | `@OneToMany(mappedBy = "client")` / `@ManyToOne` dans **Reservation** | LAZY | PERSIST | Au comptoir, un nouveau client et sa première réservation peuvent être enregistrés en un seul `save(client)`. Pas de REMOVE ni d'orphanRemoval : l'historique des réservations (et les contrats/paiements qui en dépendent) doit être conservé. |
| Reservation → Vehicule | `@ManyToOne` unidirectionnel dans **Reservation** | LAZY | Aucune | Le véhicule est une ressource du parc, indépendante de chaque réservation. L'association reste unidirectionnelle : la liste de toutes les réservations d'un véhicule grossirait sans fin et sera obtenue par une requête de repository si besoin. |
| Reservation ↔ Contrat | `@OneToOne` dans **Contrat** ; `mappedBy = "reservation"` dans Reservation | LAZY | ALL côté Reservation | Le contrat est la concrétisation d'une réservation et suit son cycle de vie : enregistrer la réservation enregistre son contrat, la supprimer supprime le contrat (puis ses paiements, par la cascade Contrat → Paiement). |
| Vehicule → Maintenance | `@OneToMany(mappedBy = "vehicule")` / `@ManyToOne` dans **Maintenance** | LAZY | PERSIST | Une maintenance est créée à partir du véhicule et enregistrée avec lui. Pas de REMOVE : l'historique d'entretien sert à la traçabilité (sécurité, garantie, revente). |

## Schéma généré attendu

Pour les `@ManyToOne` et le `@OneToOne`, aucun `@JoinColumn` n'est utilisé : Hibernate nomme la colonne `<attribut>_<clé primaire référencée>`, par exemple `contrat_id_contrat`. La notation `id_xxx` du sujet correspond à ces colonnes ; `@JoinColumn(name = "id_xxx")` permettrait d'obtenir le nom court.

| Table | Colonne de clé étrangère | Référence |
|---|---|---|
| `paiement` | `contrat_id_contrat` | `contrat(id_contrat)` |
| `vehicule` | `agence_id_agence` | `agence(id_agence)` |
| `employe` | `agence_id_agence` | `agence(id_agence)` |
| `reservation` | `client_id_client` | `client(id_client)` |
| `reservation` | `vehicule_id_vehicule` | `vehicule(id_vehicule)` |
| `contrat` | `reservation_id_reservation` (UNIQUE) | `reservation(id_reservation)` |
| `maintenance` | `vehicule_id_vehicule` | `vehicule(id_vehicule)` |
| `vehicule_equipement` | `id_vehicule` + `id_equipement` (clé primaire composée) | `vehicule`, `equipement` |

- Les côtés inverses (`mappedBy`) n'ajoutent aucune colonne : les tables `agence`, `client` et `equipement` sont inchangées.
- Pour la table de jointure, les colonnes sont nommées explicitement dans `@JoinTable` ; sans cela, Hibernate dérive des noms à partir des attributs (ex. `equipements_id_equipement`), peu lisibles.
- Hibernate pose une contrainte `UNIQUE` sur `contrat.reservation_id_reservation`, ce qui garantit en base le « un contrat par réservation ».

## Conséquences sur la suppression

Les clés étrangères protègent les données là où aucune cascade n'est posée :

- **Supprimer une agence** qui a encore des véhicules ou des employés est refusé par MySQL. Il faut d'abord les réaffecter.
- **Supprimer un véhicule** qui a des maintenances ou des réservations est refusé. Dans AutoLoc, un véhicule se retire du parc (changement de statut) plutôt que de se supprimer.
- **Supprimer un client** qui a des réservations est refusé : l'historique est conservé.
- **Supprimer une réservation** supprime son contrat et les paiements du contrat (ALL en chaîne). En pratique, une réservation ne sera pas supprimée mais passée au statut `ANNULEE` ou `TERMINEE`.

## LAZY ou EAGER (étape 5)

| Fetch | Comportement | Conséquence |
|---|---|---|
| **LAZY (retenu)** | Les paiements ne sont chargés qu'au premier appel à `contrat.getPaiements()`. | Lire un contrat reste une requête légère. Un accès hors transaction lève une `LazyInitializationException`, à traiter par une requête dédiée (Atelier 7). |
| EAGER | Les paiements sont chargés avec chaque contrat, même s'ils ne sont pas utilisés. | Accès plus simple, mais chaque lecture de contrat devient plus lourde ; sur une liste de contrats, on risque le problème N+1. |

**Limite connue du `@OneToOne` LAZY côté inverse.** Sur `Reservation.contrat` (côté `mappedBy`), Hibernate ne peut pas savoir sans interroger la table `contrat` si la réservation a un contrat ou non. Sans bytecode enhancement, il charge donc le contrat dès que la réservation est chargée, malgré `LAZY`. Le LAZY reste effectif côté propriétaire (`Contrat.reservation`). C'est acceptable ici : un contrat est un objet léger et la relation est un-à-un.
