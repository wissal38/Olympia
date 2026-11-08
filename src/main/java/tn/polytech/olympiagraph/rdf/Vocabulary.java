package tn.polytech.olympiagraph.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public final class Vocabulary {
    public static final String NS = "https://example.org/olympiagraph/ontology#";
    public static final String RESOURCE_NS = "https://example.org/olympiagraph/resource/";
    public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

    public static final Resource ATHLETE = resource("Athlete");
    public static final Resource PERSONNE = resource("Personne");
    public static final Resource PAYS = resource("Pays");
    public static final Resource EDITION = resource("Edition");
    public static final Resource JEUX_ETE = resource("JeuxEte");
    public static final Resource JEUX_HIVER = resource("JeuxHiver");
    public static final Resource SPORT = resource("Sport");
    public static final Resource EPREUVE = resource("Epreuve");

    public static final Property PARTICIPE_A = property("participeA");
    public static final Property A_GAGNE_MEDAILLE = property("aGagneMedaille");
    public static final Property A_GAGNE_OR = property("aGagneOr");
    public static final Property A_GAGNE_ARGENT = property("aGagneArgent");
    public static final Property A_GAGNE_BRONZE = property("aGagneBronze");
    public static final Property EPREUVE_DE = property("epreuveDe");
    public static final Property DE_L_EDITION = property("deLEdition");
    public static final Property REPRESENTE_PAYS = property("representePays");
    public static final Property ANNEE = property("annee");
    public static final Property VILLE_HOTE = property("villeHote");
    public static final Property AGE = property("age");
    public static final Property TAILLE = property("taille");
    public static final Property POIDS = property("poids");
    public static final Property FOAF_NAME = ResourceFactory.createProperty(FOAF_NS, "name");
    public static final Property FOAF_GENDER = ResourceFactory.createProperty(FOAF_NS, "gender");

    private Vocabulary() {
    }

    public static Resource resource(String localName) {
        return ResourceFactory.createResource(NS + localName);
    }

    public static Property property(String localName) {
        return ResourceFactory.createProperty(NS + localName);
    }
}
