package org.insightcentre.nlp.saffron.taxonomy.classifiers;

import org.insightcentre.nlp.saffron.util.SimpleCache;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.insightcentre.nlp.saffron.data.TypedLink.Type;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.robrua.nlp.bert.Bert;




/**
 * BERT Based relation classifier. Provides an array of predictions
 *
 * @author Rajdeep Sarkar
 * @author Andy Donald
 */
public class BERTBasedRelationClassifier implements MulticlassRelationClassifier<String>{
	
	private ComputationGraph net;

	private Bert bert;

	private SimpleCache<String, float[]> simpleCache = new SimpleCache<>(10000);
	
	/**
	 * Create a multi-relation classifier based on  BERT
	 * 
	 * @param simpleMLPFilePath - the file path for the trained Keras model and weights
	 * @param bertModelFilePath - the file path for the BERT model
	 * 
	 * @throws IOException
	 * @throws UnsupportedKerasConfigurationException
	 * @throws InvalidKerasConfigurationException
	 */
	public BERTBasedRelationClassifier(String simpleMLPFilePath, String bertModelFilePath) 
			throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {
		 
        net = KerasModelImport.importKerasModelAndWeights(simpleMLPFilePath);
        bert = Bert.load(new File(bertModelFilePath));
	}

	/**
	 * Predicts the probability of a set of relationships between a pair of terms 
	 * @param source - the source term string
	 * @param target - the target term string
	 * @return an array of probabilities for a set of relations (the specific relations depend on the model used)
	 * 
	 * @throws IOException
	 * @throws UnsupportedKerasConfigurationException
	 * @throws InvalidKerasConfigurationException
	 */
    public Map<Type, Double> predict(String source, String target) {


		float[] embedding_source = simpleCache.get(source, e -> this.bert.embedSequence(source));
		float[] embedding_target = simpleCache.get(target, e -> this.bert.embedSequence(target));

        INDArray features = Nd4j.zeros(1, 1, 2, 1024);

        for(int i=0; i<767; i++)
        {
            features.putScalar(0, 0, 0, i, embedding_source[i]);
            features.putScalar(0, 0, 1, i, embedding_target[i]);
        }

        INDArray[] prediction = this.net.output(features);

        double[] modelResults = prediction[0].toDoubleVector();
        Map<Type, Double> result = new HashMap<Type,Double>();

        result.put(Type.hypernymy, modelResults[0]);
        result.put(Type.synonymy, modelResults[1]);
        result.put(Type.meronymy, modelResults[2]);
        result.put(Type.other, modelResults[3]);
        result.put(Type.hyponymy, modelResults[4]);
        
        return result;
    }
}