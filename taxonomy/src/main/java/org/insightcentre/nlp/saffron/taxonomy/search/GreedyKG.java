package org.insightcentre.nlp.saffron.taxonomy.search;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.metrics.Score;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public class GreedyKG implements KGSearch{

	private final Score<TypedLink> emptyScore;
	private final KnowledgeGraphExtractionConfiguration config;
	private SaffronListener log;
	
	public GreedyKG(Score<TypedLink> score, KnowledgeGraphExtractionConfiguration config, SaffronListener log) {
		this.emptyScore = score;
		this.config = config;
		this.log = log;
	}

	@Override
	public KnowledgeGraph extractKnowledgeGraphWithDenialAndAllowanceList(Map<String, Term> termMap, Set<TypedLink> allowanceList,
			Set<TypedLink> denialList) {
		
		log.log(LocalDateTime.now().toString() + " - Starting Greedy KG");
		//1 - Verify edge cases
        if(termMap.size() == 0) {
            return KnowledgeGraph.getEmptyInstance();
        } else if(termMap.size() == 1) {
            // It is not possible to construct a KG from 1 term
            return KnowledgeGraph.getSingleTermInstance(termMap.keySet().iterator().next());
        }

        log.log(LocalDateTime.now().toString() + " - GreedyKG - Generating candidate pairs");
        //2 - Build candidate list of possible links
        GreedyKGList candidates = new GreedyKGList(createCandidateLinks(termMap, allowanceList, denialList));

        log.log(LocalDateTime.now().toString() + " - GreedyKG  - Generating Initial Solution");
        //3 - Create solution based on links on allowance list
        Pair<KnowledgeGraphSolution, Score<TypedLink>> result = generateInitialSolution(termMap, allowanceList);
        
        log.log(LocalDateTime.now().toString() + " - GreedyKG  - Starting Search");
        //4 - Greedy Search for the final solution 
        SOLN_LOOP:
        while(!candidates.isEmpty()) {//TODO: Ideally this loop should stop as soon as a "complete" solution is found
        	
        	//5 - Calculate how much each link contributes to improving the score of the current Knowledge Graph
                final Pair<KnowledgeGraphSolution, Score<TypedLink>> result0 = result;
            candidates.scoreAndSort(tl -> result0.getValue().deltaScore(tl));
            
            //7 - Choose which candidate will enter in the current Knowledge Graph
            while (!candidates.isEmpty()) {
            	
            	//8 - Create a single solution with the highest ranked candidate
                double score = candidates.getScore(0);
            	TypedLink candidate = candidates.remove(0);
                KnowledgeGraphSolution soln2 = result.getKey().add(candidate,
                        termMap.get(candidate.getSource()).getScore(),
                        termMap.get(candidate.getTarget()).getScore(),
                        score, false);
                //9 - If such solution is feasible, then update the current Knowledge Graph and go back to step 5 
                if (soln2 != null) {
                	Score<TypedLink> newScore = result.getValue().next(candidate, soln2);
                    result = new MutablePair<KnowledgeGraphSolution, Score<TypedLink>>(soln2,newScore);
                    
                    //Prune the list of candidates by removing those that will never be considered by a new 
                    // partial solution
                    soln2.pruneCandidateList(candidates, candidate);
                    continue SOLN_LOOP;
                }
            }
        }
        
        log.log(LocalDateTime.now().toString() + " - Greedy KG finished");
        //If the solution is not complete, even after considering all candidates then no solution was found
        if(!result.getKey().isComplete()) {// Complete = all terms must to appear at least in the taxonomy (except synonyms)
        	for (TypedLink candidate : candidates) {
                log.log("No solution found with candidate: " + candidate.toString());
            }
            throw new RuntimeException("Failed to find solution");
        }        
        
        return result.getKey().getKnowledgeGraph();
	}

	private Pair<KnowledgeGraphSolution, Score<TypedLink>> generateInitialSolution(
			Map<String, Term> termMap, Set<TypedLink> allowanceList) {
		
		KnowledgeGraphSolution soln = KnowledgeGraphSolution.empty(termMap.keySet(), this.config.synonymyThreshold, this.config.meronomyThreshold);
        
        Score<TypedLink> score = this.emptyScore;
        for (TypedLink sp : allowanceList) {
            if (termMap.get(sp.getSource()) != null && termMap.get(sp.getTarget()) != null) {
                soln = soln.add(sp,
                        termMap.get(sp.getSource()).getScore(),
                        termMap.get(sp.getTarget()).getScore(),
                        1.0, true);
                score = score.next(sp, soln);
            }
        }
        return new MutablePair<KnowledgeGraphSolution, Score<TypedLink>>(soln, score);
	}

	private ArrayList<TypedLink> createCandidateLinks(Map<String, Term> termMap, 
			Set<TypedLink> allowanceList, Set<TypedLink> denialList) {
		
		ArrayList<TypedLink> candidates = new ArrayList<TypedLink>();
		for (String t1 : termMap.keySet()) {
            for (String t2 : termMap.keySet()) {
        		// Assumes there are no self-loops (e.g. group 'is a' group, or group 'part of' group)
        		if (!t1.equals(t2)) {
        			for(TypedLink.Type relationType: TypedLink.Type.values()) {
        				candidates.add(new TypedLink(t1, t2, relationType));
        			}
                }
            }
        }
        candidates.removeAll(denialList);
        candidates.removeAll(allowanceList);
        
        return candidates;
	}
}
