package org.insightcentre.saffron.web;

import java.util.List;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;

public class SaffronService {
	
	private static String mongoUrl = System.getenv("MONGO_URL");
    private static String mongoPort = System.getenv("MONGO_PORT");
    private static String mongoDbName = System.getenv("MONGO_DB_NAME");
    
    //FIXME This should not be hardcoded like that. The user should be able to choose which SaffronDataSource they wish
  	private static SaffronDataSource dataSource = new MongoDBHandler(
              mongoUrl, new Integer(mongoPort), mongoDbName, "saffron_runs");

    
	/**
	 * Update the revision status of multiple topics in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param topics - the topics to be modified
	 */
	public static void updateTopicStatus(String taxonomyId, List<Topic> topics) {
    	for(Topic topic: topics){
			updateTopicStatus(taxonomyId,topic);
		}		
	}

	/**
	 * Update the revision status of a single topic in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param topic - the topic to be modified
	 */
	public static void updateTopicStatus(String taxonomyId, Topic topic) {
		/*
		 * 1 - Change topic status in the database.
		 * 2 - If new status = "rejected" then
		 * 3 - remove the topic from the taxonomy and update the taxonomy in the database
		 */
		
		// 1 - Change topic status in the database.
		dataSource.updateTopic(taxonomyId, topic.topicString, topic.status.toString());
		
		//2 - If new status = "rejected" then
		if (topic.status.equals(Status.rejected)) {
			Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
			taxonomy = taxonomy.removeChild(topic.topicString, taxonomy);
			dataSource.updateTaxonomy(taxonomyId, taxonomy);
		}	
	}
}
