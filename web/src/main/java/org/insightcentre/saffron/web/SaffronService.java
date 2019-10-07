package org.insightcentre.saffron.web;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.VirtualRootTaxonomy;
import org.insightcentre.nlp.saffron.exceptions.InvalidOperationException;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;

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
    				agException = new RuntimeException("Some topics were not updated: " + e.getMessage());
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
			taxonomy.removeDescendent(topic.topicString);
			boolean taxonomyUpdated = dataSource.updateTaxonomy(taxonomyId, taxonomy);
			if(!taxonomyUpdated)
				throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
				//TODO It should revert the topic update in this case
		}	
	}
	
	/**
	 * Update multiple parent-child relationships in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param parentChildStatusMap - the relations to be changed. Tuples of type <Parent,Child,status>
	 */
	public void updateParentRelationshipStatus(String taxonomyId, List<Pair<String,String>> parentChildStatusList) {
		RuntimeException agException = null;
		
		for(Pair tuple: parentChildStatusList) {
			try {
				updateParentRelationshipStatus(taxonomyId, (String) tuple.getLeft(), (String) tuple.getRight());
    		} catch (Exception e) {
    			if (agException == null)
    				agException = new RuntimeException("Some parent-child relations were not updated: " + e.getMessage());
    			agException.addSuppressed(e);
    		}
		}
		
		if (agException != null)
    		throw agException;
	}

	/**
	 * Update the status of a given parent-child relationship in a given taxonomy
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param topicChild - the identifier for the child topic
	 * @param status - the status to be modified
	 */
	public void updateParentRelationshipStatus(String taxonomyId, String topicChild, String status) {
		/*
		 * 1 - If new status = "rejected" then, throw InvalidOperationException (every node must have a parent) 
		 * 2 - Change relation status in the database otherwise
		 * 3 - If status = "accepted" then, change both topic status to "accepted"
		 */
		
		if (taxonomyId == null || taxonomyId == "") {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty or null");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}		
		if (topicChild == null || topicChild.equals("")) {
			InvalidValueException exception = new InvalidValueException("The topicChild cannot be empty or null");
			exception.addParameterValue("topicChild", topicChild);
			throw exception;
		}
		if (status == null) {
			InvalidValueException exception = new InvalidValueException("The status cannot be null");
			exception.addParameterValue("status", status);
			throw exception;
		}
		
		if (status.equals(Status.rejected.toString())) {
			//1 - If new status = "rejected" then, throw InvalidOperationException (every node must have a parent)
			throw new InvalidOperationException("Parent-child relations cannot be rejected. Choose a new parent instead.");
		} else if (!status.equals(Status.accepted.toString()) && !status.equals(Status.none.toString())) {
			InvalidValueException exception = new InvalidValueException("Invalid status value");
			exception.addParameterValue("status", status);
			throw exception; 
		}
		
		// 2 - Change relation status in the database otherwise
		Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
		taxonomy.setParentChildStatus(topicChild,Status.valueOf(status));
		boolean taxonomyUpdated = dataSource.updateTaxonomy(taxonomyId, taxonomy);
		if(!taxonomyUpdated)
			throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
		
		// 3 - If relation status = "accepted" then, change both topic status to "accepted"
		if (status.equals(Status.accepted.toString())) {
			
			boolean topicUpdated = dataSource.updateTopic(taxonomyId, topicChild, Status.accepted.toString());
			if(!topicUpdated)
				throw new RuntimeException("An error has ocurred when updating the status of the child topic in the database.");
			
			String parentString = taxonomy.getParent(topicChild).getRoot();			
			if (!parentString.equals(VirtualRootTaxonomy.VIRTUAL_ROOT)) {
				topicUpdated = dataSource.updateTopic(taxonomyId, parentString, Status.accepted.toString());
				if(!topicUpdated)
					throw new RuntimeException("An error has ocurred when updating the status of the parent topic in the database.");
			}
		}		
	}

	/**
	 * Update the parent of multiple topics in the taxonomy
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param childNewParentList - the topic-newParent pairs to be modified
	 */
	public void updateParent(String taxonomyId, List<Pair<String, String>> childNewParentList) {
		RuntimeException agException = null;
	
		for(Pair<String,String> childNewParent: childNewParentList) {
			try {
				this.updateParent(taxonomyId, (String) childNewParent.getLeft(), (String) childNewParent.getRight());
			} catch (Exception e) {
    			if (agException == null)
    				agException = new RuntimeException("Some change parent relations were not updated: " + e.getMessage());
    			agException.addSuppressed(e);
    		}
		}
		
		if (agException != null)
    		throw agException;
	}
	
	/**
	 * Update the parent of a given topic and mark their relationship as accepted
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param topicChild - the child topic to have the parent changed
	 * @param topicNewParent - the new parent for the child topic
	 */
	public void updateParent(String taxonomyId, String topicChild, String topicNewParent) {
		/*
		 * 1 - Get taxonomy
		 * 2 - If taxonomy exists, ask it to update the parent of the topic.
		 * 3 - Save the modifications performed in the taxonomy
		 * 4 - Change status of child - new parent relation to accepted.
		 */
		
		if (taxonomyId == null || taxonomyId == "") {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty or null");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}		
		if (topicChild == null || topicChild.equals("")) {
			InvalidValueException exception = new InvalidValueException("The topicChild cannot be empty or null");
			exception.addParameterValue("topicChild", topicChild);
			throw exception;
		}
		if (topicNewParent == null || topicNewParent.equals("")) {
			InvalidValueException exception = new InvalidValueException("The topicNewParent cannot be empty or null");
			exception.addParameterValue("topicNewParent", topicNewParent);
			throw exception;
		}
		
		
		Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
		if(taxonomy == null)
			throw new RuntimeException("There is no run with id = '" + taxonomyId + "'.");
		taxonomy.updateParent(topicChild, topicNewParent);
		
		boolean taxonomyUpdated =  dataSource.updateTaxonomy(taxonomyId, taxonomy);
		if(!taxonomyUpdated)
			throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
		
		this.updateParentRelationshipStatus(taxonomyId, topicChild, Status.accepted.toString());
	}
}
