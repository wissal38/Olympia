# OlympiaGraph — fiche de présentation

**Étudiante : Wiss**  
**Thème : Jeux Olympiques**  
**Technologies : Java 17, Spring Boot, Apache Jena 5, SPARQL et RDFS**

## 1. Sujet et données

Le projet représente des résultats olympiques sous forme de graphe RDF afin de répondre à des questions sur les athlètes, les pays, les sports, les épreuves, les éditions et les médailles.

Les sources CSV sont `athlete_events.csv` et `noc_regions.csv`. Le convertisseur sélectionne un échantillon reproductible de 15 000 lignes à partir de l’année 2000, associe les codes NOC aux noms de pays et produit `src/main/resources/data.ttl`. Les valeurs manquantes ou `NA` d’âge, de taille, de poids et de médaille ne sont pas ajoutées comme valeurs RDF. L’âge, la taille et le poids d’un athlète sont ceux de la première ligne retenue qui fournit ces informations.

## 2. Classes du vocabulaire

Une **classe** représente une catégorie d’entités. Une ressource individuelle est ensuite déclarée comme instance d’une classe avec `rdf:type` (ou `a` en Turtle).

| Classe | Signification | Exemple d’instance |
|---|---|---|
| `og:Personne` | Personne du modèle OlympiaGraph | Une personne décrite dans le graphe |
| `og:Athlete` | Athlète olympique | `og:athlete/69629` |
| `og:Pays` | Pays représenté par un athlète | `og:country/CHN` |
| `og:Edition` | Édition olympique | `og:edition/2016%20Summer` |
| `og:JeuxEte` | Édition des Jeux d’été | Une édition Summer |
| `og:JeuxHiver` | Édition des Jeux d’hiver | Une édition Winter |
| `og:Sport` | Sport olympique | Une ressource de sport |
| `og:Epreuve` | Épreuve d’un sport dans une édition | Une ressource d’épreuve |

Le préfixe `og:` représente l’espace de noms du projet : `https://example.org/olympiagraph/ontology#`. `foaf:Person` est une classe externe réutilisée, et non une classe créée par OlympiaGraph.

Exemple de déclaration d’une classe et de son héritage :

```turtle
og:Athlete a rdfs:Class ;
    rdfs:label "Athlète"@fr ;
    rdfs:subClassOf og:Personne .
```

Exemple d’une ressource individuelle (donnée du graphe) :

```turtle
<https://example.org/olympiagraph/resource/athlete/69629>
    foaf:name "Liang Xinping" ;
    og:aGagneArgent
      <https://example.org/olympiagraph/resource/event/2016%20Summer%7CSynchronized%20Swimming%20Women%27s%20Team> .
```

Le convertisseur n’ajoute pas explicitement `rdf:type og:Athlete` à ces ressources : Jena peut inférer ce type à partir du domaine de leurs propriétés.

## 3. Propriétés du modèle

Une **propriété** exprime une relation ou une information portée par une ressource. Dans un triplet RDF, elle est le prédicat : `sujet — propriété → objet`.

### Propriétés objet

| Propriété | Domaine (`rdfs:domain`) | Portée (`rdfs:range`) | Exemple de lecture |
|---|---|---|---|
| `og:participeA` | `og:Athlete` | `og:Epreuve` | Un athlète participe à une épreuve |
| `og:aGagneMedaille` | `og:Athlete` | `og:Epreuve` | Un athlète a gagné une médaille à une épreuve |
| `og:aGagneOr` | `og:Athlete` | `og:Epreuve` | Un athlète a gagné l’or à une épreuve |
| `og:aGagneArgent` | `og:Athlete` | `og:Epreuve` | Un athlète a gagné l’argent à une épreuve |
| `og:aGagneBronze` | `og:Athlete` | `og:Epreuve` | Un athlète a gagné le bronze à une épreuve |
| `og:epreuveDe` | `og:Epreuve` | `og:Sport` | Une épreuve concerne un sport |
| `og:deLEdition` | `og:Epreuve` | `og:Edition` | Une épreuve appartient à une édition |
| `og:representePays` | `og:Athlete` | `og:Pays` | Un athlète représente un pays |

Les propriétés de médaille ont `rdfs:subPropertyOf` dans la hiérarchie décrite ci-dessous. Elles ne sont **pas des classes** et les médailles ne sont pas modélisées comme des ressources séparées.

### Propriétés littérales

| Propriété | Domaine | Type de valeur |
|---|---|---|
| `og:annee` | `og:Edition` | `xsd:integer` |
| `og:villeHote` | `og:Edition` | `xsd:string` |
| `og:age` | `og:Athlete` | `xsd:integer` |
| `og:taille` | `og:Athlete` | `xsd:decimal` |
| `og:poids` | `og:Athlete` | `xsd:decimal` |
| `foaf:name` | `foaf:Person` | `xsd:string` |
| `foaf:gender` | `foaf:Person` | `xsd:string` |

`rdfs:domain` indique le type déductible du sujet quand une propriété est utilisée. `rdfs:range` indique le type attendu/déductible de la valeur objet. Par exemple :

```turtle
og:participeA rdfs:domain og:Athlete ;
              rdfs:range og:Epreuve .
```

Si le graphe contient `:personneX og:participeA :epreuveY`, le raisonneur RDFS peut en déduire que `:personneX` est une `og:Athlete` et que `:epreuveY` est une `og:Epreuve`.

## 4. Différence entre les deux héritages RDFS

| Terme | Relie quoi ? | Question à laquelle il répond | Exemple |
|---|---|---|---|
| `rdfs:subClassOf` | Deux classes | « Une instance de cette classe est-elle aussi une instance de la classe plus générale ? » | `og:Athlete rdfs:subClassOf og:Personne` |
| `rdfs:subPropertyOf` | Deux propriétés | « Une relation précise implique-t-elle aussi une relation plus générale ? » | `og:aGagneOr rdfs:subPropertyOf og:aGagneMedaille` |

Exemple de la hiérarchie des classes :

```text
Athlete → Personne → foaf:Person
JeuxEte → Edition
JeuxHiver → Edition
```

Un athlète reconnu comme `og:Athlete` est donc aussi une `og:Personne` et une `foaf:Person`.

Exemple de la hiérarchie des propriétés :

```text
aGagneOr ─┐
aGagneArgent ─┼→ aGagneMedaille → participeA
aGagneBronze ─┘
```

Une médaille d’or à une épreuve permet donc de répondre à une question générale sur les médailles et à une question sur les participants de l’épreuve. Les flèches d’héritage sont documentées dans `src/main/resources/schema.ttl` et illustrées dans `docs/schema.mmd`.

## 5. Vocabulaire externe FOAF

FOAF (*Friend of a Friend*) est un vocabulaire RDF réutilisable pour décrire des personnes. OlympiaGraph l’utilise de trois façons :

- `foaf:Person` est la classe mère de `og:Personne` (`rdfs:subClassOf`).
- `foaf:name` porte le nom de la personne.
- `foaf:gender` porte son genre.

Exemple d’héritage :

```turtle
og:Personne rdfs:subClassOf foaf:Person .
```

Exemple de données :

```turtle
<https://example.org/olympiagraph/resource/athlete/69629>
    foaf:name "Liang Xinping" ;
    foaf:gender "female" .
```

Les libellés lisibles du schéma utilisent aussi `rdfs:label` avec une étiquette française, par exemple `"Athlète"@fr`.

## 6. Requêtes SPARQL

Les dix requêtes sont définies dans `SparqlQueries.java` et exécutées sur le modèle inféré. Pour les requêtes paramétrées, le service utilise Jena `ParameterizedSparqlString` : les valeurs fournies par les utilisateurs sont liées comme paramètres, pas assemblées par concaténation.

Préfixes utilisés dans les exemples :

```sparql
PREFIX og:   <https://example.org/olympiagraph/ontology#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
```

### 1. Top 10 des pays par nombre de médailles d’or

```sparql
SELECT ?country ?countryName (COUNT(?athlete) AS ?goldMedals)
WHERE {
  ?athlete og:aGagneOr ?event ;
           og:representePays ?country .
  ?country rdfs:label ?countryName .
}
GROUP BY ?country ?countryName
ORDER BY DESC(?goldMedals)
LIMIT 10
```

### 2. Athlètes les plus médaillés — `HAVING`

Paramètre Jena : `$minMedals` (par défaut `1`).

```sparql
SELECT ?athlete ?name (COUNT(DISTINCT ?event) AS ?medalCount)
WHERE {
  ?athlete og:aGagneMedaille ?event ;
           foaf:name ?name .
}
GROUP BY ?athlete ?name
HAVING(COUNT(DISTINCT ?event) >= $minMedals)
ORDER BY DESC(?medalCount)
LIMIT 20
```

### 3. Nombre de médailles par sport — `GROUP BY`

```sparql
SELECT ?sport ?sportName (COUNT(?athlete) AS ?medalCount)
WHERE {
  VALUES ?medalProperty { og:aGagneOr og:aGagneArgent og:aGagneBronze }
  ?athlete ?medalProperty ?event .
  ?event og:epreuveDe ?sport .
  ?sport rdfs:label ?sportName .
}
GROUP BY ?sport ?sportName
ORDER BY DESC(?medalCount)
```

### 4. Âge moyen des médaillés par sport — `AVG`

```sparql
SELECT ?sport ?sportName (AVG(?ageValue) AS ?averageAge)
WHERE {
  VALUES ?medalProperty { og:aGagneOr og:aGagneArgent og:aGagneBronze }
  ?athlete ?medalProperty ?event ;
           og:age ?ageValue .
  ?event og:epreuveDe ?sport .
  ?sport rdfs:label ?sportName .
}
GROUP BY ?sport ?sportName
ORDER BY ?sportName
```

### 5. Pays représentés sans médaille dans l’échantillon — `FILTER NOT EXISTS`

```sparql
SELECT DISTINCT ?country ?countryName
WHERE {
  ?athlete og:representePays ?country .
  ?country rdfs:label ?countryName .
  FILTER NOT EXISTS {
    ?anyAthlete og:representePays ?country ;
                og:aGagneMedaille ?anyEvent .
  }
}
ORDER BY ?countryName
```

### 6. Sports présents aux Jeux d’été et d’hiver

```sparql
SELECT DISTINCT ?sport ?sportName
WHERE {
  ?summerEvent og:epreuveDe ?sport ;
               og:deLEdition ?summerEdition .
  ?summerEdition a og:JeuxEte .
  ?winterEvent og:epreuveDe ?sport ;
               og:deLEdition ?winterEdition .
  ?winterEdition a og:JeuxHiver .
  ?sport rdfs:label ?sportName .
}
ORDER BY ?sportName
```

### 7. Participants d’une épreuve et d’une édition — nécessite l’inférence

Paramètres : `$eventName` et `$gamesName`.

```sparql
SELECT DISTINCT ?athlete ?name
WHERE {
  ?event rdfs:label ?eventLabel ;
         og:deLEdition ?edition .
  ?edition rdfs:label ?gamesLabel .
  FILTER(LCASE(STR(?eventLabel)) = LCASE(STR($eventName)))
  FILTER(LCASE(STR(?gamesLabel)) = LCASE(STR($gamesName)))
  ?athlete og:participeA ?event ;
           foaf:name ?name .
}
ORDER BY ?name
```

### 8. Palmarès d’un athlète — nécessite l’inférence

Paramètre : `$athlete` (IRI de l’athlète).

```sparql
SELECT DISTINCT ?eventName ?gamesName ?sportName ?medalType
WHERE {
  $athlete og:participeA ?event .
  ?event rdfs:label ?eventName ;
         og:deLEdition ?edition ;
         og:epreuveDe ?sport .
  ?edition rdfs:label ?gamesName .
  ?sport rdfs:label ?sportName .
  OPTIONAL {
    $athlete ?medalProperty ?event .
    VALUES ?medalProperty {
      og:aGagneOr og:aGagneArgent og:aGagneBronze
    }
    BIND(REPLACE(STR(?medalProperty), "^.*#", "") AS ?medalType)
  }
}
ORDER BY ?gamesName ?sportName ?eventName
```

### 9. Athlètes plus âgés que la moyenne de leur sport — sous-requête

```sparql
SELECT DISTINCT ?athlete ?name ?sportName ?ageValue ?averageAge
WHERE {
  {
    SELECT ?sport (AVG(?observedAge) AS ?averageAge)
    WHERE {
      {
        SELECT DISTINCT ?sport ?observedAthlete ?observedAge
        WHERE {
          ?observedAthlete og:participeA ?observedEvent ;
                            og:age ?observedAge .
          ?observedEvent og:epreuveDe ?sport .
        }
      }
    }
    GROUP BY ?sport
  }
  ?athlete og:participeA ?event ;
           og:age ?ageValue ;
           foaf:name ?name .
  ?event og:epreuveDe ?sport .
  ?sport rdfs:label ?sportName .
  FILTER(?ageValue > ?averageAge)
}
ORDER BY ?sportName DESC(?ageValue)
```

### 10. Catégorie d’âge des médaillés — `BIND` et `IF`

```sparql
SELECT DISTINCT ?name ?ageValue ?category ?eventName
WHERE {
  VALUES ?medalProperty { og:aGagneOr og:aGagneArgent og:aGagneBronze }
  ?athlete ?medalProperty ?event ;
           foaf:name ?name ;
           og:age ?ageValue .
  ?event rdfs:label ?eventName .
  BIND(IF(?ageValue <= 18, "junior", "senior") AS ?category)
}
ORDER BY ?category ?name
```

## 7. Inférence RDFS

Le modèle brut contient les déclarations du schéma et les assertions produites depuis le CSV. Le `InfModel` de Jena applique le raisonnement RDFS. Les athlètes ne reçoivent pas de type `og:Athlete` explicite dans `data.ttl`.

| Nom pédagogique | Règle | Exemple : départ → déduction |
|---|---|---|
| R1 — types par sous-classe | Si `C1 rdfs:subClassOf C2` et `x rdf:type C1`, alors `x rdf:type C2`. | `x rdf:type og:Athlete` → `x rdf:type og:Personne`, puis `x rdf:type foaf:Person` |
| R2 — relations par sous-propriété | Si `p1 rdfs:subPropertyOf p2` et `x p1 y`, alors `x p2 y`. | `x og:aGagneOr e` → `x og:aGagneMedaille e` |
| R3 — transitivité des sous-classes | Si `C1` est sous-classe de `C2`, et `C2` de `C3`, alors `C1` est sous-classe de `C3`. | `og:Athlete → og:Personne → foaf:Person` donne `og:Athlete rdfs:subClassOf foaf:Person` |
| R4 — transitivité des sous-propriétés | Si `p1` est sous-propriété de `p2`, et `p2` de `p3`, alors `p1` est sous-propriété de `p3`. | `og:aGagneOr → og:aGagneMedaille → og:participeA` donne `og:aGagneOr rdfs:subPropertyOf og:participeA` |
| Domaine et portée | L’utilisation d’une propriété permet de déduire le type du sujet (domaine) et de l’objet (portée). | `x og:participeA e` donne `x rdf:type og:Athlete` et `e rdf:type og:Epreuve` |

Ces noms R1–R4 sont des règles pédagogiques résumant les conséquences RDFS utilisées ; Jena réalise le raisonnement à partir du schéma, plutôt que d’exécuter quatre règles Java écrites séparément.

### Exemples de triplets inférés

| Triplet de départ | Règle | Triplet inféré |
|---|---|---|
| `:athlete/69629 og:aGagneArgent :epreuve/2016-synchro` | R2 puis R4 | `:athlete/69629 og:aGagneMedaille :epreuve/2016-synchro`, puis `:athlete/69629 og:participeA :epreuve/2016-synchro` |
| `:athlete/69629 og:aGagneArgent :epreuve/2016-synchro` et domaine de `og:aGagneArgent` | Domaine | `:athlete/69629 rdf:type og:Athlete` |
| `:athlete/69629 rdf:type og:Athlete` et hiérarchie des classes | R1 | `:athlete/69629 rdf:type og:Personne`, puis `:athlete/69629 rdf:type foaf:Person` |
| `:edition/2016-Summer rdf:type og:JeuxEte` | R1 | `:edition/2016-Summer rdf:type og:Edition` |

Les identifiants abrégés `:athlete/...` et `:epreuve/...` sont ici des notations de lecture ; les véritables IRI du dataset commencent par `https://example.org/olympiagraph/resource/`.

### Comparaison et commentaire : sans / avec inférence

Pour l’échantillon de 15 000 lignes actuellement généré, l’API `/api/inference/compare` a retourné :

| Élément mesuré | Sans inférence | Avec inférence | Commentaire |
|---|---:|---:|---|
| Sujets de type `og:Athlete` | 0 | 12 617 | Aucun type d’athlète n’est ajouté directement par le convertisseur ; le domaine des relations permet de l’inférer. |
| Sujets de type `og:Personne` | 0 | 12 617 | Le type est propagé depuis `og:Athlete` par `rdfs:subClassOf`. |
| Triplets `og:participeA` | 12 942 | 15 000 | 2 058 participations supplémentaires sont obtenues à partir des relations de médaille. |
| Sujets de type `og:Edition` | 0 | 9 | Les éditions d’été et d’hiver sont propagées vers leur classe parente. |

Le résultat est important pour les requêtes 7 et 8 : si on interroge uniquement les assertions brutes avec `og:participeA`, les liens de participation des médaillés sont absents. Avec le raisonneur, la hiérarchie des propriétés permet de retrouver tous les participants représentés par les 15 000 lignes. Les chiffres dépendent du fichier CSV et de l’échantillon ; ils doivent être actualisés après régénération de `data.ttl`.

## 8. API REST en JSON

L’application Spring Boot expose les endpoints suivants sur `http://localhost:8080` :

| Méthode et URL | Résultat |
|---|---|
| `GET /api/health` | État du service |
| `GET /api/sports` | Liste des sports |
| `GET /api/editions` | Liste des éditions d’été et d’hiver |
| `GET /api/athletes?name=bolt&limit=10` | Recherche d’athlètes par nom |
| `GET /api/athletes/94406` | Fiche d’un athlète (ID à choisir dans le dataset) |
| `GET /api/queries` | Catalogue des dix requêtes |
| `GET /api/queries/1` à `/api/queries/10` | Exécute une requête SPARQL |
| `GET /api/queries/2?minMedals=3` | Seuil de médailles |
| `GET /api/queries/7?event=Athletics%20Men%27s%20100%20metres&games=2000%20Summer` | Participants d’une épreuve |
| `GET /api/queries/8?athleteId=94406` | Palmarès d’un athlète |
| `GET /api/inference/compare` | Comptages bruts et inférés |
| `GET /api/inference/deductions?limit=20` | Triplets déduits |

Les erreurs sont renvoyées en JSON : `400` pour un paramètre invalide, `404` pour une requête ou une ressource inconnue, et `500` en cas d’erreur interne. La collection Postman de test est dans `postman/OlympiaGraph.postman_collection.json`.

## 9. Où retrouver les fichiers dans le projet

- Schéma et classes/propriétés RDFS : `src/main/resources/schema.ttl`
- Diagramme simplifié : `docs/schema.mmd`
- Conversion des CSV en RDF : `src/main/java/tn/polytech/olympiagraph/tools/ConvertApp.java`
- Les dix définitions SPARQL : `src/main/java/tn/polytech/olympiagraph/rdf/SparqlQueries.java`
- Modèle brut et modèle avec inférence : `src/main/java/tn/polytech/olympiagraph/rdf/KnowledgeBase.java`
- Endpoints REST : `src/main/java/tn/polytech/olympiagraph/controller/ApiController.java`
- Collection Postman : `postman/OlympiaGraph.postman_collection.json`

Pour générer les données et lancer le service :

```powershell
mvn compile exec:java "-Dexec.mainClass=tn.polytech.olympiagraph.tools.ConvertApp" "-Dexec.args=athlete_events.csv noc_regions.csv 15000"
mvn spring-boot:run
```
