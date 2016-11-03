package org.insightcentre.nlp.saffron.topic.gate;

import gate.Factory;
import gate.util.GateException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.insightcentre.nlp.saffron.topic.ExtractedTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class GateProcessorConsumer {

    public static final String TOKENS_NUMBER_FEATURE_NAME = "tokensNumber";
    public static final String TOPICS_LIST_FEATURE_NAME = "topicsList";

    private final GateProcessor gateProc;

    public GateProcessorConsumer(GateProcessor gateProc) {
        this.gateProc = gateProc;
    }

    @SuppressWarnings("unchecked")
    public List<ExtractedTopic> process(gate.Document document, List<String> domainModel)
        throws GateException, InterruptedException {

        /**
         * we accumulate the topics here
         */
        List<ExtractedTopic> topicbearer = new ArrayList<>();

        // retrieve a GateProcessor from the Processor Pool, fail after a
        // timeout
        //GateProcessor gateProc = pool.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        TemporaryGazetteerDirectory tempGazetteer;

        try {
            tempGazetteer = new TemporaryGazetteerDirectory(domainModel);
            gateProc.setGazetteerListsURL(tempGazetteer.getListsURL());
        } catch (IOException e) {
            throw new GateException("Unable to get GATE gazetteer", e);
        }

        // do processing here
        try {
            gateProc.getCorpus().add(document);
            gateProc.getController().execute();

                // do stuff with annotated document, actually only return
            // topic bearer as container holding the frequency maps
            // stored in the GATE
            topicbearer = (List<ExtractedTopic>) document
                .getFeatures().get(TOPICS_LIST_FEATURE_NAME);

//                topicbearer.setTokensNumber((Integer) document.getFeatures().get(
//                        TopicExtractor.TOKENS_NUMBER_FEATURE_NAME));
        } finally {

            // clean up
            cleanUpGate(document, gateProc);

            try {
                tempGazetteer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            document = null;
                // and nicely return the GateProcessor back to the pool in case it
            // is not null
        }

        return topicbearer;
    }

    /**
     * @param document
     * @param gateProc
     */
    private void cleanUpGate(gate.Document document, GateProcessor gateProc) {
        gateProc.getCorpus().unloadDocument(document);
        gateProc.getCorpus().cleanup();
        gateProc.getCorpus().clear();
        document.cleanup();
        Factory.deleteResource((gate.Document) document);
    }

    public static class TemporaryGazetteerDirectory implements Closeable {

        private File listsFile;
        private Path tempPath;

        public TemporaryGazetteerDirectory(List<String> domainModel) throws IOException {
            tempPath = Files.createTempDirectory(UUID.randomUUID().toString());

            listsFile = tempPath.resolve("lists.def").toFile();
            FileUtils.writeStringToFile(listsFile, "domainModel.lst:domainModel\n");

            File domainModelPath = tempPath.resolve("domainModel.lst").toFile();
            String domainWordList = StringUtils.join(domainModel, "\n");
            FileUtils.writeStringToFile(domainModelPath, domainWordList);
        }

        public URL getListsURL() throws MalformedURLException {
            return listsFile.toURI().toURL();
        }

        @Override
        public void close() throws IOException {
            FileUtils.deleteDirectory(tempPath.toFile());
        }
    }

}
