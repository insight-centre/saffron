package org.insightcentre.saffron.web.mongodb;

import java.io.IOException;

import org.insightcentre.nlp.saffron.data.Document;

/**
 * Used to load document content on demand
 * 
 * @author Bianca Pereira
 *
 */
public class MongoDBCorpusDocContentLoader implements Document.Loader {
	
	private MongoDBHandler dbConnector = null;
	private String runId;
	
	public MongoDBCorpusDocContentLoader(MongoDBHandler dbConnector, String runId) {
		this.dbConnector = dbConnector;
		this.runId = runId;
	}

	@Override
	public String getContents(Document d) {
		try {
			return dbConnector.getDocumentContent(runId, d.getId());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getContentsSerializable(Document d) {
		return null;
	}

}
