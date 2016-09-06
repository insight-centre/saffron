/*
 *
 * GateProcessor.java, provides keyword/keyphrase extraction as a Java program/API
 * Copyright (C) 2008  Alexander Schutz
 * National University of Ireland, Galway
 * Digital Enterprise Research Institute
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package org.insightcentre.nlp.saffron.topicextraction.topicextractor.gate;

import gate.Corpus;
import gate.Factory;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ConditionalSerialAnalyserController;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Initializes a GATE Processing Pipeline (PP), which includes initializing the
 * necessary Processing Resources (PRs) and setting the affiliated corpus.
 * Encapsulates 2 GATE Resources -- SerialAnalyserController and Corpus, for
 * convenience reasons when providing more than 1 GateProcessor in a processor
 * pool (for multiple simultaneous requests)
 * <p>
 * The PP has been chosen to be of Conditional nature, as originally language
 * specific PRs are meant to be loaded, i.e. a German part-of-speech tagger in
 * case the document in question was found to be in German.
 * <p>
 * The running strategies for the PRs are set during initialization, and basic
 * logging is being provided.
 * 
 * @author alesch
 * 
 */
class GateProcessor {
	private static ProcessingResource annotDelete_PR = null;
	private static ProcessingResource tokeniser_PR = null;
	private static ProcessingResource sentSplit_PR = null;
	private static ProcessingResource en_POSTagger_PR = null;
	private static ProcessingResource en_Morphology_PR = null;
	private static ProcessingResource transducer_PR = null;
	private static ProcessingResource topicCollector_PR = null;
	// private static = null;

	private static Logger logger = Logger.getLogger(GateProcessor.class.getName());

	private ConditionalSerialAnalyserController controller = null;
	private Corpus corpus = null;
	private ProcessingResource gazetteer_PR;

	public GateProcessor() throws GateException {
		initializeController();
		initializeCorpus();
	}
	
	public void setGazetteerListsURL(URL url) throws ResourceInstantiationException {
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
		logger.log(Level.INFO, "added " + annotDelete_PR.getName() + " " + annotDelete_PR.getClass().getCanonicalName());

		// gate.creole.tokeniser.DefaultTokeniser
		tokeniser_PR = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "Expertise Mining Tokeniser");
		controller.add(tokeniser_PR);
		logger.log(Level.INFO, "added " + tokeniser_PR.getName() + " " + tokeniser_PR.getClass().getCanonicalName());

		// gate.creole.splitter.SentenceSplitter
		sentSplit_PR = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "Expertise Mining Sentence Splitter");
		controller.add(sentSplit_PR);
		logger.log(Level.INFO, "added " + sentSplit_PR.getName() + " " + sentSplit_PR.getClass().getCanonicalName());

		// gate.creole.POSTagger
		en_POSTagger_PR = (ProcessingResource) Factory.createResource("gate.creole.POSTagger", Factory.newFeatureMap(),
				Factory.newFeatureMap(), "Expertise Mining English POS Tagger");
		controller.add(en_POSTagger_PR);
		logger.log(Level.INFO, "added " + en_POSTagger_PR.getName() + " "
				+ en_POSTagger_PR.getClass().getCanonicalName());

		// gate.creole.morph.Morph
		en_Morphology_PR = (ProcessingResource) Factory.createResource("gate.creole.morph.Morph",
				Factory.newFeatureMap(), Factory.newFeatureMap(), "English Morphology");
		en_Morphology_PR.setParameterValue("rootFeatureName", "root");
		controller.add(en_Morphology_PR);
		logger.log(Level.INFO, "added " + en_Morphology_PR.getName() + " "
				+ en_Morphology_PR.getClass().getCanonicalName());

		// gate.creole.gazetteer.DefaultGazetteer &
		// gate.creole.gazetteer.FlexibleGazetteer
		gazetteer_PR = (ProcessingResource) Factory.createResource(
				"gate.creole.gazetteer.DefaultGazetteer", Factory.newFeatureMap(), Factory.newFeatureMap(),
				"Expertise Mining Gazetteer POS Tagger");
        controller.add(gazetteer_PR);

		// gate.creole.Transducer
		transducer_PR = (ProcessingResource) Factory.createResource("gate.creole.Transducer", Factory.newFeatureMap(),
				Factory.newFeatureMap(), "Expertise Mining Transducer");
		controller.add(transducer_PR);
		logger.log(Level.INFO, "added " + transducer_PR.getName() + " " + transducer_PR.getClass().getCanonicalName());

		// ie.deri.unlp.gate.javaservices.TopicCollector
		topicCollector_PR = (ProcessingResource) Factory.createResource(
				"ie.deri.unlp.gate.topiccollector.TopicCollector", Factory.newFeatureMap(), Factory.newFeatureMap(),
				"Expertise Mining Topic Collector");
		controller.add(topicCollector_PR);
		logger.log(Level.INFO, "added " + topicCollector_PR.getName() + " "
				+ topicCollector_PR.getClass().getCanonicalName());

		@SuppressWarnings("unchecked")
		List<ProcessingResource> prL = new ArrayList<ProcessingResource>(controller.getPRs());
		for (int idx = 0; idx < prL.size(); idx++) {
			logger.log(Level.INFO, "INDEX: " + idx + " -- " + prL.get(idx).getName());
		}
		prL = null;

	}

	private void initializeCorpus() throws ResourceInstantiationException {
		this.corpus = Factory.newCorpus("TopicExtractionCorpus");
		logger.log(Level.INFO, "setting corpus..");
		controller.setCorpus(corpus);
	}

	public ConditionalSerialAnalyserController getController() {
		return this.controller;
	}

	public Corpus getCorpus() {
		return this.corpus;
	}

}
