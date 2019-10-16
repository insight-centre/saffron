package org.insightcentre.nlp.saffron.topic.dbpedia;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Term;

/**
 * Link topics to DBpedia (by name only more or less)
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class LinkToDBpedia implements Closeable {

    private final Connection connection;
    private final PreparedStatement statement;

    public LinkToDBpedia(File dbpediaFile) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException x) {
            throw new RuntimeException("SQLite not on classpath");
        }
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbpediaFile.getAbsolutePath());
        this.statement = connection.prepareStatement("SELECT key, variant FROM dbpedia WHERE escaped=?");
    }

    LinkToDBpedia(Connection connection) throws SQLException {
        this.connection = connection;
        this.statement = connection.prepareStatement("SELECT key, variant FROM dbpedia WHERE escaped=?");
    }

    @Override
    public void close() throws IOException {
        try {
            statement.close();
            connection.close();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Checks if a topic and DBPedia title have the same case, excluding the
     * first letter of each word. e.g. 'bliss'=='Bliss' but 'bliss' != 'BLISS'
     */
    static boolean has_same_case(String topic, String title) {
        String[] topic_words = topic.split(" ");
        String[] title_words = title.split("_");
        for (int i = 0; i < topic_words.length && i < title_words.length; i++) {
            String topic_word = topic_words[i];
            String title_word = title_words[i];
            for (int j = 0; j < topic_word.length() && j < title_word.length(); j++) {
                char c1 = topic_word.charAt(j);
                char c2 = title_word.charAt(j);
                if (Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                    break;
                }
                if (c1 != c2) {
                    return false;
                }
            }
        }
        return true;
    }

    void remove_different_case(List<Candidate> topic2titles, Term topic) {
        Iterator<Candidate> i = topic2titles.iterator();
        while (i.hasNext()) {
            if (!has_same_case(topic.getString(), i.next().variant)) {
                i.remove();
            }
        }
    }

    void remove_listof_pages(List<Candidate> topic2titles) {
        Iterator<Candidate> i = topic2titles.iterator();
        while (i.hasNext()) {
            Candidate c = i.next();
            if (c.dbpediaKey.startsWith("List_of") || c.dbpediaKey.startsWith("List_of")) {
                i.remove();
            }
        }
    }

    static boolean is_titled(String s, String delim) {
        if (delim == null) {
            delim = " ";
        }
        for (String ss : s.split(delim)) {
            if (!Character.isUpperCase(ss.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    void remove_titled(List<Candidate> topic2titles) {
        Iterator<Candidate> i = topic2titles.iterator();
        while (i.hasNext()) {
            Candidate c = i.next();
            if (c.dbpediaKey.contains("_") && is_titled(c.dbpediaKey, "_")) {
                i.remove();
            }
        }
    }

    Term pick_best_matches(List<Candidate> topic2titles, Term original) {
        // TODO: Do something better here
        if (topic2titles.size() == 1) {
            try {
                original.setDbpediaUrl(new URL("http://dbpedia.org/resource/" + topic2titles.get(0).dbpediaKey));
            } catch (MalformedURLException ex) {
                System.err.println("Bad URL " + topic2titles.get(0).dbpediaKey);
            }
        } else if (topic2titles.size() > 1) {
            System.err.println("Multiple candidates for " + original.getString());
        }
        return original;
    }

    static class Candidate {

        String dbpediaKey;
        String variant;

        public Candidate(String dbpediaKey, String variant) {
            this.dbpediaKey = dbpediaKey;
            this.variant = variant;
        }
    }

    List<Candidate> candidates(Term topic) {
        try {
            statement.setString(1, topic.getString());
            ResultSet results = statement.executeQuery();
            List<Candidate> candidates = new ArrayList<>();
            while (results.next()) {
                candidates.add(new Candidate(results.getString(1), results.getString(2)));
            }
            return candidates;
        } catch (SQLException x) {
            throw new RuntimeException(x);
        }
    }

    public Term matchDBpediaArticle(Term topic) {
        List<Candidate> topic2titles = candidates(topic);
        remove_different_case(topic2titles, topic);
        //replace_redirects(topic2titles);
        remove_listof_pages(topic2titles);
        remove_titled(topic2titles);

        return pick_best_matches(topic2titles, topic);
    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("t", "The list of topics to read").withRequiredArg().ofType(File.class);
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("o", "The list of topics to write to").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            final File input = (File) os.valueOf("t");
            if (input == null || !input.exists()) {
                badOptions(p, "Input does not exist");
            }
            final File configFile = (File) os.valueOf("c");
            if (configFile == null || !configFile.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File output = (File) os.valueOf("o");
            if (output == null) {
                badOptions(p, "Output not specified");
            }

            ObjectMapper mapper = new ObjectMapper();
            Configuration config = mapper.readValue(configFile, Configuration.class);

            List<Term> outTopics = mapper.readValue(input, mapper.getTypeFactory().constructCollectionType(List.class, Term.class));

            mapper.writerWithDefaultPrettyPrinter().writeValue(output, outTopics);

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static class Configuration {

        File database = new File("models/dbpedia.db");

        public File getDatabase() {
            return database;
        }

        public void setDatabase(File database) {
            this.database = database;
        }

    }

}
