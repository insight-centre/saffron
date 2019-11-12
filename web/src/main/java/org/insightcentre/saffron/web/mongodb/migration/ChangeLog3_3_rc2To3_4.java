package org.insightcentre.saffron.web.mongodb.migration;

import java.io.IOException;

import org.insightcentre.nlp.saffron.data.Term;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * This class includes all database changes required to upgrade
 * from Saffron 3.3-rc2 to 3.4
 * 
 * @author Bianca Pereira
 *
 */
@ChangeLog
public class ChangeLog3_3_rc2To3_4 {

	@ChangeSet(order = "001", id = "migrateTopicCollectionToTerm", author = "bianca_pereira")
    public void migrateTopicCollectionToTerm(DB db) throws JsonParseException, JsonMappingException, IOException {
        final DBCollection topicCollection = db.getCollection("saffron_run_topics");
        final DBCollection termCollection = db.getCollection("saffron_run_terms");
        DBCursor topicDocs = topicCollection.find();
        
        while (topicDocs.hasNext()) {
        	DBObject obj = topicDocs.next();
        	Term term = new ObjectMapper().readValue(JSON.serialize(obj),Term.class);
        	
        	obj.put("term_string",obj.get("topic_string"));
        	obj.removeField("topic_string");
        	
        	obj.put("term",obj.get("topic"));
        	obj.removeField("topic");
        	termCollection.save(obj);
        }
        
        topicCollection.drop();
    }
	
	@ChangeSet(order = "002", id = "migrateTopicsCorrespondenceCollectionToTerm", author = "bianca_pereira")
    public void migrateTopicsCorrespondenceCollectionToTerm(DB db) throws JsonParseException, JsonMappingException, IOException {
        final DBCollection topicCollection = db.getCollection("saffron_run_topics_correspondence");
        final DBCollection termCollection = db.getCollection("saffron_run_terms_correspondence");
        DBCursor topicDocs = topicCollection.find();
        
        while (topicDocs.hasNext()) {
        	DBObject obj = topicDocs.next();
        	Term term = new ObjectMapper().readValue(JSON.serialize(obj),Term.class);
        	
        	obj.put("term_string",obj.get("topic_string"));
        	obj.removeField("topic_string");

        	termCollection.save(obj);
        }
        
        topicCollection.drop();
    }
	
	@ChangeSet(order = "003", id = "migrateTopicsExtractionCollectionToTerm", author = "bianca_pereira")
    public void migrateTopicsExtractionCollectionToTerm(DB db) throws JsonParseException, JsonMappingException, IOException {
        final DBCollection topicCollection = db.getCollection("saffron_run_topics_extraction");
        final DBCollection termCollection = db.getCollection("saffron_run_terms_extraction");
        DBCursor topicDocs = topicCollection.find();
        
        while (topicDocs.hasNext()) {
        	DBObject obj = topicDocs.next();
        	Term term = new ObjectMapper().readValue(JSON.serialize(obj),Term.class);
        	
        	obj.put("term",obj.get("topic"));
        	obj.removeField("topic");

        	termCollection.save(obj);
        }
        topicCollection.drop();
    }
	
	@ChangeSet(order = "004", id = "migrateAuthorTopicsCollectionToTerm", author = "bianca_pereira")
    public void migrateAuthorTopicsCollectionToTerm(DB db) throws JsonParseException, JsonMappingException, IOException {
        final DBCollection topicCollection = db.getCollection("saffron_run_author_topics");
        final DBCollection termCollection = db.getCollection("saffron_run_author_terms");
        DBCursor topicDocs = topicCollection.find();
        
        while (topicDocs.hasNext()) {
        	DBObject obj = topicDocs.next();
        	Term term = new ObjectMapper().readValue(JSON.serialize(obj),Term.class);
        	
        	obj.put("author_term",obj.get("author_topic"));
        	obj.removeField("author_topic");
        	
        	obj.put("term_id",obj.get("topic_id"));
        	obj.removeField("topic_id");

        	termCollection.save(obj);
        }
        topicCollection.drop();
    }
	
	@ChangeSet(order = "005", id = "migrateTopicsSimilarityCollectionToTerm", author = "bianca_pereira")
    public void migrateTopicsSimilarityCollectionToTerm(DB db) throws JsonParseException, JsonMappingException, IOException {
        final DBCollection topicCollection = db.getCollection("saffron_run_topics_similarity");
        final DBCollection termCollection = db.getCollection("saffron_run_terms_similarity");
        DBCursor topicDocs = topicCollection.find();
        
        while (topicDocs.hasNext()) {
        	DBObject obj = topicDocs.next();
        	Term term = new ObjectMapper().readValue(JSON.serialize(obj),Term.class);

        	obj.put("term1_id",obj.get("topic1_id"));
        	obj.removeField("topic1_id");
        	
        	obj.put("term2_id",obj.get("topic2_id"));
        	obj.removeField("topic2_id");

        	termCollection.save(obj);
        }
        topicCollection.drop();
    }
	
}
