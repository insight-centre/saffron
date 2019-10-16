package org.insightcentre.nlp.saffron.taxonomy.supervised;

import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration;
import Jama.Matrix;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;
import org.insightcentre.nlp.saffron.taxonomy.wordnet.Hypernym;

/**
 * Train the supervised model based on some existing settings and create a
 * model.
 *
 * Typically train command
 * 
 *     SAFFRON_HOME=.. ./train -c ../models/config.json \
 *        -d ../benchmarks/data/texeval/food_en-doc-topics.json \
 *        -p ../benchmarks/data/texeval/food_en-topics.json \
 *        -t ../benchmarks/data/texeval/food_en.taxo
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Train {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        //args = "-c ../models/config.json -d ../benchmarks/data/texeval/food_en-doc-topics.json -p ../benchmarks/data/texeval/food_en-topics.json -t ../benchmarks/data/texeval/food_en.taxo".split(" ");
        //System.setProperty("saffron.home", "..");
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("d", "The doc-topics connection").withRequiredArg().ofType(File.class);
                    accepts("t", "The taxonomies to train on").withRequiredArg().withValuesSeparatedBy(',').ofType(File.class);
                    accepts("p", "The topics to train on").withRequiredArg().ofType(File.class);
                    accepts("c", "The configuration").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            final ObjectMapper mapper = new ObjectMapper();

            final File topicsFile = (File) os.valueOf("p");

            final File docTopicsFile = (File) os.valueOf("d");
            final List<File> taxoFiles = (List<File>) os.valuesOf("t");
            if (taxoFiles == null) {
                badOptions(p, "No taxonomy files provided");
                return;
            }

            final TaxonomyExtractionConfiguration config;
            final File configFile = (File) os.valueOf("c");
            if (configFile == null) {
                config = new TaxonomyExtractionConfiguration();
            } else {
                config = mapper.readValue(configFile, Configuration.class).taxonomy;
            }
            
            if (config.verify() != null) {
                badOptions(p, "Config invalid: " + config.verify());
            }

            final List<DocumentTopic> docTopics;
            if (docTopicsFile == null) {
                docTopics = null;
            } else {
                docTopics = mapper.readValue(docTopicsFile, mapper.getTypeFactory().constructCollectionType(List.class, DocumentTopic.class));
            }
            final List<List<StringPair>> taxos = new ArrayList<>();
            for (File taxoFile : taxoFiles) {
                taxos.add(loadTaxoFile(taxoFile, mapper));
            }

            final List<Term> topics = topicsFile == null ? null
                    : (List<Term>) mapper.readValue(topicsFile, mapper.getTypeFactory().constructCollectionType(List.class, Term.class));

            Map<String, Term> topicMap = topics == null ? null : loadMap(topics, mapper, new DefaultSaffronListener());

            train(docTopics, topicMap, taxos, config);

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    public static Map<String, double[]> loadGLoVE(File gloveFile) throws IOException {
        if (!gloveFile.exists()) {
            System.err.println("GloVe file does not exist. Not using GloVe");
            return null;
        }
        final Map<String, double[]> data = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new FileReader(gloveFile))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() > 0) {
                    String[] elems = line.split(" ");
                    double[] v = new double[elems.length - 1];
                    for (int i = 1; i < elems.length; i++) {
                        v[i - 1] = Double.parseDouble(elems[i]);
                    }
                    data.put(elems[0], v);
                }
            }
        }
        return data;
    }

    private static List<StringPair> loadTaxoFile(File taxoFile, ObjectMapper mapper) throws IOException {
        if (taxoFile.getName().endsWith(".taxo")) {
            final List<StringPair> pairs = new ArrayList<>();
            try (BufferedReader in = new BufferedReader(new FileReader(taxoFile))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.length() > 0) {
                        String[] elems = line.split("\t");
                        if (elems.length != 2) {
                            throw new RuntimeException("Bad line: " + line);
                        }
                        pairs.add(new StringPair(elems[1], elems[0]));
                    }
                }
            }
            return pairs;
        } else {
            throw new UnsupportedOperationException("Unsupported file type: " + taxoFile.getName());
        }
    }

    private static void train(List<DocumentTopic> docTopics, Map<String, Term> topicMap,
            List<List<StringPair>> taxos, TaxonomyExtractionConfiguration config) throws IOException {
        final Model model = new Model();
        model.features = config.features;
        final Map<String, double[]> glove = config.features == null || config.features.gloveFile == null ? null : loadGLoVE(config.features.gloveFile.toFile());
        final Set<Hypernym> hypernyms = config.features == null || config.features.hypernyms == null ? null : loadHypernyms(config.features.hypernyms.toFile());
        
        Features features = new Features(null, null, indexDocTopics(docTopics), 
                glove, topicMap, hypernyms, config.features == null ? null : config.features.featureSelection);
        
        features = glove == null ? features : learnSVD(taxos, features, config, model);

        svm_problem prob = loadInstances(taxos, features, config.negSampling);
        svm_parameter param = makeParameters();

        libsvm.svm_model svmModel;
        try {
            svmModel = libsvm.svm.svm_train(prob, param);
        } catch (Exception x) {
            throw new RuntimeException("Could not train classifier", x);
        }
        
        writeClassifier(model, svmModel);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(config.modelFile.toFile(), model);
    }

    public static Map<String, IntSet> indexDocTopics(List<DocumentTopic> docTopics) {
        Object2IntMap<String> docIds = new Object2IntOpenHashMap<>();
        HashMap<String, IntSet> index = new HashMap<>();
        int id = 0;
        for (DocumentTopic dt : docTopics) {
            final int id2;
            if (!docIds.containsKey(dt.document_id)) {
                docIds.put(dt.document_id, id2 = id++);
            } else {
                id2 = id;
            }
            if (!index.containsKey(dt.topic_string)) {
                index.put(dt.topic_string, new IntRBTreeSet());
            }
            index.get(dt.topic_string).add(id2);
        }
        return index;
    }

    public static svm_problem loadInstances(List<List<StringPair>> taxos, Features features, double negSampling) {
        final Random random = new Random();
        ArrayList<String> attributes = buildAttributes(features.featureNames());
        final svm_problem instances = new svm_problem();
        ArrayList<Instance> instanceList = new ArrayList<>();
        for (List<StringPair> taxo : taxos) {
            List<String> topicsList = new ArrayList<>(buildTopics(taxo));
            Set<StringPair> taxoSet = new HashSet<>(taxo); // Faster but uses more memory
            for (StringPair sp : taxo) {
                double[] d1 = features.buildFeatures(sp._1, sp._2);
                instanceList.add(makeInstance(d1, +1));
            }
            System.err.println("positive:" + instanceList.size());
            for (int i = 0; i < negSampling * taxo.size(); i++) {
                int j = random.nextInt(topicsList.size());
                int k = random.nextInt(topicsList.size());
                if (j == k) {
                    continue;
                }
                StringPair topicPair = new StringPair(topicsList.get(j), topicsList.get(k));
                if (!taxoSet.contains(topicPair)) {
                    double[] d1 = features.buildFeatures(topicPair._1, topicPair._2);
                    instanceList.add(makeInstance(d1, 0));
                }
            }
            System.err.println("total:" + instanceList.size());
        }
        instances.x = new svm_node[instanceList.size()][];
        instances.y = new double[instanceList.size()];
        int i = 0;
        for(Instance instance : instanceList) {
            instances.x[i] = instance.x;
            instances.y[i++] = instance.y;
        }
        instances.l = instanceList.size();
        return instances;
    }

    /*private static void writeClassifier(File file, svm_model classifier) throws IOException {
        libsvm.svm.svm_save_model(file.getAbsolutePath(), classifier);
    }*/
    public static void writeClassifier(Model model, svm_model classifier) throws IOException {
        File tmpFile = File.createTempFile("svm", ".data");
        libsvm.svm.svm_save_model(tmpFile.getAbsolutePath(), classifier);
        try (BufferedReader reader = new BufferedReader(new FileReader(tmpFile))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read = 0;
            while((read = reader.read(buf)) >= 0) {
                sb.append(buf, 0, read);
            }
            model.classifierData = sb.toString();
        } finally {
            tmpFile.delete();
        }
    }

    static ArrayList<String> buildAttributes(String[] featureNames) {
        ArrayList<String> attributes = new ArrayList<>();
        for (String name : featureNames) {
            attributes.add(name);
        }
        //attributes.add(new Attribute("score"));
        return attributes;
    }

    private static Set<String> buildTopics(List<StringPair> taxo) {
        Set<String> s = new HashSet<>();
        for (StringPair sp : taxo) {
            s.add(sp._1);
            s.add(sp._2);
        }
        return s;
    }

    static Instance makeInstance(double[] fss, int score) {
        final svm_node[] d = new svm_node[fss.length];
        for(int i = 0; i < fss.length; i++) {
            d[i] = new svm_node();
            d[i].index = i;
            d[i].value = fss[i];
        }
        
        return new Instance(d, score);
    }

    public static Features learnSVD(List<List<StringPair>> taxos, Features features, 
            TaxonomyExtractionConfiguration config, Model model) {
        Matrix ave = learnSVD(taxos, features.svdByAve);
        model.svdAve = ave.getArray();
        Matrix minMax = learnSVD(taxos, features.svdByMinMax);
        model.svdMinMax = minMax.getArray();
        return new Features(ave, minMax, features);
    }

    private static Matrix learnSVD(List<List<StringPair>> taxos, SVD svd) {
        Matrix m = null;
        for (List<StringPair> sps : taxos) {
            for (StringPair sp : sps) {
                svd.addExample(sp._1, sp._2, +1);
                svd.addExample(sp._2, sp._1, -1);
            }
            if (m == null) {
                m = svd.solve();
            } else {
                m.plusEquals(svd.solve());
            }
        }
        if (m != null) {
            m.timesEquals(1.0 / taxos.size());
        }
        return m;
    }

    /*private static void writeMatrix(Matrix matrix, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeInt(matrix.getRowDimension());
            oos.writeInt(matrix.getColumnDimension());
            for (int i = 0; i < matrix.getRowDimension(); i++) {
                for (int j = 0; j < matrix.getColumnDimension(); j++) {
                    oos.writeDouble(matrix.get(i, j));
                }
            }
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }*/

    public static Features makeFeatures(List<DocumentTopic> docTopics, Map<String, Term> topicMap,
            Model model) throws IOException {
        Matrix svdAveMatrix = model.svdAve == null ? null 
                : new Matrix(model.svdAve);
        Matrix svdMinMaxMatrix = model.svdMinMax == null ? null
                : new Matrix(model.svdMinMax);
        final Map<String, double[]> glove = model.features.gloveFile == null ? null : loadGLoVE(model.features.gloveFile.toFile());
        final Set<Hypernym> hypernyms = model.features.hypernyms == null ? null : loadHypernyms(model.features.hypernyms.toFile());
        return new Features(svdAveMatrix, svdMinMaxMatrix, indexDocTopics(docTopics), 
                glove, topicMap, hypernyms, model.features.featureSelection);
    }

    private static Matrix readMatrix(File svdAveFile) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(svdAveFile))) {
            int m = ois.readInt();
            int n = ois.readInt();
            Matrix matrix = new Matrix(m, n);
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < n; j++) {
                    matrix.set(i, j, ois.readDouble());
                }
            }
            return matrix;
        }
    }

    public static Set<Hypernym> loadHypernyms(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
            final CollectionType setOfHypernymType = TypeFactory.defaultInstance().constructCollectionType(HashSet.class, Hypernym.class);
        if(file.getName().endsWith(".gz")) {
            return mapper.readValue(new GZIPInputStream(new FileInputStream(file)), setOfHypernymType);
        } else {
            return mapper.readValue(file, setOfHypernymType);
        }
    }

    public static class StringPair {

        public final String _1, _2;

        public StringPair(String _1, String _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this._1);
            hash = 97 * hash + Objects.hashCode(this._2);
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

        @Override
        public String toString() {
            return "StringPair{" + _1 + ", " + _2 + '}';
        }
        
        

    }
//
//    static svm_model loadClassifier(TaxonomyExtractionConfiguration config) {
//        final String className = config._class;
//        final Class c;
//        try {
//            c = Class.forName(className);
//        } catch (ClassNotFoundException x) {
//            throw new RuntimeException(String.format("%s is not a valid class name", className), x);
//        }
//        final Object o;
//        try {
//            o = c.newInstance();
//        } catch (IllegalAccessException | InstantiationException x) {
//            throw new RuntimeException(x);
//        }
//        if (o instanceof svm_model) {
//            return (svm_model) o;
//        } else {
//            throw new RuntimeException(String.format("%s is not a weka.core.Classifier", className));
//        }
//    }

    public static svm_parameter makeParameters() {
        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.5;
        param.nu = 0.5;
        param.C = 1;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.001;
        return param;
    }

    static class Instance {

        svm_node[] x;
        double y;

        public Instance(svm_node[] x, double y) {
            this.x = x;
            this.y = y;
        }

    }
}
