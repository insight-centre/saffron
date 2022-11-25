package org.insightcentre.nlp.saffron.taxonomy.supervised;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.ModelFactory;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.SaffronModel;
import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.taxonomy.classifiers.BERTBasedRelationClassifier;
import org.insightcentre.nlp.saffron.taxonomy.extract.KGExtraction;
import org.insightcentre.nlp.saffron.taxonomy.extract.KGExtractionUtils;
import org.insightcentre.nlp.saffron.taxonomy.search.KGSearch;
import org.insightcentre.nlp.saffron.taxonomy.extract.ConvertKGToRDF;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySearch;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TaxonomyExtractionController {
    public static final String SKOS = "http://www.w3.org/2004/02/skos/core#";

    /**
     * Term Extraction endpoint for extracting terms from a given corpus.
     *
     * @param input the SaffronModel
     * @return the response entity
     * @throws ConfigDataResourceNotFoundException the resource not found exception
     */
    @PostMapping("/taxonomy-extraction")
    public ResponseEntity postRequest(
            @RequestBody SaffronModel input)
            throws ConfigDataResourceNotFoundException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<DocumentTerm> docTerms = input.getInput().documentTermMapping;
            List<Term> terms = input.getInput().termsMapping;
            List<TermTerm> termsSimilarity = input.getInput().termTerms;
            Map<String, Term> termMap = loadMap(terms, mapper, new DefaultSaffronListener());
            TaxonomyExtractionConfiguration config = input.getConfiguration().taxonomy;
            System.out.println(config);
            Model model = mapper.readValue(config.modelFile.toFile(), Model.class);
            SupervisedTaxo supTaxo = new SupervisedTaxo(docTerms, termMap, model);
            TaxonomySearch search = TaxonomySearch.create(config.search, supTaxo, termMap.keySet());
            final Taxonomy graph = search.extractTaxonomy(termMap);
            if (config.returnRDF) {
                KGExtraction kgExtraction = new KGExtraction();
                KnowledgeGraph kgTaxo = new KnowledgeGraph();
                kgTaxo.setTaxonomy(graph);
                KGExtractionUtils kgExtractionUtils = KGExtractionUtils.fromMemory(kgTaxo, docTerms, terms, termsSimilarity);
                org.apache.jena.rdf.model.Model kg = ModelFactory.createDefaultModel();
                for(Term term : terms) {
                    kgExtraction.getHyponyms(kg, "http://saffron.insight-centre.org", graph, term, kgExtractionUtils);
                }
                kg.setNsPrefix("skos", SKOS);
                StringWriter stringWriter = new StringWriter();
                kg.write(stringWriter);
                return ResponseEntity.ok(stringWriter.toString());
            } else {
                return ResponseEntity.ok(graph);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }


    /**
     * Term Extraction endpoint for extracting terms from a given corpus.
     *
     * @param input the SaffronModel
     * @return the response entity
     * @throws ConfigDataResourceNotFoundException the resource not found exception
     */
    @PostMapping("/kg-extraction")
    public ResponseEntity postKnowledgeGraphExtraction(
            @RequestBody SaffronModel input)
            throws ConfigDataResourceNotFoundException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            KnowledgeGraphExtractionConfiguration kgConfig = input.getConfiguration().kg;
            TaxonomyExtractionConfiguration taxonomyExtractionConfiguration = input.getConfiguration().taxonomy;
            List<Term> terms = input.getInput().termsMapping;

            Map<String, Term> termMap = loadMap(terms, mapper, new DefaultSaffronListener());
            BERTBasedRelationClassifier relationClassifier = BERTBasedRelationClassifier.getInstance(
                    kgConfig.kerasModelFile.getResolvedPath(), kgConfig.bertModelFile.getResolvedPath(), kgConfig.numberOfRelations);

            KGSearch search = KGSearch.create(taxonomyExtractionConfiguration.search, kgConfig, relationClassifier, termMap.keySet());
            final KnowledgeGraph graph = search.extractKnowledgeGraph(termMap, relationClassifier.typeMap.keySet());
            ObjectMapper resultMapper = new ObjectMapper();
            String json = resultMapper.writeValueAsString(graph);

            if (kgConfig.returnRDF) {
                org.apache.jena.rdf.model.Model model = ConvertKGToRDF.convertToRDF("http://saffron.insight-centre.org", graph);
                StringWriter stringWriter = new StringWriter();
                model.write(stringWriter);
                System.out.print(stringWriter);
                return ResponseEntity.ok(stringWriter.toString());
            } else {
                return ResponseEntity.ok(json);
            }


        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(e);
        }
    }

    public static Map<String, Term> loadMap(List<Term> terms, ObjectMapper mapper, SaffronListener log) throws IOException {
        Map<String, Term> tMap = new HashMap<>();
        for (Term term : terms) {
            tMap.put(term.getString(), term);
        }
        return tMap;
    }

}
