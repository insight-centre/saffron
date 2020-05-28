package org.insightcentre.nlp.saffron.benchmarks;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;

/**
 * Benchmark system for comparing two extracted taxonomies
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TaxonomyExtractionBenchmark {

    private static int matches(Taxonomy taxo, Set<StringPair> gold) {
        int m = 0;
        for(Taxonomy child : taxo.children) {
            if(gold.contains(new StringPair(taxo.root.toLowerCase(), child.root.toLowerCase()))) {
                m++;
            }
            m += matches(child, gold);
        }
        return m;
    }

    private static class Stats {
        public int size;
        public int maxDepth;
        public int childSq;
    }
    
    private static double size(Taxonomy taxo) {
        int m = 1;
        for(Taxonomy child : taxo.children) {
            m += size(child);
        }
        return m;
    }
    
    private static Stats stats(Taxonomy taxo) {
        Stats s = new Stats();
        s.size = 1;
        s.maxDepth = 1;
        s.childSq = taxo.children.size() * taxo.children.size();
        for(Taxonomy child : taxo.children) {
            Stats c = stats(child);
            s.size += c.size;
            s.maxDepth = Math.max(s.maxDepth, c.maxDepth + 1);
            s.childSq += c.childSq;
        }
        return s;
    }
    
    private static class Scores {
        public int matches;
        public double precision;
        public double recall;

        public Scores(int matches, double precision, double recall) {
            this.matches = matches;
            this.precision = precision;
            this.recall = recall;
        }
    }
    
    private static Scores evalTaxo(Taxonomy extracted, Set<StringPair> gold) {
        final int matches = matches(extracted, gold);
        return new Scores(matches,
                (double)matches / size(extracted),
                (double)matches / gold.size());
    }
    
    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("o", "The output taxonomy").withRequiredArg().ofType(File.class);
                    accepts("g", "The gold taxonomy").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            
            final File taxoFile = (File)os.valueOf("o");
            if(taxoFile == null || !taxoFile.exists()) {
                badOptions(p, "Output taxonomy not specified or does not exist");
                return;
            }
            
            final File goldFile = (File)os.valueOf("g");
            if(goldFile == null || !goldFile.exists()) {
                badOptions(p, "Gold taxonomy not specified or does not exist");
                return;
            }
            
            final Taxonomy taxo = mapper.readValue(taxoFile, Taxonomy.class);
            
            final Set<StringPair> gold;
            final Taxonomy goldTaxo;
            if(goldFile.getName().endsWith(".json")) {
                goldTaxo = mapper.readValue(taxoFile, Taxonomy.class);
                gold = linksFromTaxo(goldTaxo);
            } else {
                gold = readTExEval(goldFile);
                goldTaxo = taxoFromLinks(gold);
            }
            
            final Scores s = evalTaxo(taxo, gold);
            final Stats stats = stats(taxo);
            final double modFM = FowlkesMallows.fowlkesMallows(taxo, goldTaxo);
            
            
            System.err.printf("|-----------|--------|\n");
            System.err.printf("| Matches   | % 6d |\n", s.matches);
            System.err.printf("| Predicted | % 6d |\n", stats.size);
            System.err.printf("| Gold      | % 6d |\n", gold.size());
            System.err.printf("| Depth     | % 6d |\n", stats.maxDepth);
            System.err.printf("| Branching | %.4f |\n", Math.sqrt((double)stats.childSq) / stats.size);
            System.err.printf("|-----------|--------|\n");
            System.err.printf("| Precision | %.4f |\n", s.precision);
            System.err.printf("| Recall    | %.4f |\n", s.recall);
            System.err.printf("| F-Measure | %.4f |\n", 
                    s.precision == 0.0 && s.recall == 0.0 ? 0.0 :
                    2.0 * s.recall * s.precision / (s.precision + s.recall));
            System.err.printf("| F&M       | %.4f |\n", modFM);
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }

    }
    
    private static Set<StringPair> linksFromTaxo(Taxonomy taxo) {
        HashSet<StringPair> links = new HashSet<>();
        _linksFromTaxo(taxo, links);
        return links;        
    }
    
    
    private static Taxonomy taxoFromLinks(Set<StringPair> gold) {
        Map<String, Taxonomy> taxos = new HashMap<>();
        Set<String> nonRoots = new HashSet<>();
        for(StringPair sp : gold) {
            final ArrayList<Taxonomy> children;
            if(taxos.containsKey(sp._1)) {
                children = new ArrayList<>(taxos.get(sp._1).children);
            } else {
                children = new ArrayList<>();
            }
            final Taxonomy child;
            if(taxos.containsKey(sp._2)) {
                child = taxos.get(sp._2);
            } else {
                child = new Taxonomy(sp._2, 0, 0, new ArrayList<>(), Status.none);
            }
            children.add(child);
            taxos.put(sp._1, new Taxonomy(sp._1, 0, 0,  children, Status.none));
            nonRoots.add(sp._2);
        }
        Set<String> roots = new HashSet<>(taxos.keySet());
        roots.removeAll(nonRoots);
        if(roots.size() == 1) {
            return taxos.get(roots.iterator().next());
        } else if(roots.size() == 0) {
            throw new RuntimeException("Taxo file contains loops");
        } else {
            final ArrayList<Taxonomy> children = new ArrayList<>();
            for(String root : roots) {
                children.add(taxos.get(root));
            }
            return new Taxonomy("", 0, 0, children, Status.none);
        }
    }

    static Set<StringPair> readTExEval(File goldFile) throws IOException {
        HashSet<StringPair> links = new HashSet<>();
        String line;
        try(BufferedReader reader = new BufferedReader(new FileReader(goldFile))) {
            while((line = reader.readLine()) != null) {
                if(!line.equals("")) {
                    String[] elems = line.split("\t");
                    if(elems.length != 2) {
                        throw new IOException("Bad Line: " + line);
                    }
                    links.add(new StringPair(elems[1].toLowerCase(), elems[0].toLowerCase()));
                }
            }
        }
        return links;
    }

    private static void _linksFromTaxo(Taxonomy taxo, HashSet<StringPair> links) {
        for(Taxonomy child : taxo.children) {
            links.add(new StringPair(taxo.root.toLowerCase(), child.root.toLowerCase()));
            _linksFromTaxo(child, links);
        }
    }
    
    static class StringPair {
        public final String _1, _2;

        public StringPair(String _1, String _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this._1);
            hash = 53 * hash + Objects.hashCode(this._2);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringPair other = (StringPair) obj;
            if (!Objects.equals(this._1, other._1)) {
                return false;
            }
            if (!Objects.equals(this._2, other._2)) {
                return false;
            }
            return true;
        }
        
        
    }
}
