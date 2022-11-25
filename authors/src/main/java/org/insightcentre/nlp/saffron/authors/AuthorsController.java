package org.insightcentre.nlp.saffron.authors;

import org.insightcentre.nlp.saffron.SaffronModel;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.authors.connect.ConnectAuthorTerm;
import org.insightcentre.nlp.saffron.authors.sim.AuthorSimilarity;
import org.insightcentre.nlp.saffron.config.AuthorSimilarityConfiguration;
import org.insightcentre.nlp.saffron.config.AuthorTermConfiguration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@RestController
@RequestMapping("/api/v1")
public class AuthorsController {
    /**
     * Author consolidation endpoint
     *
     * @param input the SaffronModel
     * @return the response entity
     * @throws ConfigDataResourceNotFoundException the resource not found exception
     */
    @PostMapping("/author-consolidation")
    public ResponseEntity postAuthorConsolidationRequest(
            @RequestBody SaffronModel input)
            throws ConfigDataResourceNotFoundException {
        try {
            Corpus searcher = CorpusTools.fromCollection(input.getInput().documents);
            Set<Author> authors = Consolidate.extractAuthors(searcher);
            Map<Author, Set<Author>> consolidation = new ConsolidateAuthors().consolidate(authors);
            Corpus newCorpus = Consolidate.applyConsolidation(searcher, consolidation, new DefaultSaffronListener());
            return ResponseEntity.ok(newCorpus);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }

    /**
     * Author consolidation endpoint
     *
     * @param authorsConnectionModel the AuthorsConnectionModel
     * @return the response entity
     * @throws ConfigDataResourceNotFoundException the resource not found exception
     */
    @PostMapping("/author-connection")
    public ResponseEntity postAuthorConnectionRequest(
            @RequestBody SaffronModel authorsConnectionModel)
            throws ConfigDataResourceNotFoundException {
        try {
            AuthorTermConfiguration config = authorsConnectionModel.getConfiguration().authorTerm;
            Corpus corpus = CorpusTools.fromCollection(authorsConnectionModel.getInput().documents);
            List<DocumentTerm> docTerms = authorsConnectionModel.getInput().documentTermMapping;
            List<Term> terms = authorsConnectionModel.getInput().termsMapping;
            ConnectAuthorTerm cr = new ConnectAuthorTerm(config);
            Collection<AuthorTerm> authorTerms = cr.connectResearchers(terms, docTerms, corpus.getDocuments());
            return ResponseEntity.ok(authorTerms);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }

    /**
     * Author consolidation endpoint
     *
     * @param authorsSimilarityModel the SaffronModel
     * @return the response entity
     * @throws ConfigDataResourceNotFoundException the resource not found exception
     */
    @PostMapping("/author-similarity")
    public ResponseEntity postAuthorSimilarityRequest(
            @RequestBody SaffronModel authorsSimilarityModel)
            throws ConfigDataResourceNotFoundException {
        try {
            AuthorSimilarityConfiguration config = authorsSimilarityModel.getConfiguration().authorSim;
            List<AuthorTerm> authorTerms = authorsSimilarityModel.getInput().authorTerms;
            AuthorSimilarity ts = new AuthorSimilarity(config);
            return ResponseEntity.ok(ts.authorSimilarity(authorTerms, ""));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }
}
