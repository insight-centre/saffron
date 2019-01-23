package org.insightcentre.nlp.saffron.topic.ngrams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.zip.GZIPInputStream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class ConstructNGramIndex {
   private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: "  + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
  
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {{
                accepts("d", "The DBpedia dump file (redirects_en.ttl.bz2)").withRequiredArg().ofType(File.class);
                accepts("o", "The output model database file").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            final File redirectFile  = (File)os.valueOf("d");
            if(redirectFile == null || !redirectFile.exists()) {
                badOptions(p, "DBpedia redirect file not specified");
            }
            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }

            createDatabase(redirectFile, output); 
            
  
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }

    private static void createDatabase(File redirectFile, File output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(redirectFile))));
        String line;
        int i = 0;
        final PreparedStatement stat;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch(ClassNotFoundException x) {
            throw new RuntimeException("SQLite not on classpath");
        }
        try(Connection c = DriverManager.getConnection("jdbc:sqlite:" + output.getAbsolutePath())) {
            java.sql.Statement s = c.createStatement();
            s.execute("CREATE TABLE IF NOT EXISTS ngrams "
                + "(ngram TEXT NOT NULL,"
                + " year INT NOT NULL,"
                + " match_count INT NOT NULL,"
                + " volume_count TEXT NOT NULL)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_ngram ON ngrams(ngram)");
            c.setAutoCommit(false);
            stat = c.prepareStatement("INSERT INTO ngrams VALUES (?,?,?,?)");
            while((line = reader.readLine()) != null) {
                if(line.startsWith("#"))
                    continue;
                
                String[] elems = line.split("\\t");
                if(elems.length != 4) {
                    throw new IllegalArgumentException("Bad line: " + line);
                }
                
                stat.setString(1, elems[0]);
                stat.setInt(2, Integer.parseInt(elems[1]));
                stat.setInt(3, Integer.parseInt(elems[2]));
                stat.setInt(4, Integer.parseInt(elems[3]));

                stat.addBatch();

                if(++i % 100000 == 0) {
                    System.err.print(".");
                    stat.executeBatch();
                }
            }
            stat.executeBatch();
            stat.close();
            c.commit();
        } catch(SQLException x) {
            throw new RuntimeException(x);
        }
    }
}
