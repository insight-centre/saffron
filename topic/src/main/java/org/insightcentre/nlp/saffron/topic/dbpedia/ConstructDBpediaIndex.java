package org.insightcentre.nlp.saffron.topic.dbpedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Builds the DBpedia index from the redirects dump
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class ConstructDBpediaIndex {
   private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: "  + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
  
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {{
                accepts("d", "The Google NGrams dump file").withRequiredArg().ofType(File.class);
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(redirectFile))));
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
            s.execute("CREATE TABLE dbpedia "
                + "(key TEXT NOT NULL,"
                + " variant TEXT NOT NULL,"
                + " escaped TEXT NOT NULL)");
            s.execute("CREATE INDEX idx_escaped ON dbpedia(escaped)");
            c.setAutoCommit(false);
            stat = c.prepareStatement("INSERT INTO dbpedia VALUES (?,?,?)");
            while((line = reader.readLine()) != null) {
                if(line.startsWith("#"))
                    continue;
                
                String[] elems = line.split("\\s+");
                if(elems.length != 4) {
                    throw new IllegalArgumentException("Bad line: " + line);
                }
                
                String variant = dbpediaName(elems[0]);
                String key = dbpediaName(elems[2]);
                String cleanName = clean(variant);

                stat.setString(1, key);
                stat.setString(2, variant);
                stat.setString(3, cleanName);

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

    private static String dbpediaName(String elem) {
        if(!elem.startsWith("<http://dbpedia.org/resource/"))
            throw new IllegalArgumentException(elem);
        return elem.substring(29, elem.length()-1);
    }

    private static String clean(String variant) {
       try {
           return URLDecoder.decode(variant, "UTF-8").replaceAll("_", " ").replaceAll(" \\(.*\\)$", "").toLowerCase();
       } catch (UnsupportedEncodingException ex) {
           throw new RuntimeException(ex); // Will never happen
       }
    }
}
