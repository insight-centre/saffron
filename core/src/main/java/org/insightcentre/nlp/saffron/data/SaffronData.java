package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

import java.util.ArrayList;
import java.util.List;

public class SaffronData {

    @JsonAlias("documentTermMapping")
    public ArrayList<DocumentTerm> documentTermMapping = new ArrayList<>();

    @JsonAlias("termsMapping")
    public ArrayList<Term> termsMapping = new ArrayList<>();

    @JsonAlias("documents")
    public ArrayList<Document> documents = new ArrayList<>();

    @JsonAlias("authorTerms")
    public List<AuthorTerm> authorTerms = new ArrayList<>();

    @Override
    public String toString() {
        return String.format("{ %s }", documentTermMapping.toString(), termsMapping.toString(), documents.toString(), authorTerms.toString());
    }

}
