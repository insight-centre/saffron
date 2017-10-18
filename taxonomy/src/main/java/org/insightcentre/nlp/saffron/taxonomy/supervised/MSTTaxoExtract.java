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
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.taxonomy.graph.Edmonds;
/**
 * Extract a taxonomy by using a MST
 * @author John McCrae <john@mccr.ae>
 */
public class MSTTaxoExtract {
    
    
    private final SupervisedTaxo classifier;

    public MSTTaxoExtract(SupervisedTaxo classifier) {
        this.classifier = classifier;
    }
    
    
    public Taxonomy extractTaxonomy(List<DocumentTopic> docTopics, Map<String, Topic> topicMap) {
        final ArrayList<String> topics = new ArrayList<>(topicMap.keySet());
        final double[][] matrix = new double[topics.size()][topics.size()];
        String topNode = null;
                
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestOcc = Integer.MIN_VALUE;
        for(int i = 0; i < topics.size(); i++) {
            Topic t1 = topicMap.get(topics.get(i));
            for(int j = 0; j < topics.size(); j++) {
                matrix[i][j] = i == j ? 0 : classifier.predict(topics.get(i), topics.get(j));
            }
            if(t1.score > bestScore || (t1.score == bestScore && t1.occurrences > bestOcc)) {
                bestScore = t1.score;
                bestOcc = t1.occurrences;
                topNode = t1.topicString;
            }
        }
        WeightedGraph<String> graph = DenseWeightedGraph.from(topics, matrix);
        
        if(topNode == null) {
            throw new IllegalArgumentException("No topics for taxonomy construction");
        }
        final Weighted<Arborescence<String>> arbor = ChuLiuEdmonds.getMaxArborescence(graph, topNode);
        return buildTaxo(topNode, arbor);        
    }

    private Taxonomy buildTaxo(String node, Weighted<Arborescence<String>> arbor) {
        Map<String,List<String>> invertedArbor = new HashMap<>();
        for(ImmutableMap.Entry<String,String> e : arbor.val.parents.entrySet()) {
            if(!invertedArbor.containsKey(e.getValue())) {
                invertedArbor.put(e.getValue(), new ArrayList<String>());
            }
            invertedArbor.get(e.getValue()).add(e.getKey());
        }
        return buildTaxo(node, invertedArbor);
    }
    
    private Taxonomy buildTaxo(String node, Map<String, List<String>> tree) {
        List<Taxonomy> children = new ArrayList<>();
        List<String> edges = tree.get(node);
        if(edges != null) {
            for(String s : edges) {
                children.add(buildTaxo(s, tree));
            }
        }
        return new Taxonomy(node, children);
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
