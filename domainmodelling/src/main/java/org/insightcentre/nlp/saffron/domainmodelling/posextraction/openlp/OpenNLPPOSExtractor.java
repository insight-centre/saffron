package org.insightcentre.nlp.saffron.domainmodelling.posextraction.openlp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSBearer;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSExtractor;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.UnsupportedFileTypeException;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class OpenNLPPOSExtractor implements POSExtractor {
    private final POSTaggerME tagger;
    private final Tokenizer tokenizer;
    private final Chunker chunker;

    public OpenNLPPOSExtractor(File tokenizerModelFile, File posModelFile, File chunkModelFile) throws IOException {
        this.tagger = new POSTaggerME(new POSModel(posModelFile));
        this.tokenizer = new TokenizerME(new TokenizerModel(tokenizerModelFile));
        this.chunker = new ChunkerME(new ChunkerModel(chunkModelFile));
    }


    
    @Override
    public POSBearer getPOSFromUrl(String documentUrl) throws MalformedURLException, UnsupportedFileTypeException, RuntimeException {
        URL url = new URL(documentUrl);
        StringBuilder data = new StringBuilder();
        try(InputStream is = url.openStream()) {
            byte[] buf = new byte[1024];
            int i = 0;
            while((i = is.read(buf)) >= 0) {
                data.append(new String(buf));
            }
        } catch(IOException x) {
            throw new RuntimeException(x);
        } 
        return getPOSFromText(data.toString());
    }

    @Override
    public POSBearer getPOSFromText(String documentText) throws RuntimeException {
        String[] tokens = tokenizer.tokenize(documentText);
        String[] tags = tagger.tag(tokens);
        String[] chunks = chunker.chunk(tokens, tags);

        assert(tags.length == tokens.length);
        List<String> nounList = new ArrayList<>(), verbList = new ArrayList<>(),
            adjectiveList = new ArrayList<>();
        Map<String, Integer> nounphraseMap = new HashMap<>();
        
        for(int i = 0; i < tokens.length; i++) {
            if(tags[i].startsWith("N")) {
                nounList.add(tokens[i]);
            } else if(tags[i].startsWith("V")) {
                verbList.add(tokens[i]);
            } else if(tags[i].startsWith("J")) {
                adjectiveList.add(tokens[i]);
            }
        }

        StringBuilder currentNP = new StringBuilder();
        int currentNPlen = 0;
        for(int i = 0; i < chunks.length; i++) {
            if(chunks[i].equals("B-NP")) {
                currentNP.append(tokens[i]);
                currentNPlen = 1;
            } else if(chunks[i].equals("I-NP")) {
                currentNP.append(" ").append(tokens[i]);
                currentNPlen++;
            } else if(currentNPlen > 0) {
                nounphraseMap.put(currentNP.toString(), currentNPlen);
                currentNPlen = 0;
                currentNP.delete(0, currentNP.length());
            }
        }
        if(currentNPlen > 0) {
            nounphraseMap.put(currentNP.toString(), currentNPlen);
        }

        return new OpenNLPPOSBearer(nounList, verbList, adjectiveList, nounphraseMap, tokens.length);
    }

    private static final class OpenNLPPOSBearer implements POSBearer {
        private List<String> nounList;
        private List<String> verbList;
        private List<String> adjectiveList;
        private Map<String, Integer> nounphraseMap;
        private Integer tokensNo;

        public OpenNLPPOSBearer() {
        }


        public OpenNLPPOSBearer(List<String> nounList, List<String> verbList, List<String> adjectiveList, Map<String, Integer> nounphraseMap, Integer tokensNo) {
            this.nounList = nounList;
            this.verbList = verbList;
            this.adjectiveList = adjectiveList;
            this.nounphraseMap = nounphraseMap;
            this.tokensNo = tokensNo;
        }

        @Override
        public Map<String, Integer> getNounphraseMap() {
            return nounphraseMap;
        }

        @Override
        public void setNounphraseMap(Map<String, Integer> nounphraseMap) {
            this.nounphraseMap = nounphraseMap;
        }

        @Override
        public List<String> getNounList() {
            return nounList;
        }

        @Override
        public void setNounList(List<String> nounList) {
            this.nounList = nounList;
        }

        @Override
        public List<String> getVerbList() {
            return verbList;
        }

        @Override
        public void setVerbList(List<String> verbList) {
            this.verbList = verbList;
        }

        @Override
        public List<String> getAdjectiveList() {
            return adjectiveList;
        }

        @Override
        public void setAdjectiveList(List<String> adjectiveList) {
            this.adjectiveList = adjectiveList;
        }

        @Override
        public Integer getTokensNo() {
            return tokensNo;
        }

        @Override
        public void setTokensNo(Integer tokensNo) {
            this.tokensNo = tokensNo;
        }
    }
}
