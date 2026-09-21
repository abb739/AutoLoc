# AutoLoc

Plateforme de gestion de location de véhicules multi-agences — Projet ESPRIT ASI (Architecture des Systèmes d'Information).

## Objectifs du projet

AutoLoc est une application Java/Spring Boot conçue pour gérer les réservations de véhicules à travers plusieurs agences. Le projet couvre l'ensemble de la chaîne : catalogue de véhicules, réservations, gestion des agences et administration de la plateforme.

## Acteurs identifiés (Séance 1)

- **Client** — consulte le catalogue, réserve un véhicule, consulte l'historique de ses réservations.
- **Agent d'agence** — enregistre les prises en charge et les retours de véhicules, gère les clients au comptoir.
- **Responsable d'agence** — supervise l'activité de son agence, gère le personnel et les véhicules du parc local.
- **Administrateur** — administre la plateforme au global (agences, utilisateurs, configuration système).

## Stack technique

- **Langage / Build** : Java 17, Maven
- **Framework** : Spring Boot, Spring Data JPA, Spring MVC
- **Base de données** : MySQL 8
- **Productivité** : Lombok, SpringDoc OpenAPI (Swagger UI)
- **Tests** : JUnit 5, Mockito, MockMvc, Jacoco
- **Outillage** : Git/GitHub, Postman, IntelliJ IDEA Ultimate

## Équipe

Projet réalisé dans le cadre de l'UP ASI — ESPRIT, année académique 2025-2026.
