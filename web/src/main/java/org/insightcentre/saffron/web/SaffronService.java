package org.insightcentre.saffron.web;

import java.util.List;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.saffron.web.exceptions.InvalidOperationException;
import org.insightcentre.saffron.web.exceptions.InvalidValueException;

/**
 * Service to connect with other Saffron components and
 * perform overall operations
 * 
 * @author Bianca Pereira
 *
 */
public class SaffronService {
	
	private SaffronDataSource dataSource;

    public SaffronService(SaffronDataSource dataSource) {
    	this.dataSource = dataSource;
    }
    
	/**
	 * Update the revision status of multiple topics in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param topics - the topics to be modified
	 */
	public void updateTopicStatus(String taxonomyId, List<Topic> topics) {
		RuntimeException agException = null;
		
    	for(Topic topic: topics){
    		try {
    			updateTopicStatus(taxonomyId,topic);
    		} catch (Exception e) {
    			if (agException == null)
    				agException = new RuntimeException("some topics were not updated");
    			agException.addSuppressed(e);
    		}
		}
    	
    	if (agException != null)
    		throw agException;
	}

	/**
	 * Update the revision status of a single topic in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param topic - the topic to be modified
	 */
	public void updateTopicStatus(String taxonomyId, Topic topic) {
		/*
		 * 1 - Change topic status in the database.
		 * 2 - If new status = "rejected" then
		 * 3 - remove the topic from the taxonomy and update the taxonomy in the database
		 */
		if (taxonomyId == "") {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}		
		if (topic.topicString == null || topic.topicString.equals("")) {
			InvalidValueException exception = new InvalidValueException("The topic string cannot be empty or null");
			exception.addParameterValue("topic.string", topic.topicString);
			throw exception;
		}
		if (topic.status == null) {
			InvalidValueException exception = new InvalidValueException("The topic status cannot be null");
			exception.addParameterValue("topic.status", topic.status);
			throw exception;
		}
		
		Topic dbTopic = dataSource.getTopic(taxonomyId, topic.topicString);
		if (dbTopic.status.equals(Status.rejected))
			throw new InvalidOperationException("The status of a 'rejected' topic cannot be modified.");
		
		// 1 - Change topic status in the database.
		boolean topicUpdated = dataSource.updateTopic(taxonomyId, topic.topicString, topic.status.toString());
		if(!topicUpdated)
			throw new RuntimeException("An error has ocurred when updating the topic in the database.");
		
		//2 - If new status = "rejected" then
		if (topic.status.equals(Status.rejected)) {
			Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
			taxonomy.removeChild(topic.topicString);
			boolean taxonomyUpdated = dataSource.updateTaxonomy(taxonomyId, taxonomy);
			if(!taxonomyUpdated)
				throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
				//TODO It should revert the topic update in this case
		}	
	}
}
