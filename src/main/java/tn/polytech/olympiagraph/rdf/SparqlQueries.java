package tn.polytech.olympiagraph.rdf;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;

public final class SparqlQueries {
    public record Definition(int id, String title, String description, String sparql) {
    }

    private static final String PREFIXES = """
            PREFIX og: <https://example.org/olympiagraph/ontology#>
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            """;

    private static final Map<Integer, Definition> DEFINITIONS = definitions();

    private SparqlQueries() {
    }

    public static List<Definition> list() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static Definition definition(int id) {
        Definition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Numéro de requête inconnu : " + id);
        }
        return definition;
    }

    public static Query prepare(int id, Map<String, String> parameters) {
        Definition definition = definition(id);
        ParameterizedSparqlString sparql = new ParameterizedSparqlString(PREFIXES + definition.sparql());
        switch (id) {
            case 2 -> sparql.setLiteral("minMedals", integerParameter(parameters, "minMedals", 1));
            case 7 -> {
                sparql.setLiteral("eventName", required(parameters, "event"));
                sparql.setLiteral("gamesName", required(parameters, "games"));
            }
            case 8 -> {
                String athleteId = required(parameters, "athleteId");
                if (!athleteId.matches("\\d+")) {
                    throw new IllegalArgumentException("athleteId doit être un identifiant numérique.");
                }
                sparql.setIri("athlete", RESOURCE_NS + "athlete/" + encode(athleteId));
            }
            default -> {
            }
        }
        return sparql.asQuery();
    }

    public static String athleteUri(String athleteId) {
        return RESOURCE_NS + "athlete/" + encode(athleteId);
    }

    private static String required(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le paramètre " + name + " est obligatoire.");
        }
        return value.trim();
    }

    private static int integerParameter(Map<String, String> parameters, String name, int defaultValue) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 100) {
                throw new IllegalArgumentException(name + " doit être compris entre 1 et 100.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " doit être un entier.");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Map<Integer, Definition> definitions() {
        Map<Integer, Definition> queries = new LinkedHashMap<>();
        add(queries, 1, "Top 10 des pays par médailles d’or",
                "Compte les performances en or par pays.",
                """
                SELECT ?country ?countryName (COUNT(?athlete) AS ?goldMedals)
                WHERE {
                  ?athlete og:aGagneOr ?event ;
                           og:representePays ?country .
                  ?country rdfs:label ?countryName .
                }
                GROUP BY ?country ?countryName
                ORDER BY DESC(?goldMedals)
                LIMIT 10
                """);
        add(queries, 2, "Athlètes les plus médaillés",
                "Classe les athlètes ayant au moins le nombre de médailles demandé (HAVING).",
                """
                SELECT ?athlete ?name (COUNT(DISTINCT ?event) AS ?medalCount)
                WHERE {
                  ?athlete og:aGagneMedaille ?event ;
                           foaf:name ?name .
                }
                GROUP BY ?athlete ?name
                HAVING(COUNT(DISTINCT ?event) >= $minMedals)
                ORDER BY DESC(?medalCount)
                LIMIT 20
                """);
        add(queries, 3, "Nombre de médailles par sport",
                "Regroupe les médailles d’or, d’argent et de bronze par sport.",
                """
                SELECT ?sport ?sportName (COUNT(?athlete) AS ?medalCount)
                WHERE {
                  VALUES ?medalProperty { og:aGagneOr og:aGagneArgent og:aGagneBronze }
                  ?athlete ?medalProperty ?event .
                  ?event og:epreuveDe ?sport .
                  ?sport rdfs:label ?sportName .
                }
                GROUP BY ?sport ?sportName
                ORDER BY DESC(?medalCount)
                """);
        add(queries, 4, "Âge moyen des médaillés par sport",
                "Calcule la moyenne d’âge des athlètes médaillés pour chaque sport.",
                """
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
                """);
        add(queries, 5, "Pays sans médaille",
                "Retourne les pays représentés dans le sous-ensemble sans performance médaillée.",
                """
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
                """);
        add(queries, 6, "Sports d’été et d’hiver",
                "Identifie les sports présents dans au moins une édition d’été et une édition d’hiver.",
                """
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
                """);
        add(queries, 7, "Participants d’une épreuve",
                "Liste les participants d’une épreuve et d’une édition (l’inférence complète les médaillés).",
                """
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
                """);
        add(queries, 8, "Palmarès d’un athlète",
                "Liste les épreuves auxquelles l’athlète a participé et les médailles éventuelles.",
                """
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
                """);
        add(queries, 9, "Athlètes plus âgés que la moyenne de leur sport",
                "Compare chaque âge à la moyenne des âges distincts des athlètes observés dans le sport.",
                """
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
                """);
        add(queries, 10, "Catégorie d’âge des médaillés",
                "Classe les médaillés comme juniors (18 ans ou moins) ou seniors (plus de 18 ans).",
                """
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
                """);
        return Collections.unmodifiableMap(queries);
    }

    private static void add(Map<Integer, Definition> queries, int id, String title, String description,
                            String sparql) {
        queries.put(id, new Definition(id, title, description, sparql));
    }

    private static final String RESOURCE_NS = Vocabulary.RESOURCE_NS;
}
