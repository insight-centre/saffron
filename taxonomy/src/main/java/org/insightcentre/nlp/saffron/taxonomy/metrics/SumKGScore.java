package org.insightcentre.nlp.saffron.taxonomy.metrics;

import java.util.Map;

import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public class SumKGScore implements Score<TypedLink>{
	
	private final MulticlassRelationClassifier<String> classifier;
	public final Object2DoubleMap<TypedLink> scores = new Object2DoubleOpenHashMap<>();
	private final boolean enableSynonymyNormalisation;
	
	public SumKGScore(MulticlassRelationClassifier<String> classifier, boolean enableSynonymyNormalisation) {
	    this.classifier = classifier;
	    this.enableSynonymyNormalisation = enableSynonymyNormalisation;
	}
	
	@Override
	public double deltaScore(final TypedLink tl) {
	    if (!scores.containsKey(tl)) {
	    	Map<TypedLink.Type, Double> prediction = classifier.predict(tl.getSource(), tl.getTarget());
	    	for(TypedLink.Type relationType : prediction.keySet()) {
				TypedLink deepCopy = new TypedLink(tl);

				if (this.enableSynonymyNormalisation &&
						relationType.equals(TypedLink.Type.synonymy)) {
					deepCopy.setType(relationType);
					double synonymyScore = normaliseSynonymyScores(deepCopy, prediction.get(TypedLink.Type.synonymy));
					scores.put(deepCopy, synonymyScore);
				} else {

					deepCopy.setType(relationType);
					scores.put(deepCopy,prediction.get(relationType));
				}
	    	}
	    }
	    return scores.getDouble(tl);
	}

	private double normaliseSynonymyScores(TypedLink tl, double currentSynonymyScore) {
		
		double normalisedScore;
		TypedLink deepCopyReverse = new TypedLink(tl.getTarget(), tl.getSource(), tl.getType());
		double otherSynonymyScore;
		if (scores.containsKey(deepCopyReverse)) {
			otherSynonymyScore = scores.getDouble(deepCopyReverse);
			normalisedScore = (currentSynonymyScore + otherSynonymyScore)*0.5;
			scores.replace(deepCopyReverse, normalisedScore);
		}
		else {
			normalisedScore = currentSynonymyScore;
		}



		return normalisedScore;
	}

	@Override
	public Score<TypedLink> next(TypedLink link, Solution soln) {
	    return this;
	}
}
