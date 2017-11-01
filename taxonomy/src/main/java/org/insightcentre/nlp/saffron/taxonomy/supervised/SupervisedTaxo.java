package org.insightcentre.nlp.saffron.taxonomy.supervised;

import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Provides pairwise supervised predictions of the order of elements in a taxonomy.
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class SupervisedTaxo {
    private final Features features;
    private final Classifier classifier;
    private final ArrayList<Attribute> attributes;
    
    public SupervisedTaxo(TaxonomyExtractionConfiguration config, List<DocumentTopic> docTopics,
            Map<String, Topic> topicMap) throws IOException {
        this.features = Train.makeFeatures(config, docTopics, topicMap);
        this.classifier = readClassifier(config);
        this.attributes = Train.buildAttributes(features.featureNames());
    }
 
    public double predict(String top, String bottom) {
        
        final Instances instances = new Instances("saffron", attributes, 100);
        instances.setClassIndex(attributes.size() - 1);
        final Instance instance = Train.makeInstance(features.buildFeatures(top, bottom), 0);
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
        instances.add(instance);
        instance.setDataset(instances);
        try {
            return classifier.classifyInstance(instance);
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }

    private static Classifier readClassifier(TaxonomyExtractionConfiguration config) throws IOException {
        try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(config.modelFile.toFile()))) {
            final Classifier c;
            try {
                return (Classifier) ois.readObject();
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
