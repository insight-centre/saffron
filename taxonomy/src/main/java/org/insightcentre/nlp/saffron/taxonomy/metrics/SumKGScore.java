package org.insightcentre.nlp.saffron.taxonomy.metrics;

import java.time.LocalDateTime;
import java.util.Map;

import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public class SumKGScore implements Score<TypedLink>{
	
	private final MulticlassRelationClassifier<String> classifier;
	private final Object2DoubleMap<TypedLink> scores = new Object2DoubleOpenHashMap<>();
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

				if (config.enableSynonmyNormalisation) {
					Double synonmyScore = normaliseSynonmyScores(tl, prediction, relationType);
					deepCopy.setType(relationType);
					scores.put(deepCopy,synonmyScore);
				} else {

					deepCopy.setType(relationType);
					scores.put(deepCopy,prediction.get(relationType));
				}

	    	}
	    }
	    return scores.getDouble(tl);
	}

	private Double normaliseSynonmyScores(TypedLink tl, Map<TypedLink.Type, Double> prediction, TypedLink.Type relationType) {
		Double synonmyScore = 0.0;
		if (relationType.equals(TypedLink.Type.synonymy)) {
            TypedLink deepCopyReverse = new TypedLink(tl.getTarget(), tl.getSource(), tl.getType());
            Double synonmyScoreA = prediction.get(relationType);
            Double synonmyScoreB = 0.0;

            if (scores.containsKey(deepCopyReverse)) {
                synonmyScoreB = scores.getDouble(deepCopyReverse);
                synonmyScore = (synonmyScoreA + synonmyScoreB)*0.5;
                scores.replace(deepCopyReverse, synonmyScore);
            }
            else {
                synonmyScore = synonmyScoreA;
            }

        }
		return synonmyScore;
	}

	@Override
	public Score<TypedLink> next(TypedLink link, Solution soln) {
	    return this;
	}
}
