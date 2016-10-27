package org.insightcentre.nlp.saffron.taxonomy.db.saffron2;

import org.insightcentre.nlp.saffron.taxonomy.db.Saffron2Paper;
import org.insightcentre.nlp.saffron.taxonomy.db.DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.MorphologicalVariation;
import org.insightcentre.nlp.saffron.taxonomy.db.Topic;
import org.insightcentre.nlp.saffron.taxonomy.db.TopicSimilarity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Saffron2DAO implements DAO {
	private Connection conn;

	public Saffron2DAO(String jdbcUrl, String user, String password) throws SQLException {
		if ("None".equals(password)) {
			password = null;
		}
		conn = DriverManager.getConnection(jdbcUrl, user, password);
	}

	protected List<String> getColumn(String query) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(query);
		ResultSet rs = ps.executeQuery();

		try {
			List<String> col = new ArrayList<String>();
			while (rs.next()) {
				col.add(rs.getString(1));
			}
			return col;
		} finally {
			ps.close();
			rs.close();
		}
	}

	protected String getString(String query) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(query);
		ResultSet rs = ps.executeQuery();
		try {
			if (!rs.next())
				return null;
			return rs.getString(1);
		} finally {
			ps.close();
			rs.close();
		}
	}
	
	protected Double getDouble(String query) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(query);
		ResultSet rs = ps.executeQuery();
		try {
			if (!rs.next())
				return null;
			return rs.getDouble(1);
		} finally {
			ps.close();
			rs.close();
		}
	}

	protected Integer getInteger(String query) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(query);
		ResultSet rs = ps.executeQuery();
		try {
			if (!rs.next())
				return null;
			return rs.getInt(1);
		} finally {
			ps.close();
			rs.close();
		}
	}

	@Override
	public Integer numDocuments() throws SQLException {
		String q = "SELECT COUNT(id) FROM Paper;";
		return getInteger(q);
	}

	@Override
	public List<String> topRankingTopicStrings(int num) throws SQLException {
		String q = "SELECT topic_string FROM Topic ORDER BY score DESC LIMIT " + num;
		return getColumn(q);
	}

	@Override
	public Integer calculateTotalTokensNo() {
		// TODO: Since it's just a normalising thing, this should not affect the
		// results.
		return Integer.MAX_VALUE;
	}

	@Override
	public Integer selectCountJointTopics(String rootSequence, String rootSequence2)
			throws SQLException {
		int topicId1 = getInteger("SELECT id FROM Topic WHERE slug=\"" + rootSequence + "\"");
		int topicId2 = getInteger("SELECT id FROM Topic WHERE slug=\"" + rootSequence2 + "\"");

		String q = "SELECT COUNT(*) FROM PaperTopic pt1, PaperTopic pt2 WHERE pt1.paper_id=pt2.paper_id "
				+ "AND pt1.topic_id=" + topicId1 + " AND pt2.topic_id=" + topicId2;
		return getInteger(q);
	}

//	@Override
//	public Map<String, Double> topScoringTopicsForResearcher(String researcherId, int limit)
//			throws SQLException {
//		String q = "SELECT Topic.topic_string, ResearcherTopic.score FROM ResearcherTopic "
//				+ " JOIN Topic ON topic_id=Topic.id " + "WHERE researcher_id=" + researcherId
//				+ " ORDER BY score DESC LIMIT " + limit;
//		PreparedStatement ps = conn.prepareStatement(q);
//		ResultSet rs = ps.executeQuery();
//
//		try {
//			Map<String, Double> topicString2Score = new HashMap<String, Double>();
//			while (rs.next()) {
//				topicString2Score.put(rs.getString(1), rs.getDouble(2));
//			}
//			return topicString2Score;
//		} finally {
//			ps.close();
//			rs.close();
//		}
//	}
//
//	@Override
//	public TopicSimilarity getTopicSimilarity(String topicString, String topicString2)
//			throws SQLException {
//		int topicId1 = getInteger("SELECT id FROM Topic WHERE topic_string=\"" + topicString + "\"");
//		int topicId2 = getInteger("SELECT id FROM Topic WHERE topic_string=\"" + topicString2
//				+ "\"");
//		Double sim = getDouble("SELECT similarity FROM TopicSimilarity WHERE topic1_id=" + topicId1
//				+ " AND topic2_id=" + topicId2);
//		return sim == null ? null : new TopicSimilarity(sim);
//	}

	@Override
	public Topic getTopic(String preferredString) throws SQLException {
		String q = "SELECT slug, occurrences, matches, score FROM Topic " + "WHERE topic_string=\""
				+ preferredString + "\"";
		PreparedStatement ps = conn.prepareStatement(q);
		ResultSet rs = ps.executeQuery();

		try {
			if (!rs.next())
				return null;

			String topic_slug = rs.getString("slug");
			List<MorphologicalVariation> mvs = new ArrayList<>();
			for (String mv : getColumn("SELECT DISTINCT(term_string) FROM PaperTerm "
					+ "WHERE topic_slug=\"" + topic_slug + "\"")) {
				mvs.add(new MorphologicalVariation(mv));
			}

			return new Topic(preferredString, topic_slug, rs.getInt("occurrences"),
					rs.getInt("matches"), rs.getDouble("score"), mvs);
		} finally {
			ps.close();
			rs.close();
		}
	}

	@Override
	public Iterator<Saffron2Paper> getPapers() throws SQLException {
		final PreparedStatement ps = conn.prepareStatement("SELECT id, raw_text FROM Paper");
		final ResultSet rs = ps.executeQuery();

		return new Iterator<Saffron2Paper>() {
			@Override
			public boolean hasNext() {
				try {
					boolean has = rs.next();
					if (!has) {
						ps.close();
						rs.close();					
					}
					return has;
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public Saffron2Paper next() {
				try {
					return new Saffron2Paper(rs.getString(1), rs.getString(2));
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
//
//	@Override
//	public String getTopicStringFromRootSequence(String rootSequence) throws SQLException {
//		String q = "SELECT topic_string FROM Topic " + "WHERE slug=\""
//				+ rootSequence+ "\"";
//		return getString(q);
//	}
}
