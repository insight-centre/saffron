package org.insightcentre.nlp.saffron.topic.topicsim;

import org.insightcentre.nlp.saffron.SaffronModel;
import org.insightcentre.nlp.saffron.config.TermSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.springframework.boot.context.config.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TermSimilarityController {
    /**
     * Term Extraction endpoint for extracting terms from a given corpus.
     *
     * @param input the DocumentIndexModel
     * @return the response entity
     * @throws ResourceNotFoundException the resource not found exception
     */
    @PostMapping("/term-similarity")
    public ResponseEntity postRequest(
            @RequestBody SaffronModel input)
            throws ResourceNotFoundException {
        try {
            TermSimilarityConfiguration config = input.getConfiguration().termSim;
            List<DocumentTerm> docTerms = input.getInput().documentTermMapping;
            TermSimilarity ts = new TermSimilarity(config);
            return ResponseEntity.ok(ts.termSimilarity(docTerms));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }

}
