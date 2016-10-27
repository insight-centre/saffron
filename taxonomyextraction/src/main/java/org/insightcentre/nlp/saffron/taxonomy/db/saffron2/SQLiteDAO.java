package org.insightcentre.nlp.saffron.taxonomy.db.saffron2;

import org.insightcentre.nlp.saffron.taxonomy.db.Saffron2Paper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.insightcentre.nlp.saffron.taxonomy.db.DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.Topic;
import org.insightcentre.nlp.saffron.taxonomy.db.TopicSimilarity;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class SQLiteDAO implements DAO {
    private final String jdbcUrl;

    public SQLiteDAO(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch(ClassNotFoundException x) {
            throw new RuntimeException("SQLite not in JDBC path");
        }
    }

    private static interface Action<A> {
        public A act(Statement c) throws SQLException;
    }
    
    private <A> A withConnection(Action<A> action) throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl);
        Statement s = c.createStatement();
        try {
            return action.act(s);
        } finally {
            s.close();
            c.close();
        }
    }
    
    @Override
    public Integer numDocuments() throws SQLException {
        return withConnection(new Action<Integer>() {

            @Override
            public Integer act(Statement c) throws SQLException {
                final ResultSet rs = c.executeQuery("SELECT COUNT(*) FROM Papers;");
                return rs.getInt(1);
            }
        });
    }

    @Override
    public List<String> topRankingTopicStrings(final int num) throws SQLException {
        return withConnection(new Action<List<String>>() {

            @Override
            public List<String> act(Statement c) throws SQLException {
                final ResultSet rs = c.executeQuery("SELECT topic_string FROM Topic ORDER BY score DESC LIMIT " + num);
                List<String> results = new ArrayList<>();
                while(rs.next()) {
                    results.add(rs.getString(1));
                }
                return results;
            }

        });
    }

    @Override
    public Integer calculateTotalTokensNo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer selectCountJointTopics(String rootSequence, String rootSequence2) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Topic getTopic(String preferredString) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<Saffron2Paper> getPapers() throws SQLException {
        return withConnection(new Action<Iterator<Saffron2Paper>>() {

            @Override
            public Iterator<Saffron2Paper> act(Statement c) throws SQLException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
    }

}
