package org.insightcentre.nlp.saffron.taxonomy.classifiers;

import java.io.IOException;

import com.robrua.nlp.bert.Bert;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;

import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;



/**
 * BERT Based relation classifier. Provides an array of predictions
 *
 * @author Andy Donald
 */
public class BERTBasedRelationClassifier {

    public double[] predict (String source, String target) throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {

        String simpleMlp = new ClassPathResource("model_test_bert_new_softmax.h5").getFile().getPath();
        ComputationGraph net = KerasModelImport.importKerasModelAndWeights(simpleMlp);
        Bert bert = Bert.load("com/robrua/nlp/easy-bert/bert-cased-L-12-H-768-A-12");

        float[] embedding_left = bert.embedSequence(source);
        float[] embedding_right = bert.embedSequence(target);

        INDArray features = Nd4j.zeros(1, 1, 2, 1024);

        for(int i=0; i<767; i++)
        {
            features.putScalar(0, 0, 0, i, embedding_left[i]);
            features.putScalar(0, 0, 1, i, embedding_right[i]);
        }

        INDArray[] prediction = net.output(features);

        return prediction[0].toDoubleVector();
    }

}