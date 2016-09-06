package org.insightcentre.nlp.saffron.taxonomy.db;

import org.insightcentre.nlp.saffron.taxonomy.db.saffron2.Saffron2Paper;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface DAO {

	public abstract Integer numDocuments() throws SQLException;

	public abstract List<String> topRankingTopicStrings(int num) throws SQLException;

	public abstract Integer calculateTotalTokensNo();

	public abstract Integer selectCountJointTopics(String rootSequence, String rootSequence2)
			throws SQLException;

	public abstract Map<String, Double> topScoringTopicsForResearcher(String researcherId, int limit)
			throws SQLException;

	public abstract TopicSimilarity getTopicSimilarity(String topicString, String topicString2)
			throws SQLException;

	public abstract Topic getTopic(String preferredString) throws SQLException;
	
	public String getTopicStringFromRootSequence(String rootSequence) throws SQLException;

	public abstract Iterator<Saffron2Paper> getPapers() throws SQLException;
}