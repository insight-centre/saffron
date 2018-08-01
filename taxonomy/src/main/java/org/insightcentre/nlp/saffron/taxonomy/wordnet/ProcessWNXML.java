package org.insightcentre.nlp.saffron.taxonomy.wordnet;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is used to translate a WordNet XML dump into a hypernym file, as follows:
 * 
 * mvn exec:java -Dexec.mainClass="org.insightcentre.nlp.saffron.taxonomy.wordnet.ProcessWNXML" -Dexec.args="../../../jmccrae/gwn-scala-api/wordnets/wn31.xml ../models/wn-hyps.json.gz"
 *
 * @author John McCrae
 */
public class ProcessWNXML {

    public static void main(String[] args) throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();
        WNXMLHandler handler = new WNXMLHandler();
        saxParser.parse(new File(args[0]), handler);
        Set<Hypernym> hypernyms = new HashSet<>();
        for(Map.Entry<String, Set<String>> e : handler.hypernyms.entrySet()) {
            for(String hyp : e.getValue()) {
                for(String hypTerm : getAllHyps(hyp, handler)) {
                    for(String hypoTerm : handler.synsetWords.get(e.getKey())) {
                        hypernyms.add(new Hypernym(hypoTerm, hypTerm));
                    }
                }
            }   
        }
        ObjectMapper mapper = new ObjectMapper();
        GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(args[1]));
        mapper.writerWithDefaultPrettyPrinter().writeValue(gos, hypernyms);
    }

    private static Set<String> getAllHyps(String hyp, WNXMLHandler handler) {
        Set<String> values = new HashSet<>();
        values.addAll(handler.synsetWords.get(hyp));
        if(handler.hypernyms.containsKey(hyp)) {
            for(String hyp2 : handler.hypernyms.get(hyp)) {
                values.addAll(getAllHyps(hyp2, handler));
            }
        }
        return values;
    }

    public static class WNXMLHandler extends DefaultHandler {
        public final HashMap<String, Set<String>> synsetWords = new HashMap<>();
        public final HashMap<String, Set<String>> hypernyms = new HashMap<>();
        private String lemma;
        private String synsetRef;

        @Override
        public void startElement(String uri, String _localName, String qName, Attributes attributes) throws SAXException {
            if("Lemma".equals(qName)) {
                lemma = attributes.getValue("writtenForm");
            } else if("Sense".equals(qName)) {
                synsetRef = attributes.getValue("synset");
                if(!synsetWords.containsKey(synsetRef)) {
                    synsetWords.put(synsetRef, new HashSet<String>());
                }
                synsetWords.get(synsetRef).add(lemma);
            } else if("Synset".equals(qName)) {
                synsetRef = attributes.getValue("id");
            } else if("SynsetRelation".equals(qName)) {
                if("hypernym".equals(attributes.getValue("relType"))) {
                    String target = attributes.getValue("target");
                    if(!hypernyms.containsKey(synsetRef)) {
                        hypernyms.put(synsetRef, new HashSet<String>());
                    }
                    hypernyms.get(synsetRef).add(target);
                }
            }
            super.startElement(uri, _localName, qName, attributes); 
        }
    }
}

