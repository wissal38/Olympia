package tn.polytech.olympiagraph.tools;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDFS;

import tn.polytech.olympiagraph.rdf.Vocabulary;

public final class ConvertApp {
    private static final int DEFAULT_MAX_ROWS = 15_000;
    private static final int MIN_ROWS = 10_000;
    private static final int MAX_ROWS = 20_000;
    private static final int MIN_YEAR = 2000;
    private static final long SAMPLE_SEED = 20261102L;
    private static final Set<String> REQUIRED_HEADERS =
            Set.of("ID", "Name", "Sex", "Age", "Height", "Weight", "NOC", "Games", "Year",
                    "Season", "City", "Sport", "Event", "Medal");

    private ConvertApp() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: ConvertApp <athlete_events.csv> <noc_regions.csv> [nombre-de-lignes]");
        }
        int maxRows = args.length == 3 ? Integer.parseInt(args[2]) : DEFAULT_MAX_ROWS;
        if (maxRows < MIN_ROWS || maxRows > MAX_ROWS) {
            throw new IllegalArgumentException("Le nombre de lignes doit être compris entre 10000 et 20000.");
        }
        Path output = Path.of("src/main/resources/data.ttl");
        long selected = convert(Path.of(args[0]), Path.of(args[1]), output, maxRows);
        System.out.printf(Locale.ROOT, "%d lignes échantillonnées ; RDF écrit dans %s%n", selected, output);
    }

    public static long convert(Path eventsPath, Path nocPath, Path outputPath, int maxRows) throws IOException {
        JenaSystem.init();
        if (maxRows < MIN_ROWS || maxRows > MAX_ROWS) {
            throw new IllegalArgumentException("Le nombre de lignes doit être compris entre 10000 et 20000.");
        }
        Map<String, String> regions = readRegions(nocPath);
        List<CSVRecord> sample = reservoirSample(eventsPath, maxRows);
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("og", Vocabulary.NS);
        model.setNsPrefix("foaf", Vocabulary.FOAF_NS);
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");

        Map<String, Resource> countries = new HashMap<>();
        Map<String, Resource> editions = new HashMap<>();
        Map<String, Resource> sports = new HashMap<>();
        Map<String, Resource> events = new HashMap<>();
        Set<String> attributesAdded = new java.util.HashSet<>();

        for (CSVRecord row : sample) {
            String athleteId = value(row, "ID");
            if (athleteId.isBlank()) {
                continue;
            }
            Resource athlete = model.createResource(Vocabulary.RESOURCE_NS + "athlete/" + encode(athleteId));
            addFrenchLabel(athlete, value(row, "Name"));
            String name = value(row, "Name");
            if (!name.isBlank() && !model.contains(athlete, Vocabulary.FOAF_NAME)) {
                model.add(athlete, Vocabulary.FOAF_NAME, name);
            }
            String gender = switch (value(row, "Sex").toUpperCase(Locale.ROOT)) {
                case "M" -> "male";
                case "F" -> "female";
                default -> "";
            };
            if (!gender.isBlank() && !model.contains(athlete, Vocabulary.FOAF_GENDER)) {
                model.add(athlete, Vocabulary.FOAF_GENDER, gender);
            }
            if (attributesAdded.add(athleteId)) {
                addInteger(model, athlete, Vocabulary.AGE, value(row, "Age"));
                addDecimal(model, athlete, Vocabulary.TAILLE, value(row, "Height"));
                addDecimal(model, athlete, Vocabulary.POIDS, value(row, "Weight"));
            }

            String noc = value(row, "NOC");
            if (!noc.isBlank()) {
                Resource country = countries.computeIfAbsent(noc, key -> {
                    Resource resource = model.createResource(Vocabulary.RESOURCE_NS + "country/" + encode(key));
                    resource.addProperty(org.apache.jena.vocabulary.RDF.type, Vocabulary.PAYS);
                    addFrenchLabel(resource, regions.getOrDefault(key, key));
                    return resource;
                });
                model.add(athlete, Vocabulary.REPRESENTE_PAYS, country);
            }

            String games = value(row, "Games");
            if (games.isBlank()) {
                continue;
            }
            Resource edition = editions.computeIfAbsent(games, key -> {
                Resource resource = model.createResource(Vocabulary.RESOURCE_NS + "edition/" + encode(key));
                resource.addProperty(org.apache.jena.vocabulary.RDF.type,
                        value(row, "Season").equalsIgnoreCase("Winter")
                                ? Vocabulary.JEUX_HIVER : Vocabulary.JEUX_ETE);
                addFrenchLabel(resource, key);
                addInteger(model, resource, Vocabulary.ANNEE, value(row, "Year"));
                String city = value(row, "City");
                if (!city.isBlank()) {
                    model.add(resource, Vocabulary.VILLE_HOTE, model.createLiteral(city, "fr"));
                }
                return resource;
            });

            String sportName = value(row, "Sport");
            String eventName = value(row, "Event");
            if (sportName.isBlank() || eventName.isBlank()) {
                continue;
            }
            Resource sport = sports.computeIfAbsent(sportName, key -> {
                Resource resource = model.createResource(Vocabulary.RESOURCE_NS + "sport/" + encode(key));
                resource.addProperty(org.apache.jena.vocabulary.RDF.type, Vocabulary.SPORT);
                addFrenchLabel(resource, key);
                return resource;
            });
            String eventKey = games + "|" + eventName;
            Resource event = events.computeIfAbsent(eventKey, key -> {
                Resource resource = model.createResource(Vocabulary.RESOURCE_NS + "event/" + encode(key));
                resource.addProperty(org.apache.jena.vocabulary.RDF.type, Vocabulary.EPREUVE);
                addFrenchLabel(resource, eventName);
                model.add(resource, Vocabulary.EPREUVE_DE, sport);
                model.add(resource, Vocabulary.DE_L_EDITION, edition);
                return resource;
            });

            String medal = value(row, "Medal");
            switch (medal.toLowerCase(Locale.ROOT)) {
                case "gold" -> model.add(athlete, Vocabulary.A_GAGNE_OR, event);
                case "silver" -> model.add(athlete, Vocabulary.A_GAGNE_ARGENT, event);
                case "bronze" -> model.add(athlete, Vocabulary.A_GAGNE_BRONZE, event);
                default -> model.add(athlete, Vocabulary.PARTICIPE_A, event);
            }
        }

        Files.createDirectories(outputPath.toAbsolutePath().getParent());
        try (var output = Files.newOutputStream(outputPath)) {
            model.write(output, "TURTLE");
        } finally {
            model.close();
        }
        return sample.size();
    }

    private static List<CSVRecord> reservoirSample(Path eventsPath, int maxRows) throws IOException {
        List<CSVRecord> sample = new ArrayList<>(maxRows);
        Random random = new Random(SAMPLE_SEED);
        long eligible = 0;
        try (Reader reader = Files.newBufferedReader(eventsPath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {
            if (!parser.getHeaderMap().keySet().containsAll(REQUIRED_HEADERS)) {
                throw new IOException("Le fichier athlete_events.csv ne contient pas toutes les colonnes attendues.");
            }
            for (CSVRecord row : parser) {
                if (integerValue(value(row, "Year")) < MIN_YEAR || value(row, "ID").isBlank()) {
                    continue;
                }
                eligible++;
                if (sample.size() < maxRows) {
                    sample.add(row);
                } else {
                    long slot = random.nextLong(eligible);
                    if (slot < maxRows) {
                        sample.set((int) slot, row);
                    }
                }
            }
        }
        if (eligible < MIN_ROWS) {
            throw new IOException("Le CSV contient moins de 10000 lignes exploitables à partir de l’année 2000.");
        }
        return sample;
    }

    private static Map<String, String> readRegions(Path path) throws IOException {
        Map<String, String> regions = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {
            for (CSVRecord row : parser) {
                String noc = value(row, "NOC");
                String region = value(row, "region");
                if (!noc.isBlank() && !region.isBlank()) {
                    regions.put(noc, region);
                }
            }
        }
        return regions;
    }

    private static void addFrenchLabel(Resource resource, String label) {
        if (!label.isBlank() && !resource.hasProperty(RDFS.label)) {
            resource.addProperty(RDFS.label, resource.getModel().createLiteral(label, "fr"));
        }
    }

    private static void addInteger(Model model, Resource subject, org.apache.jena.rdf.model.Property property,
                                   String rawValue) {
        if (!rawValue.isBlank() && !rawValue.equalsIgnoreCase("NA")) {
            try {
                model.add(subject, property, model.createTypedLiteral(new BigInteger(rawValue), XSDDatatype.XSDinteger));
            } catch (NumberFormatException ignored) {
                // Les valeurs absentes ou mal formées du CSV sont ignorées.
            }
        }
    }

    private static void addDecimal(Model model, Resource subject, org.apache.jena.rdf.model.Property property,
                                   String rawValue) {
        if (!rawValue.isBlank() && !rawValue.equalsIgnoreCase("NA")) {
            try {
                model.add(subject, property,
                        model.createTypedLiteral(new BigDecimal(rawValue), XSDDatatype.XSDdecimal));
            } catch (NumberFormatException ignored) {
                // Les valeurs absentes ou mal formées du CSV sont ignorées.
            }
        }
    }

    private static int integerValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static String value(CSVRecord row, String column) {
        try {
            String value = row.get(column);
            return value == null ? "" : value.trim();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
