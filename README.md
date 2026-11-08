# OlympiaGraph

Mini-projet de Web sémantique sur les résultats olympiques, réalisé avec Java 17, Spring Boot, Apache Jena 5 et Commons CSV.

## Préparation et lancement

Placez `athlete_events.csv` et `noc_regions.csv` à la racine du projet, puis exécutez :

```powershell
mvn compile exec:java "-Dexec.mainClass=tn.polytech.olympiagraph.tools.ConvertApp" "-Dexec.args=athlete_events.csv noc_regions.csv 15000"
mvn spring-boot:run
```

Le convertisseur sélectionne aléatoirement, avec une graine fixe, au plus 15 000 lignes de l’année 2000 ou après. Il exige au minimum 10 000 lignes éligibles. Les valeurs vides et `NA` ne sont pas ajoutées au graphe. Les mesures d’âge, taille et poids correspondent à la première valeur non vide observée pour chaque identifiant d’athlète dans l’échantillon.

## API JSON

| URL | Fonction |
|---|---|
| `GET /api/health` | État du service |
| `GET /api/sports` | Sports chargés |
| `GET /api/editions` | Éditions d’été et d’hiver |
| `GET /api/athletes?name=bolt&limit=10` | Recherche d’athlètes |
| `GET /api/athletes/{id}` | Fiche d’un athlète |
| `GET /api/queries` | Catalogue des dix requêtes |
| `GET /api/queries/{id}` | Exécution d’une requête (1 à 10) |
| `GET /api/queries/2?minMedals=3` | Paramètre de seuil pour la requête 2 |
| `GET /api/queries/7?event=Athletics%20Men%27s%20100%20metres&games=2000%20Summer` | Participants d’une épreuve avec inférence |
| `GET /api/queries/8?athleteId=94406` | Palmarès d’un athlète échantillonné |
| `GET /api/inference/compare` | Comptages bruts et inférés |
| `GET /api/inference/deductions?limit=50` | Exemples de triplets déduits |

Paramètres invalides : HTTP 400 ; ressource absente : HTTP 404 ; erreur inattendue : HTTP 500 au format JSON.

## Tester avec Postman

1. Démarrez l’API avec `mvn spring-boot:run`.
2. Dans Postman, choisissez **Import** puis sélectionnez `postman/OlympiaGraph.postman_collection.json`.
3. Envoyez d’abord la requête **État du service**. La réponse attendue contient `"status": "ok"`.
4. Les requêtes de la collection utilisent `http://localhost:8080` et couvrent les dix questions SPARQL et l’inférence. Les dossiers de requêtes nommés « Erreur » vérifient les réponses 400 et 404.

## Projet et présentation

Les sources sont organisées dans `controller`, `service`, `rdf`, `exception` et `tools`. Le schéma RDFS est dans `src/main/resources/schema.ttl`. Le diagramme Mermaid est dans [`docs/schema.mmd`](./docs/schema.mmd) ; les instructions d’export et le plan de cinq diapositives sont dans [`docs/presentation.md`](./docs/presentation.md).

La fiche complète de présentation du modèle, des requêtes SPARQL, de l’inférence et des API est dans [`Wiss.md`](./Wiss.md).
