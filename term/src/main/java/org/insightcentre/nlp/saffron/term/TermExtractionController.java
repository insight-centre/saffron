package org.insightcentre.nlp.saffron.term;

import org.insightcentre.nlp.saffron.SaffronModel;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.springframework.boot.context.config.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
            JSONObject returnJson = new JSONObject();
            returnJson.put("terms", r.terms);
            returnJson.put("docTerms", r.docTerms);
            return ResponseEntity.ok(returnJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }

}
