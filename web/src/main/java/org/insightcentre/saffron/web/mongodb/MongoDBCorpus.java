package org.insightcentre.saffron.web.mongodb;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Iterator;

import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Document.Loader;

import com.mongodb.client.FindIterable;

/**
 * Represents a corpus stored within MongoDB
 * 
 * @author Bianca Pereira
 *
 */
public class MongoDBCorpus implements Corpus{

	private MongoDBHandler dbHandler;
	private String runId;

    public MongoDBCorpus(MongoDBHandler dbHandler, String runId) {
    	this.dbHandler = dbHandler;
    	this.runId = runId;
    }

    @Override
    public Iterable<Document> getDocuments() {
        return new DocList();
    }

    @Override
    public int size() {
        return (int) dbHandler.corpusCollection.count(and(eq(dbHandler.RUN_IDENTIFIER, runId)));
    }

    
    private class DocList implements Iterable<Document> {

    	private FindIterable<org.bson.Document> dbDocs;
    	private Loader contentLoader;
    	
    	public DocList() {
    		this.dbDocs = dbHandler.corpusCollection.find(and(eq(dbHandler.RUN_IDENTIFIER, runId)));
    		this.contentLoader = new MongoDBCorpusDocContentLoader(dbHandler,runId);
    	}
    	
		@Override
		public Iterator<Document> iterator() {
			
			Iterator<org.bson.Document> dbIterator = dbDocs.iterator();
			
			Iterator<Document> it = new Iterator<Document>() {

				@Override
				public boolean hasNext() {
					return dbIterator.hasNext();
				}

				@Override
				public Document next() {
					return dbHandler.buildCorpusDoc(runId, dbIterator.next(), contentLoader);
				}			
			};
			
			return it;
		}
    }
    
}
