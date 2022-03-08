package org.insightcentre.nlp.saffron.documentindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.IteratorUtils;
import org.insightcentre.nlp.saffron.SaffronModel;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.springframework.boot.context.config.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;


@RestController
@RequestMapping("/api/v1")
public class DocumentIndexController {
    /**
     * Term Extraction endpoint for extracting terms from a given corpus.
     *
     * @param input the SaffronModel
     * @return the response entity
     * @throws ResourceNotFoundException the resource not found exception
     */
    @PostMapping("/documentindex")
    public ResponseEntity postRequest(
            @RequestBody SaffronModel input)
            throws ResourceNotFoundException {
        try {
            Collection<Document> data = input.getInput().documents;
            Corpus corpus = CorpusTools.fromCollection(data);
            return ResponseEntity.ok(corpus);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }
}
