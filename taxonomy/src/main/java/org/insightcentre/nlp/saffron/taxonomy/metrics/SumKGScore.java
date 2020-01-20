package org.insightcentre.nlp.saffron.taxonomy.metrics;

import java.util.Map;

import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public class SumKGScore implements Score<TypedLink>{
	
	private final MulticlassRelationClassifier<String> classifier;
	private final Object2DoubleMap<TypedLink> scores = new Object2DoubleOpenHashMap<>();
	
	public SumKGScore(MulticlassRelationClassifier<String> classifier) {
	    this.classifier = classifier;
	}
	
	@Override
	public double deltaScore(final TypedLink tl) {
	    if (!scores.containsKey(tl)) {
	    	Map<TypedLink.Type, Double> prediction = classifier.predict(tl.getSource(), tl.getTarget());
	    	for(TypedLink.Type relationType : prediction.keySet()) {
	    		TypedLink deepCopy = new TypedLink(tl);
	    		deepCopy.setType(relationType);
	    		
	    		scores.put(deepCopy,prediction.get(relationType));
	    	}
	    }
	    return scores.getDouble(tl);
	}
	
	@Override
	public Score<TypedLink> next(TypedLink link, Solution soln) {
	    return this;
	}
}
