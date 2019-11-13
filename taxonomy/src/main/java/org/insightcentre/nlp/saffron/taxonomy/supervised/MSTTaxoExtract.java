package org.insightcentre.nlp.saffron.taxonomy.supervised;

import com.google.common.collect.ImmutableMap;
import edu.cmu.cs.ark.cle.Arborescence;
import edu.cmu.cs.ark.cle.ChuLiuEdmonds;
import edu.cmu.cs.ark.cle.graph.DenseWeightedGraph;
import edu.cmu.cs.ark.cle.graph.WeightedGraph;
import edu.cmu.cs.ark.cle.util.Weighted;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxoLink;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySearch;
/**
 * Extract a taxonomy by using a MST
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class MSTTaxoExtract implements TaxonomySearch {
    
    
    private final SupervisedTaxo classifier;

    public MSTTaxoExtract(SupervisedTaxo classifier) {
        this.classifier = classifier;
    }

    @Override
    public Taxonomy extractTaxonomy(Map<String, Term> termMap) {
        final ArrayList<String> terms = new ArrayList<>(termMap.keySet());
        final double[][] matrix = new double[terms.size()][terms.size()];
        String topNode = null;
                
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestOcc = Integer.MIN_VALUE;
        for(int i = 0; i < terms.size(); i++) {
            Term t1 = termMap.get(terms.get(i));
            for(int j = 0; j < terms.size(); j++) {
                matrix[i][j] = i == j ? 0 : classifier.predict(terms.get(i), terms.get(j));
            }
            if(t1.getScore() > bestScore || (t1.getScore() == bestScore && t1.getOccurrences() > bestOcc)) {
                bestScore = t1.getScore();
                bestOcc = t1.getOccurrences();
                topNode = t1.getString();
            }
        }
        System.err.println("Built graph");
        WeightedGraph<String> graph = DenseWeightedGraph.from(terms, matrix);
        
        if(topNode == null) {
            throw new IllegalArgumentException("No terms for taxonomy construction");
        }
        System.err.println("Starting Chu-Liu Edmonds");
        final Weighted<Arborescence<String>> arbor = ChuLiuEdmonds.getMaxArborescence(graph, topNode);
        System.err.println("Finished... building taxonomy");
        return buildTaxo(topNode, arbor, termMap, null, terms, matrix);        
    }

    @Override
    public Taxonomy extractTaxonomyWithBlackWhiteList(Map<String, Term> termMap, Set<TaxoLink> whiteList, Set<TaxoLink> blackList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
    
    private Taxonomy buildTaxo(String node, Weighted<Arborescence<String>> arbor,
            Map<String, Term> termMap, String parent,
            ArrayList<String> term, double[][] matrix) {
        Map<String,List<String>> invertedArbor = new HashMap<>();
        for(ImmutableMap.Entry<String,String> e : arbor.val.parents.entrySet()) {
            if(!invertedArbor.containsKey(e.getValue())) {
                invertedArbor.put(e.getValue(), new ArrayList<String>());
            }
            invertedArbor.get(e.getValue()).add(e.getKey());
        }
        
        return buildTaxo(node, invertedArbor, termMap, parent, term, matrix);
    }
    
    private Taxonomy buildTaxo(String node, Map<String, List<String>> tree,
            Map<String, Term> termMap, String parent,
            ArrayList<String> term, double[][] matrix) {
        List<Taxonomy> children = new ArrayList<>();
        List<String> edges = tree.get(node);
        if(edges != null) {
            for(String s : edges) {
                children.add(buildTaxo(s, tree, termMap, parent, term, matrix));
            }
        }
        double linkScore = parent == null ? Double.NaN : matrix[term.indexOf(parent)][term.indexOf(node)];
        return new Taxonomy(node, termMap.get(node).getScore(), linkScore, "", "", children, Status.none);
    }
    
    /*public Taxonomy extractTaxonomy(List<DocumentTopic> docTopics, Map<String, Topic> topicMap) {
        List<String> topics = new ArrayList<>(topicMap.keySet());
        Edmonds.Node topNode = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestOcc = Integer.MIN_VALUE;
        HashMap<String,Edmonds.Node> nodeMap = new HashMap<>();
        for(Topic s : topicMap.values()) {
            final Edmonds.Node node = new Edmonds.Node(s.topicString);
            nodeMap.put(s.topicString, node);
            if(s.score > bestScore || (s.score == bestScore && s.occurrences > bestOcc)) {
                bestScore = s.score;
                bestOcc = s.occurrences;
                topNode = node;
            }
        }
        Edmonds.AdjacencyList list = new Edmonds.AdjacencyList();
        for(int i = 0; i < topics.size(); i++) {
            for(int j = 0; j < topics.size(); j++) {
                if(i != j) {
                    list.addEdge(nodeMap.get(topics.get(i)),
                            nodeMap.get(topics.get(j)),
                            -classifier.predict(topics.get(i), topics.get(j)));
                }
                    
            }
        }
        System.err.println(list.size());
        Edmonds ed = new Edmonds();
        Edmonds.AdjacencyList mst = ed.getMinBranching(topNode, list);
        System.err.println(mst.size());
        return buildTaxo(topNode, mst);        
    }

    private Taxonomy buildTaxo(Edmonds.Node node, Edmonds.AdjacencyList mst) {
        List<Taxonomy> children = new ArrayList<>();
        List<Edmonds.Edge> edges = mst.getAdjacent(node);
        if(edges != null) {
            for(Edmonds.Edge e : edges) {
                children.add(buildTaxo(e.getTo(), mst));
            }
        }
        return new Taxonomy((String)node.getValue(),children);
    }*/
}
