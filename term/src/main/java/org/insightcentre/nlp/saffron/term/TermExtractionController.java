package org.insightcentre.nlp.saffron.term;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.nlp.saffron.SaffronModel;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.springframework.boot.context.config.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/api/v1")
public class TermExtractionController {
    /**
     * Term Extraction endpoint for extracting terms from a given corpus.
     *
     * @param input the SaffronModel
     * @return the response entity
     * @throws ResourceNotFoundException the resource not found exception
     */
    @PostMapping("/term-extraction")
    public ResponseEntity postRequest(
            @RequestBody SaffronModel input)
            throws ResourceNotFoundException {
        try {
            TermExtractionConfiguration config = input.getConfiguration().termExtraction;
            Collection<Document> data = input.getInput().documents;
            Configuration c = new Configuration();
            c.termExtraction = config;
            Corpus searcher = CorpusTools.fromCollection(data);
            final TermExtraction te = new TermExtraction(c.termExtraction);
            final TermExtraction.Result r = te.extractTerms(searcher);
            r.normalize();
            ArrayList<Term> termList = new ArrayList<Term>();
            for (Term term : r.terms) {
                Term newTerm = new Term(term.getString(), term.getOccurrences(),
                        term.getMatches(), term.getScore(), null, null);

                termList.add(newTerm);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(termList);
            String docString = objectMapper.writeValueAsString(r.docTerms);
            String returnString = "{ \"data\": {\"termsMapping\": " + jsonString + ", \"documentTermMapping\": " + docString + "} }";
            returnString=returnString.replace("\"documentId\":","\"document_id\":");
            returnString=returnString.replace("\"termString\":","\"term_string\":");
            returnString=returnString.replaceAll("\\{\\}", "\"none\"");
            return ResponseEntity.ok(returnString);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }

}
