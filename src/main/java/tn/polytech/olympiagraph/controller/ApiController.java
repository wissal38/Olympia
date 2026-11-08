package tn.polytech.olympiagraph.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tn.polytech.olympiagraph.exception.ApiException;
import tn.polytech.olympiagraph.rdf.KnowledgeBase;
import tn.polytech.olympiagraph.rdf.SparqlQueries;
import tn.polytech.olympiagraph.rdf.Vocabulary;
import tn.polytech.olympiagraph.service.SparqlService;

@RestController
public class ApiController {
    private final KnowledgeBase knowledgeBase;
    private final SparqlService sparqlService;

    public ApiController(KnowledgeBase knowledgeBase, SparqlService sparqlService) {
        this.knowledgeBase = knowledgeBase;
        this.sparqlService = sparqlService;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "project", "OlympiaGraph");
    }

    @GetMapping("/api/sports")
    public List<Map<String, String>> sports() {
        return select("""
                PREFIX og: <https://example.org/olympiagraph/ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT DISTINCT ?sport ?label WHERE {
                  ?sport a og:Sport ; rdfs:label ?label .
                }
                ORDER BY ?label
                """);
    }

    @GetMapping("/api/editions")
    public List<Map<String, String>> editions() {
        return select("""
                PREFIX og: <https://example.org/olympiagraph/ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT DISTINCT ?edition ?label ?year WHERE {
                  ?edition a ?season ; rdfs:label ?label .
                  VALUES ?season { og:JeuxEte og:JeuxHiver }
                  OPTIONAL { ?edition og:annee ?year }
                }
                ORDER BY ?year
                """);
    }

    @GetMapping("/api/athletes")
    public List<Map<String, String>> athletes(
            @RequestParam String name,
            @RequestParam(defaultValue = "20") int limit) {
        return sparqlService.findAthletes(name, limit);
    }

    @GetMapping("/api/athletes/{id}")
    public Map<String, String> athlete(@PathVariable String id) {
        if (!id.matches("\\d+")) {
            throw ApiException.badRequest("L’identifiant de l’athlète doit être numérique.");
        }
        Resource athlete = knowledgeBase.rawModel().createResource(SparqlQueries.athleteUri(id));
        if (!knowledgeBase.rawModel().contains(athlete, Vocabulary.FOAF_NAME)) {
            throw ApiException.notFound("Athlète inconnu dans le sous-ensemble chargé.");
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", athlete.getProperty(Vocabulary.FOAF_NAME).getString());
        addLiteralIfPresent(result, athlete, "gender", Vocabulary.FOAF_GENDER);
        addLiteralIfPresent(result, athlete, "age", Vocabulary.AGE);
        addLiteralIfPresent(result, athlete, "height", Vocabulary.TAILLE);
        addLiteralIfPresent(result, athlete, "weight", Vocabulary.POIDS);
        return result;
    }

    @GetMapping("/api/queries")
    public List<Map<String, Object>> queries() {
        return SparqlQueries.list().stream()
                .map(definition -> Map.<String, Object>of(
                        "id", definition.id(),
                        "title", definition.title(),
                        "description", definition.description()))
                .toList();
    }

    @GetMapping("/api/queries/{id}")
    public Map<String, Object> runQuery(
            @PathVariable int id,
            @RequestParam(required = false) String minMedals,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) String games,
            @RequestParam(required = false) String athleteId,
            @RequestParam(required = false) Integer limit) {
        SparqlQueries.Definition definition;
        try {
            definition = SparqlQueries.definition(id);
        } catch (IllegalArgumentException exception) {
            throw ApiException.notFound(exception.getMessage());
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        putIfPresent(parameters, "minMedals", minMedals);
        putIfPresent(parameters, "event", event);
        putIfPresent(parameters, "games", games);
        putIfPresent(parameters, "athleteId", athleteId);
        List<Map<String, String>> results = sparqlService.execute(id, parameters, limit);
        return Map.of(
                "id", definition.id(),
                "title", definition.title(),
                "description", definition.description(),
                "results", results);
    }

    @GetMapping("/api/inference/compare")
    public Map<String, Object> inferenceCompare() {
        Model raw = knowledgeBase.rawModel();
        Model inferred = knowledgeBase.inferredModel();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("counts", List.of(
                compareCount("Athlete", raw, inferred, Vocabulary.ATHLETE, true),
                compareCount("Personne", raw, inferred, Vocabulary.PERSONNE, true),
                compareCount("participeA", raw, inferred, Vocabulary.PARTICIPE_A, false),
                compareCount("Edition", raw, inferred, Vocabulary.EDITION, true)));
        result.put("note", "Les nombres de types comptent les sujets distincts ; participeA compte les triplets.");
        return result;
    }

    @GetMapping("/api/inference/deductions")
    public Map<String, Object> inferenceDeductions(@RequestParam(defaultValue = "50") int limit) {
        if (limit < 1 || limit > 500) {
            throw ApiException.badRequest("limit doit être compris entre 1 et 500.");
        }
        Model deductions = knowledgeBase.deductions();
        List<Map<String, String>> statements = new ArrayList<>();
        var iterator = deductions.listStatements();
        try {
            while (iterator.hasNext() && statements.size() < limit) {
                var statement = iterator.nextStatement();
                statements.add(Map.of(
                        "subject", statement.getSubject().toString(),
                        "predicate", statement.getPredicate().toString(),
                        "object", statement.getObject().toString()));
            }
        } finally {
            iterator.close();
        }
        return Map.of("totalDeductions", deductions.size(), "returned", statements.size(), "deductions", statements);
    }

    private List<Map<String, String>> select(String queryText) {
        Query query = QueryFactory.create(queryText);
        List<Map<String, String>> rows = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create(query, knowledgeBase.inferredModel())) {
            var results = execution.execSelect();
            while (results.hasNext()) {
                var solution = results.next();
                Map<String, String> row = new LinkedHashMap<>();
                solution.varNames().forEachRemaining(variable -> {
                    RDFNode value = solution.get(variable);
                    row.put(variable, value.isLiteral() ? value.asLiteral().getString() : value.toString());
                });
                rows.add(row);
            }
        }
        return rows;
    }

    private Map<String, Object> compareCount(String name, Model raw, Model inferred, Resource target,
                                             boolean countSubjects) {
        long rawCount = countSubjects
                ? raw.listSubjectsWithProperty(RDF.type, target).toSet().size()
                : raw.listStatements(null, Vocabulary.PARTICIPE_A, (RDFNode) null).toSet().size();
        long inferredCount = countSubjects
                ? inferred.listSubjectsWithProperty(RDF.type, target).toSet().size()
                : inferred.listStatements(null, Vocabulary.PARTICIPE_A, (RDFNode) null).toSet().size();
        return Map.of(
                "item", name,
                "sansInference", rawCount,
                "avecInference", inferredCount,
                "tripletsOuTypesAjoutes", inferredCount - rawCount);
    }

    private void addLiteralIfPresent(Map<String, String> result, Resource subject, String key,
                                     org.apache.jena.rdf.model.Property property) {
        if (subject.hasProperty(property)) {
            result.put(key, subject.getProperty(property).getObject().asLiteral().getString());
        }
    }

    private void putIfPresent(Map<String, String> parameters, String name, String value) {
        if (value != null) {
            parameters.put(name, value);
        }
    }
}
