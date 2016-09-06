/*
 *
 * GateProcessorConsumer.java, provides topic extraction as a Java program/API
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

import gate.Factory;
import gate.util.GateException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;
import org.insightcentre.nlp.saffron.topicextraction.data.DomainModel;
import org.insightcentre.nlp.saffron.topicextraction.data.DomainModel.TemporaryGazetteerDirectory;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.TopicBearer;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.TopicExtractor;

/**
 * Handles the consumption (and returning) of a GateProcessor from the
 * ProcessorPool
 * <p/>
 * If the retrieval of a GateProcessor from the Processor Pool is successful
 * before a timeout occurs, it is used for processing a gate.Document, which is
 * added to the corpus over which the Processing Pipeline of the GateProcessor
 * is being executed.
 * <p/>
 * After executing the Processing Pipeline, the relevant annotations and
 * amendments to the gate.Document are being post-processed, and transformed
 * into an acceptable output structure.
 * <p/>
 * Finally, the GATE-internal structures are being cleaned up and GateProcessor
 * is being returned back to the Processor Pool so that other calls can be made
 *
 * @author alesch
 */
class GateProcessorConsumer {

    private static Logger logger =
            Logger.getLogger(GateProcessorConsumer.class.getName());

    private GateProcessorPool pool;

    // the timeout in milliseconds
    private static final int TIMEOUT = 5000;

    public GateProcessorConsumer(GateProcessorPool p) {
        this.pool = p;
    }

    @SuppressWarnings("unchecked")
	public TopicBearer process(gate.Document document, DomainModel domainModel)
            throws GateException, GateProcessorNotAvailableException, InterruptedException {

        /**
         * we accumulate the topics here
         */
        TopicBearer topicbearer = new TopicBearer();

        // retrieve a GateProcessor from the Processor Pool, fail after a
        // timeout
        GateProcessor gateProc = pool.poll(TIMEOUT, TimeUnit.MILLISECONDS);

        if (gateProc != null) {

        	TemporaryGazetteerDirectory tempGazetteer;
        	
        	try {
        		tempGazetteer = domainModel.asGateGazetteer();
				gateProc.setGazetteerListsURL(tempGazetteer.getListsURL());
			} catch (IOException e) {
				throw new GateException("Unable to get GATE gazetteer", e);
			}
        	
            // do processing here
            try {
                logger.log(Level.TRACE, "Adding document to corpus");
                gateProc.getCorpus().add(document);
                logger.log(Level.TRACE, "Executing pipeline over corpus ...");
                gateProc.getController().execute();

                // do stuff with annotated document, actually only return
                // topic bearer as container holding the frequency maps
                // stored in the GATE

                topicbearer.setExtractedTopics((List<ExtractedTopic>) document
                        .getFeatures().get(TopicExtractor.TOPICS_LIST_FEATURE_NAME));

                topicbearer.setTokensNumber((Integer) document.getFeatures().get(
                        TopicExtractor.TOKENS_NUMBER_FEATURE_NAME));
            } finally {

                // clean up
                cleanUpGate(document, gateProc);

                try {
					tempGazetteer.close();
				} catch (IOException e) {
					logger.error("Unable to delete temporary gazetteer directory", e);
				}
                
                document = null;
                // and nicely return the GateProcessor back to the pool in case it
                // is not null
                if (gateProc != null && pool.offer(gateProc)) {
                    logger.log(Level.INFO,
                            "successfully returned GateProcessor back to the pool");
                }
            }

        } else {
        	String msg = "No GateProcessor available at the moment, please try again later.";
            throw new GateProcessorNotAvailableException(msg);
        }


        return topicbearer;
    }

    /**
     * @param document
     * @param gateProc
     */
    private void cleanUpGate(gate.Document document, GateProcessor gateProc) {
        gateProc.getCorpus().unloadDocument(document);
        gateProc.getCorpus().cleanup();
        gateProc.getCorpus().clear();
        document.cleanup();
        Factory.deleteResource((gate.Document) document);
    }

}
