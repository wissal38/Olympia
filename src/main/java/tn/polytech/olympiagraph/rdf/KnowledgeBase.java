package tn.polytech.olympiagraph.rdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class KnowledgeBase {
    private Model schema;
    private Model rawModel;
    private InfModel inferredModel;
    private Model deductionsModel;

    @PostConstruct
    public void load() {
        schema = readClasspathModel("/schema.ttl", "schema RDFS");
        rawModel = loadDataModel();
        inferredModel = ModelFactory.createRDFSModel(schema, rawModel);
        inferredModel.prepare();
        deductionsModel = buildDeductionsModel();
    }

    public Model rawModel() {
        return rawModel;
    }

    public InfModel inferredModel() {
        return inferredModel;
    }

    public Model deductions() {
        return deductionsModel;
    }

    private Model buildDeductionsModel() {
        Model deductions = ModelFactory.createDefaultModel();
        StmtIterator statements = inferredModel.listStatements();
        try {
            while (statements.hasNext()) {
                var statement = statements.nextStatement();
                if (!rawModel.contains(statement) && !schema.contains(statement)) {
                    deductions.add(statement);
                }
            }
        } finally {
            statements.close();
        }
        return deductions;
    }

    private Model loadDataModel() {
        Path generatedFile = Path.of("src/main/resources/data.ttl");
        if (Files.isRegularFile(generatedFile)) {
            Model model = ModelFactory.createDefaultModel();
            try {
                RDFDataMgr.read(model, generatedFile.toUri().toString(), Lang.TURTLE);
                return model;
            } catch (RiotException exception) {
                throw new IllegalStateException("Le fichier data.ttl généré n’est pas un Turtle valide.", exception);
            }
        }
        return readClasspathModel("/data.ttl", "données RDF");
    }

    private Model readClasspathModel(String resourceName, String description) {
        try (InputStream input = getClass().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Ressource absente : " + resourceName
                        + ". Générez data.ttl avec ConvertApp avant de démarrer l’API.");
            }
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, input, Lang.TURTLE);
            return model;
        } catch (IOException | RiotException exception) {
            throw new IllegalStateException("Impossible de charger " + description + ".", exception);
        }
    }
}
