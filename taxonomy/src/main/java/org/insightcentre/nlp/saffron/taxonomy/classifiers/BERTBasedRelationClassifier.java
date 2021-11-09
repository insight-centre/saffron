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
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.data.TypedLink.Type;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.robrua.nlp.bert.Bert;




/**
 * BERT Based relation classifier. Provides an array of predictions
 *
 * @author Rajdeep Sarkar
 * @author Andy Donald
 * @author Bianca Pereira
 * @author Jamal Nasir
 */
public abstract class BERTBasedRelationClassifier implements MulticlassRelationClassifier<String>{
	
	private ComputationGraph net;

	private Bert bert;
	private final long sizeEmbeddings;

	private SimpleCache<String, float[]> simpleCache = new SimpleCache<>(10000);
	
	public final Map<TypedLink.Type,Integer> typeMap;

	/**
	 * Choose the correct instance according to the number of relations the user wishes to detect.
	 * Note that the Keras and BERT models need to fit the configuration provided.
	 *
	 * @param simpleMLPFilePath - the file path for the trained Keras model and weights
	 * @param bertModelFilePath - the file path for the BERT model
	 * @param numberOfRelations - the number of relations to be detected
	 *
	 *
	 * @return an instance of BERTBasedRelationClassifier according to the configuration provided
	 * @throws IOException
	 * @throws UnsupportedKerasConfigurationException
	 * @throws InvalidKerasConfigurationException
	 */
	public static BERTBasedRelationClassifier getInstance(String simpleMLPFilePath, String bertModelFilePath, int numberOfRelations)
			throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {
		 //TO DO
        // It should extract this information from the Keras model automatically

		if (numberOfRelations == 3) {
			return new BERTBasedRelationClassifier3Relations(simpleMLPFilePath, bertModelFilePath);
		} else if (numberOfRelations == 5) {
			return new BERTBasedRelationClassifier5Relations(simpleMLPFilePath, bertModelFilePath);
		} else {
			throw new InvalidValueException("The BERTBasedRelationClassifier only accepts 3 or 5 relations as parameters");
		}
	}


	/**
	 * Create a multi-relation classifier based on  BERT
	 * 
	 * @param simpleMLPFilePath - the file path for the trained Keras model and weights
	 * @param bertModelFilePath - the file path for the BERT model
	 * @param typeMap - the mapping of relations the classifier uses and their position in the output layer
	 * @param sizeEmbeddings - the size of the array representing the BERT embeddings
	 *
	 * @throws IOException
	 * @throws UnsupportedKerasConfigurationException
	 * @throws InvalidKerasConfigurationException
	 */
	protected BERTBasedRelationClassifier(String simpleMLPFilePath, String bertModelFilePath, Map<TypedLink.Type,Integer> typeMap,
			long sizeEmbeddings)
			throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

		this.typeMap = typeMap;
        net = KerasModelImport.importKerasModelAndWeights(simpleMLPFilePath);
        bert = Bert.load(new File(bertModelFilePath));
        this.sizeEmbeddings = sizeEmbeddings;
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

        INDArray features = Nd4j.zeros(1, 1, 2, sizeEmbeddings);

        for(int i=0; i<sizeEmbeddings; i++)
        {
            features.putScalar(0, 0, 0, i, embedding_source[i]);
            features.putScalar(0, 0, 1, i, embedding_target[i]);
        }

        INDArray[] prediction = this.net.output(features);

        double[] modelResults = prediction[0].toDoubleVector();
        Map<Type, Double> result = new HashMap<Type,Double>();

        for(Type relationType: typeMap.keySet()) {
        	result.put(relationType, modelResults[typeMap.get(relationType)]);
        }
        
        return result;
    }
}