package org.insightcentre.nlp.saffron.taxonomy.classifiers;

import java.io.IOException;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.robrua.nlp.bert.Bert;

/**
 * BERT Based relation classifier. Provides an array of predictions
 *
 * @author Rajdeep Sarkar
 * @author Andy Donald
 */
public class BERTBasedRelationClassifier {
	
	private ComputationGraph net;
	private Bert bert;
	
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
        bert = Bert.load(bertModelFilePath);
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
    public double[] predict (String source, String target) 
    		throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        float[] embedding_source = this.bert.embedSequence(source);
        float[] embedding_target = this.bert.embedSequence(target);

        INDArray features = Nd4j.zeros(1, 1, 2, 1024);

        for(int i=0; i<767; i++)
        {
            features.putScalar(0, 0, 0, i, embedding_source[i]);
            features.putScalar(0, 0, 1, i, embedding_target[i]);
        }

        INDArray[] prediction = this.net.output(features);

        return prediction[0].toDoubleVector();
    }
}