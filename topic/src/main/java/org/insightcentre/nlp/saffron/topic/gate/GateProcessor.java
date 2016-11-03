package org.insightcentre.nlp.saffron.topic.gate;

import gate.Corpus;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ConditionalSerialAnalyserController;
import gate.creole.ResourceInstantiationException;
import gate.event.StatusListener;
import gate.jape.Batch;
import gate.util.GateException;
import java.net.URL;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class GateProcessor {
	private static ProcessingResource annotDelete_PR = null;
	private static ProcessingResource tokeniser_PR = null;
	private static ProcessingResource sentSplit_PR = null;
	private static ProcessingResource en_POSTagger_PR = null;
	private static ProcessingResource en_Morphology_PR = null;
	private static ProcessingResource transducer_PR = null;
	private static ProcessingResource topicCollector_PR = null;
	// private static = null;

	private ConditionalSerialAnalyserController controller = null;
	private Corpus corpus = null;
	private ProcessingResource gazetteer_PR;

    public GateProcessor() throws GateException {
        initializeController();
        initializeCorpus();
    }


    void setGazetteerListsURL(URL url) throws ResourceInstantiationException {
		gazetteer_PR.setParameterValue("listsURL", url);
		gazetteer_PR.reInit();
    }

	private void initializeController() throws GateException {

		controller = (ConditionalSerialAnalyserController) Factory.createResource(
				"gate.creole.ConditionalSerialAnalyserController", Factory.newFeatureMap(), Factory.newFeatureMap(),
				"TOPIC_EXTRACTOR_" + Gate.genSym());
		// gate.creole.annotdelete.AnnotationDeletePR
		annotDelete_PR = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "Expertise Mining Annotation Delete PR");
		controller.add(annotDelete_PR);

		// gate.creole.tokeniser.DefaultTokeniser
		tokeniser_PR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "Expertise Mining Tokeniser");
		controller.add(tokeniser_PR);

		// gate.creole.splitter.SentenceSplitter
		sentSplit_PR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "Expertise Mining Sentence Splitter");
		controller.add(sentSplit_PR);

		// gate.creole.POSTagger
		en_POSTagger_PR = (ProcessingResource) Factory.createResource("gate.creole.POSTagger", Factory.newFeatureMap(),
				Factory.newFeatureMap(), "Expertise Mining English POS Tagger");
		controller.add(en_POSTagger_PR);

		// gate.creole.morph.Morph
		en_Morphology_PR = (ProcessingResource) Factory.createResource("gate.creole.morph.Morph",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "English Morphology");
		en_Morphology_PR.setParameterValue("rootFeatureName", "root");
		controller.add(en_Morphology_PR);

		// gate.creole.gazetteer.DefaultGazetteer &
		// gate.creole.gazetteer.FlexibleGazetteer
		gazetteer_PR = (ProcessingResource) Factory.createResource(
				"gate.creole.gazetteer.DefaultGazetteer", Factory.newFeatureMap(), Factory.newFeatureMap(),
				"Expertise Mining Gazetteer POS Tagger");
        controller.add(gazetteer_PR);
          
		// gate.creole.Transducer
        FeatureMap i = Factory.newFeatureMap();
        i.put("grammarURL", this.getClass().getResource("/grammar/main.jape"));
		transducer_PR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", i,
				Factory.newFeatureMap(), "Expertise Mining Transducer");
		controller.add(transducer_PR);

        Gate.getCreoleRegister().registerComponent(TopicCollector.class);
        FeatureMap f2 = Factory.newFeatureMap();
        f2.put("topicAnnotationSetName", "Topic");
		topicCollector_PR = (ProcessingResource) Factory.createResource(
				"org.insightcentre.nlp.saffron.topic.gate.TopicCollector", f2, Factory.newFeatureMap(),
		        "Expertise Mining Topic Collector");
		controller.add(topicCollector_PR);

        for(ProcessingResource prL : controller.getPRs()) {
            prL.init();
        }
        controller.init();

	}

	private void initializeCorpus() throws ResourceInstantiationException {
		this.corpus = Factory.newCorpus("TopicExtractionCorpus");
		controller.setCorpus(corpus);
	}


    
    public ConditionalSerialAnalyserController getController() {
        return controller;
    }

    Corpus getCorpus() {
        return corpus;
    }

}
