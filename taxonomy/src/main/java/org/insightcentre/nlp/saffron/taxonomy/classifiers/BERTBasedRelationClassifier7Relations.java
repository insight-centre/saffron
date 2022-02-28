package org.insightcentre.nlp.saffron.taxonomy.classifiers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.insightcentre.nlp.saffron.data.TypedLink;

/**
 * BERT Based relation classifier. Provides an array of predictions
 * 
 * Introduced in version 4.0.0-rc2. Outputs Knowledge Graph with following relations:
 * 
 * - isA
 * - meronymy (partOf)
 * - synonymy
 * - atLocation
 * - usedFor
 * - hasPrerequisite
 * 
 * Requires the correct BERT and KERAS models
 *
 * @author Bianca Pereira
 */
public class BERTBasedRelationClassifier7Relations extends BERTBasedRelationClassifier{
	
	private static final long SIZE_EMBEDDINGS = 768;
	private static final Map<TypedLink.Type, Integer> relationMap;
	static {
		Map<TypedLink.Type, Integer> temp = new HashMap<TypedLink.Type, Integer>();
		temp.put(TypedLink.Type.hypernymy, 0);
		temp.put(TypedLink.Type.synonymy, 1);
		temp.put(TypedLink.Type.meronymy, 2);
		temp.put(TypedLink.Type.atLocation, 3);
		temp.put(TypedLink.Type.usedFor, 4);
		temp.put(TypedLink.Type.hasPrerequisite, 5);
		temp.put(TypedLink.Type.other, 6);
		
		relationMap = Collections.unmodifiableMap(temp);
	}
	
	public BERTBasedRelationClassifier7Relations(String simpleMLPFilePath, String bertModelFilePath)
			throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {
		
		super(simpleMLPFilePath, bertModelFilePath, relationMap, SIZE_EMBEDDINGS);
	}
}