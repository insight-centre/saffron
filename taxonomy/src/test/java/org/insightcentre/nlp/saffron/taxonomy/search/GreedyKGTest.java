package org.insightcentre.nlp.saffron.taxonomy.search;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.metrics.SumKGScore;
import org.insightcentre.nlp.saffron.taxonomy.search.testing.KnowledgeGraph;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;
import org.junit.Test;

public class GreedyKGTest {
	
	private static class TestMultiRelationClassifier implements MulticlassRelationClassifier<String> {

        public TestMultiRelationClassifier() throws IOException {
        }

        @Override
        public Map<TypedLink.Type, Double> predict(String source, String target) {
        	Map<TypedLink.Type, Double> predictions = new HashMap<TypedLink.Type,Double>();
        	predictions.put(TypedLink.Type.hypernymy, 0.0);
    		predictions.put(TypedLink.Type.hyponymy, 0.0);
    		predictions.put(TypedLink.Type.meronymy, 0.0);
    		predictions.put(TypedLink.Type.synonymy, 0.0);
    		predictions.put(TypedLink.Type.other, 1.0);
        	
        	switch(source) {
        		case "thing":
        			if (target.equals("vehicles") || target.equals("wheel")) {
        				predictions.put(TypedLink.Type.hypernymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "vehicles":
        			if (target.equals("train") || target.equals("automobile")) {
        				predictions.put(TypedLink.Type.hypernymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("thing")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "wheel":
        			if (target.equals("car wheel")) {
        				predictions.put(TypedLink.Type.hypernymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("thing")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("automobile")) {
        				predictions.put(TypedLink.Type.meronymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "train":
        			if (target.equals("vehicles")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "automobile":
        			if (target.equals("bus") || target.equals("coach") || target.equals("car")) {
        				predictions.put(TypedLink.Type.hypernymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("vehicles")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "car wheel":
        			if (target.equals("wheel")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("car")) {
        				predictions.put(TypedLink.Type.meronymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "bus":
        		case "coach":
        			if (target.equals("automobile")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			} else if (target.equals("coach") || target.equals("coach")) {
        				predictions.put(TypedLink.Type.synonymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        		case "car":
        			if (target.equals("automobile")) {
        				predictions.put(TypedLink.Type.hyponymy, 1.0);
        				predictions.put(TypedLink.Type.other, 0.0);
        			}
        			break;
        	}

    		return predictions;
        }
        
    }
    
    private void addTerm(HashMap<String, Term> terms, String t, double score) {
        terms.put(t, new Term(t, 0, 0, score, Collections.EMPTY_LIST, Status.none.toString()));
    }
    
    /**
     * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
     */
    @Test
    public void testExtractTaxonomy() throws Exception {
        HashMap<String, Term> terms = new HashMap<>();
        addTerm(terms, "thing", 0.0);
        addTerm(terms, "vehicles", 0.0);
        addTerm(terms, "train", 0.0);
        addTerm(terms, "automobile", 0.0);
        addTerm(terms, "bus", 0.0);
        addTerm(terms, "coach", 0.0);
        addTerm(terms, "car", 0.0);
        addTerm(terms, "wheel", 0.0);
        addTerm(terms, "car wheel", 0.0);
        
        GreedyKG instance = new GreedyKG(new SumKGScore(new TestMultiRelationClassifier()));
        KnowledgeGraph result = instance.extractKnowledgeGraph(terms);
        System.out.println(result.getTaxonomy());
        System.out.println(result.getPartonomy().get(0));
        assertEquals("thing", result.getTaxonomy().root);
        assertEquals(2, result.getTaxonomy().children.size());
        assertEquals(2, result.getPartonomy().size());
        assertEquals(1,result.getSynonymyClusters().size());
        
    }
    
        /**
     * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
     */
    @Test
    public void testExtractTaxonomyWithBlackWhiteList() throws Exception {
        HashMap<String, Term> terms = new HashMap<>();
        addTerm(terms, "thing", 0.0);
        addTerm(terms, "vehicles", 0.0);
        addTerm(terms, "train", 0.0);
        addTerm(terms, "automobile", 0.0);
        addTerm(terms, "bus", 0.0);
        addTerm(terms, "coach", 0.0);
        addTerm(terms, "car", 0.0);
        addTerm(terms, "wheel", 0.0);
        addTerm(terms, "car wheel", 0.0);
        
        Set<TypedLink> whiteList = new HashSet<>();
        Set<TypedLink> blackList = new HashSet<>();
        whiteList.add(new TaxoLink("thing", "vehicles"));
        whiteList.add(new TypedLink("wheel", "automobile",TypedLink.Type.meronymy));
        blackList.add(new TaxoLink("automobile", "coach"));
        blackList.add(new TypedLink("car wheel", "car",TypedLink.Type.meronymy));
        
        GreedyKG instance = new GreedyKG(new SumKGScore(new TestMultiRelationClassifier()));
        KnowledgeGraph result = instance.extractKnowledgeGraphWithDenialAndAllowanceList(terms, whiteList, blackList);
        
        assertEquals(2, result.getTaxonomy().children.size());
        assertEquals(1, result.getPartonomy().size());
        assertEquals(1,result.getSynonymyClusters().size());
        
        assert(result.getTaxonomy().children.stream().anyMatch((Taxonomy t) -> t.root.equals("wheel") && t.status == Status.none));
        assert(result.getTaxonomy().children.stream().anyMatch((Taxonomy t) -> t.root.equals("vehicles") && t.status == Status.accepted));
        assert(result.getTaxonomy().children.stream().anyMatch((Taxonomy t) -> t.hasDescendent("vehicles")));
                
        assert(result.getPartonomy().get(0).children.stream().anyMatch((Taxonomy t) -> t.root.equals("automobile") && t.status == Status.accepted));
        assert(result.getPartonomy().get(0).children.stream().noneMatch((Taxonomy t) -> t.root.equals("car wheel") && t.hasDescendent("car")));
        
    }

}
