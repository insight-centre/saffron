package org.insightcentre.nlp.saffron.taxonomy.supervised;

import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import libsvm.svm;
import libsvm.svm_model;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 * Provides pairwise supervised predictions of the order of elements in a
 * taxonomy.
 *
 * @author John McCrae <john@mccr.ae>
 */
public class SupervisedTaxo {

    private final Features features;
    private final svm_model classifier;
    private final ArrayList<String> attributes;

    public SupervisedTaxo(TaxonomyExtractionConfiguration config, List<DocumentTopic> docTopics,
            Map<String, Topic> topicMap) throws IOException {
        this.features = Train.makeFeatures(config, docTopics, topicMap);
        this.classifier = readClassifier(config);
        this.attributes = Train.buildAttributes(features.featureNames());
    }

    protected SupervisedTaxo(Features features, svm_model classifier, ArrayList<String> attributes) {
        this.features = features;
        this.classifier = classifier;
        this.attributes = attributes;
    }
    
    

    public double predict(String top, String bottom) {

        final Train.Instance instance = Train.makeInstance(features.buildFeatures(top, bottom), 0);
        //if(features.names.length != featNames.length) {
        //    throw new RuntimeException("Classifier has wrong number of attributes. Model does not match trained");
        //}
        //if(!checkedFeatures) {
        //    for(int i = 0; i < featNames.length; i++) {
        //        if(!featNames[i].equals(features.names[i]._1 + "-" + features.names[i]._2)) {
        //            throw new RuntimeException("Feature names are not equal: " + featNames[i] + " vs " + features.names[i]);
        //        }
        //    }
        //    checkedFeatures = true;
        //}
        //assert(instance.numAttributes() == featNames.length + 1);
        try {
            double[] prob_estimates = new double[2];
            svm.svm_predict_probability(classifier, instance.x, prob_estimates);
            double sim = prob_estimates[0];
            return sim;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private static svm_model readClassifier(TaxonomyExtractionConfiguration config) throws IOException {
        return libsvm.svm.svm_load_model(config.modelFile.toFile().getAbsolutePath());
    }
}
