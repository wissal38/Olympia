package tn.polytech.olympiagraph.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.stereotype.Service;

import tn.polytech.olympiagraph.rdf.KnowledgeBase;
import tn.polytech.olympiagraph.rdf.SparqlQueries;

@Service
public class SparqlService {
    private final KnowledgeBase knowledgeBase;

    public SparqlService(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public List<Map<String, String>> execute(int queryId, Map<String, String> parameters, Integer limit) {
        Query query = SparqlQueries.prepare(queryId, parameters);
        if (limit != null) {
            if (limit < 1 || limit > 500) {
                throw new IllegalArgumentException("limit doit être compris entre 1 et 500.");
            }
            query.setLimit(limit);
        }
        List<Map<String, String>> rows = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create(query, knowledgeBase.inferredModel())) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                rows.add(toMap(results.next()));
            }
        }
        return rows;
    }

    public List<Map<String, String>> findAthletes(String name, int limit) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le paramètre name est obligatoire.");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit doit être compris entre 1 et 100.");
        }
        ParameterizedSparqlString sparql = new ParameterizedSparqlString("""
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT DISTINCT ?athlete ?name
                WHERE {
                  ?athlete foaf:name ?name .
                  FILTER(CONTAINS(LCASE(STR(?name)), LCASE($name)))
                }
                ORDER BY ?name
                """);
        sparql.setLiteral("name", name.trim());
        Query query = sparql.asQuery();
        query.setLimit(limit);
        List<Map<String, String>> rows = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create(query, knowledgeBase.inferredModel())) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                rows.add(toMap(results.next()));
            }
        }
        return rows;
    }

    private Map<String, String> toMap(QuerySolution solution) {
        Map<String, String> row = new LinkedHashMap<>();
        solution.varNames().forEachRemaining(variable -> {
            RDFNode node = solution.get(variable);
            row.put(variable, node.isLiteral() ? node.asLiteral().getString() : node.toString());
        });
        return row;
    }
}
