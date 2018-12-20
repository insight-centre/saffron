package org.insightcentre.nlp.saffron.term.lda;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.index.SearchException;

/**
 * Converts a Saffron Corpus into an assignment buffer for the LDA algorithm
 *
 * @author John McCrae
 */
public class CorpusProcessor {

    public static Result convert(Corpus searcher, ThreadLocal<Tokenizer> tokenizer) throws IOException, SearchException {
        final Object2IntMap<String> dictionary = new Object2IntOpenHashMap<>();
        final File tmpFile = File.createTempFile("assign", ".buf");
        tmpFile.deleteOnExit();
        int J = 0;
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(tmpFile))) {
            for (Document doc : searcher.getDocuments()) {

                String contents = doc.contents();
                for (String sentence : contents.split("\n")) {

                    String[] tokens = tokenizer.get().tokenize(sentence.toLowerCase());
                    for (String token : tokens) {
                        final int i;
                        if (dictionary.containsKey(token)) {
                            i = dictionary.getInt(token);
                        } else {
                            i = dictionary.size();
                            dictionary.put(token, i);
                        }
                        out.writeInt(i);
                        out.writeInt(0);
                    }
                }
                out.writeInt(-1);
                out.writeInt(0);
                J++;
            }
        }
        return new Result(
                new AssignmentBuffer(new RandomAccessFile(tmpFile, "rw").getChannel(), 4194304, tmpFile.length()),
                J, dictionary);

    }

    public static class Result {

        final public AssignmentBuffer buffer;
        final public int docCount;
        final public Object2IntMap<String> dictionary;

        public Result(AssignmentBuffer buffer, int docCount, Object2IntMap<String> dictionary) {
            this.buffer = buffer;
            this.docCount = docCount;
            this.dictionary = dictionary;
        }

    }
}
