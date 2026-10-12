# Couche Service, IoC et injection de dépendances — AutoLoc

Note technique — Atelier 4

## A. Modes d'injection comparés

Expérience de l'étape 2 sur `ContratServiceImpl` : injection par constructeur (`@RequiredArgsConstructor` + champ `final`) puis, temporairement, injection par attribut (`@Autowired` sur le champ).

| Critère | Injection par constructeur | Injection par attribut |
|---|---|---|
| Champ `final` possible ? | Oui : le champ est affecté une seule fois, dans le constructeur. | Non : Spring affecte le champ après la construction, il ne peut pas être `final`. |
| Dépendance visible dans la signature de la classe ? | Oui : elle apparaît dans le constructeur `ContratServiceImpl(IContratRepository)`. | Non : elle est cachée dans un champ privé, invisible depuis l'extérieur. |
| Classe utilisable avec `new` hors conteneur sans dépendance `null` ? | Oui : le compilateur oblige à fournir le repository ; l'objet est toujours complet. | Non : `new ContratServiceImpl()` compile, mais le champ reste `null` et le premier appel lève une `NullPointerException`. |
| Signalé par SonarQube for IDE ? | Non. | Oui : règle **S6813**, « Remove this field injection and use constructor injection instead ». |

**Conclusion :** tous les services AutoLoc utilisent l'injection par constructeur. Le service ne peut jamais exister dans un état incomplet, ses dépendances sont explicites et il reste testable sans Spring.

## B. Services

Chaque service est annoté `@Service` et `@RequiredArgsConstructor`. Il reçoit son repository par constructeur, dans un champ `final` typé par l'interface. Aucun attribut modifiable : les services sont des singletons sans état.

| Service | Dépendances injectées | Mode | Justification / règle de gestion |
|---|---|---|---|
| `ContratServiceImpl` | `IContratRepository` | Constructeur | CRUD. Refuse un montant total négatif ou absent. La suppression d'un contrat supprime ses paiements (cascade `ALL` + `orphanRemoval`). `update` ne touche ni à l'identifiant ni à la collection de paiements. |
| `AgenceServiceImpl` | `IAgenceRepository` | Constructeur | CRUD. Refuse la création ou la modification si `nom`, `ville` ou `adresse` est vide. |
| `EmployeServiceImpl` | `IEmployeRepository` | Constructeur | CRUD. Refuse un employé dont `nom`, `prenom` ou `role` est absent. |
| `VehiculeServiceImpl` | `IVehiculeRepository` | Constructeur | CRUD. Refuse un `tarifJournalier` négatif ou absent. Statut `DISPONIBLE` par défaut à la création. |
| `EquipementServiceImpl` | `IEquipementRepository` | Constructeur | CRUD. Refuse un équipement sans `libelle` (le modèle n'a pas d'attribut quantité). |
| `ClientServiceImpl` | `IClientRepository` | Constructeur | CRUD. Refuse un client dont `nom`, `prenom`, `email` ou `numPermis` est vide. `dateInscription` vaut la date du jour si elle est absente et n'est pas modifiable. |
| `ReservationServiceImpl` | `IReservationRepository` | Constructeur | CRUD. Refuse une `dateFin` antérieure à la `dateDebut`. Statut `EN_ATTENTE` par défaut à la création. |
| `PaiementServiceImpl` | `IPaiementRepository` | Constructeur | Lecture seule (`findById`, `findAll`) : un paiement est créé, modifié ou supprimé à travers son contrat. |
| `MaintenanceServiceImpl` | `IMaintenanceRepository` | Constructeur | CRUD. Refuse une `dateFin` antérieure à la `dateDebut` ; `dateFin` peut rester vide (maintenance en cours). Le modèle n'a pas d'attribut coût. |

Règles communes à tous les services CRUD :

- `create` refuse un identifiant non `null` : sinon `save` déclencherait un `merge` et pourrait écraser une ligne existante.
- `findById` traduit l'absence en `ResourceNotFoundException` au lieu de renvoyer un `Optional` ou `null`.
- `update` valide les données, charge l'entité existante (ce qui lève `ResourceNotFoundException` si elle est absente), recopie uniquement les attributs modifiables, puis appelle `save`. Les associations ne sont pas modifiées : leur gestion relève du Chapitre 9.
- `deleteById` vérifie l'existence avec `existsById`, car `deleteById` du repository ne lève aucune exception si l'identifiant n'existe pas.
- Les violations de règle lèvent une `IllegalArgumentException` avec un message clair. Les contrôles de champs obligatoires sont regroupés dans `ValidationUtils`, en attendant la validation déclarative du Chapitre 10.

## C. Messages d'erreur du conteneur

### Message 1 — `ContratController` requiert un bean `IContratService` introuvable

- **Exception :** `NoSuchBeanDefinitionException`, enveloppée dans une `UnsatisfiedDependencyException`.
- **Bean concerné :** `ContratController`, paramètre 0 de son constructeur.
- **Causes probables côté `ContratServiceImpl` :**
  1. l'annotation `@Service` a été oubliée : la classe n'est pas un bean ;
  2. la classe est placée hors du package racine `tn.esprit.autoloc` (par exemple `tn.esprit.service`) : le scan déclenché par `@SpringBootApplication` ne la détecte jamais.
- **Correction :** ajouter `@Service` sur `ContratServiceImpl` et la placer dans `tn.esprit.autoloc.service`.

### Message 2 — `NotificationController` requiert un seul bean, mais 2 ont été trouvés

- **Exception :** `NoUniqueBeanDefinitionException`.
- **Bean concerné :** `NotificationController`, paramètre 0 de son constructeur. Deux beans implémentent le type demandé : `emailNotificateur` et `smsNotificateur`.
- **Correction 1 — `@Primary` sur la classe :** annoter `EmailNotificateur` avec `@Primary`. Il devient le candidat par défaut partout où ce type est injecté.
- **Correction 2 — `@Qualifier` sur le paramètre du constructeur :** `NotificationController(@Qualifier("smsNotificateur") Notificateur notificateur)`. Le choix est explicite, à l'endroit de l'injection, et prime sur `@Primary`. Avec `@RequiredArgsConstructor`, Lombok ne recopie pas `@Qualifier` du champ vers le constructeur sans la ligne `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier` dans `lombok.config` : sinon, écrire le constructeur à la main.
- **Autre option :** injecter `List<Notificateur>` si le contrôleur doit utiliser toutes les implémentations.

### Message 3 — cycle `clientServiceImpl` ↔ `reservationServiceImpl`

- **Exception :** dépendance circulaire (`BeanCurrentlyInCreationException`).
- **Pourquoi l'injection par constructeur rend le cycle impossible à contourner :** pour construire `ClientServiceImpl`, Spring doit d'abord disposer de `ReservationServiceImpl`, dont le constructeur exige lui-même `ClientServiceImpl`. Aucun des deux objets ne peut exister avant l'autre, et il n'existe pas d'objet « à moitié construit » à injecter, comme avec l'injection par setter ou par attribut. Depuis Spring Boot 2.6, ces cycles sont de toute façon refusés par défaut.
- **Piste de conception :** extraire la responsabilité commune dans un troisième composant dont dépendent les deux services, ou faire dépendre un service du **repository** de l'autre entité plutôt que de son service (par exemple `ReservationServiceImpl` utilise `IClientRepository`). Les dépendances doivent aller dans un seul sens.

## D. Anomalies SonarQube for IDE

| Règle | Où | Explication | Correction apportée |
|---|---|---|---|
| **S6813** — injection par attribut | `ContratServiceImpl`, pendant l'expérience de l'étape 2 (`@Autowired` sur le champ) | La dépendance est cachée, le champ ne peut pas être `final`, et la classe utilisée avec `new` a un champ `null`. | Retour à l'injection par constructeur : `@RequiredArgsConstructor` + champ `final`. |
| **S112** — exception générique levée | `ContratServiceImpl.findById`, pendant l'expérience de l'étape 4 (`RuntimeException` au lieu de `ResourceNotFoundException`) | Une exception générique ne dit pas ce qui s'est passé et ne peut pas être interceptée de façon ciblée (par exemple pour renvoyer un 404 à l'Atelier 5). | Restauration de l'exception métier `ResourceNotFoundException`. |
| **S1128** — import inutile | `ContratServiceImpl`, après les allers-retours de l'étape 2 (`import lombok.RequiredArgsConstructor` sans l'annotation, puis `import org.springframework.beans.factory.annotation.Autowired` après la restauration) | Un import non utilisé est du code mort : il alourdit la lecture et laisse croire à une dépendance qui n'existe plus. | Suppression des imports inutiles. |

Autres choix qui évitent des anomalies : messages d'erreur répétés regroupés dans une constante `RESSOURCE` par service, `ValidationUtils` déclarée `final` avec un constructeur privé (classe utilitaire), et aucun `@Data` sur les entités.

## E. Questions de compréhension

**1. Où est le `new` de `ContratServiceImpl` et qui l'exécute ?**
Il n'apparaît nulle part dans notre code. C'est le conteneur Spring (`ApplicationContext`) qui l'exécute au démarrage. Le scan détecte `@Service`, puis le conteneur appelle par réflexion l'unique constructeur, en lui passant le proxy de `IContratRepository` généré par Spring Data.

**2. Pourquoi un contrôleur dépendra-t-il de `IContratService` et non de `ContratServiceImpl` ?**
Pour dépendre d'une abstraction et non d'une implémentation (couplage faible). On peut changer d'implémentation, ou fournir un faux service dans un test, sans modifier le contrôleur. Le contrôleur ne connaît que le contrat (les méthodes), pas la façon dont elles sont réalisées.

**3. Pourquoi un service singleton doit-il rester sans état ? Exemple de bug.**
Une seule instance du service est partagée par toutes les requêtes HTTP, traitées en parallèle par des threads différents. Si le service avait un attribut `private Contrat contratCourant;`, la requête A pourrait l'affecter, puis la requête B l'écraser avant que A ne l'utilise : A modifierait alors le contrat de B. Ce bug n'apparaît que sous charge et il est très difficile à reproduire.

**4. Pourquoi `update` charge-t-il l'entité existante avant d'appeler `save` au lieu de lui passer directement l'objet reçu ?**
Passer l'objet reçu à `save` déclenche un `merge` qui recopie **toutes** ses colonnes. Les champs absents de la requête deviennent `null`, l'identifiant du corps peut viser une autre ligne, et une collection vide (`paiements`) combinée à `orphanRemoval` supprimerait tous les paiements du contrat. En chargeant l'existant, on vérifie qu'il existe (`ResourceNotFoundException` sinon), on garde son identifiant et ses associations, et on ne recopie que les attributs modifiables.

**5. Différences `@Component` / `@Bean`, `@Primary` / `@Qualifier`, et pourquoi `IPaiementService` n'expose ni création, ni modification, ni suppression ?**
- `@Component` se pose sur **une classe** que l'on écrit : le scan la détecte et Spring l'instancie. `@Bean` se pose sur **une méthode** d'une classe `@Configuration` qui crée l'objet elle-même : on l'utilise pour une classe qu'on ne peut pas annoter (bibliothèque, JDK) ou qui demande une construction particulière.
- `@Primary` se pose sur **le bean** : c'est le candidat par défaut quand plusieurs beans ont le même type. `@Qualifier("nom")` se pose **au point d'injection** et choisit explicitement un bean par son nom ; il prime sur `@Primary`.
- Un `Paiement` n'existe que rattaché à son `Contrat` (composition, cascade `ALL` + `orphanRemoval`). Le créer, le modifier ou le supprimer passe donc par le contrat (`addPaiement` / `removePaiement`, puis enregistrement du contrat). Le contrat est ainsi le seul point d'entrée, ce qui garde l'ensemble cohérent : aucun paiement orphelin et aucune clé étrangère nulle.
