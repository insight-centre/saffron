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
	private final KnowledgeGraphExtractionConfiguration config;
	
	public SumKGScore(MulticlassRelationClassifier<String> classifier, KnowledgeGraphExtractionConfiguration config) {
	    this.classifier = classifier;
	    this.config = config;
	}
	
	@Override
	public double deltaScore(final TypedLink tl) {
	    if (!scores.containsKey(tl)) {
	    	Map<TypedLink.Type, Double> prediction = classifier.predict(tl.getSource(), tl.getTarget());
	    	for(TypedLink.Type relationType : prediction.keySet()) {
				TypedLink deepCopy = new TypedLink(tl);

				if (config.enableSynonymyNormalisation &&
						relationType.equals(TypedLink.Type.synonymy)) {
					deepCopy.setType(relationType);
					double synonymyScore = normaliseSynonymyScores(deepCopy, prediction, relationType);
					scores.put(deepCopy, synonymyScore);
				} else {

					deepCopy.setType(relationType);
					scores.put(deepCopy,prediction.get(relationType));
				}
	    	}
	    }
	    return scores.getDouble(tl);
	}

	private double normaliseSynonymyScores(TypedLink tl, Map<TypedLink.Type, Double> prediction, TypedLink.Type relationType) {
		double synonymyScore;
		TypedLink deepCopyReverse = new TypedLink(tl.getTarget(), tl.getSource(), tl.getType());
		double synonymyScoreA = prediction.get(relationType);
		double synonymyScoreB;
		if (scores.containsKey(deepCopyReverse)) {
			synonymyScoreB = scores.getDouble(deepCopyReverse);
			synonymyScore = (synonymyScoreA + synonymyScoreB)*0.5;
			scores.replace(deepCopyReverse, synonymyScore);
		}
		else {
			synonymyScore = synonymyScoreA;
		}



		return synonymyScore;
	}

	@Override
	public Score<TypedLink> next(TypedLink link, Solution soln) {
	    return this;
	}
}
