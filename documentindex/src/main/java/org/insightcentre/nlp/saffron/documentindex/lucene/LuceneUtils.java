package org.insightcentre.nlp.saffron.documentindex.lucene;

import org.insightcenter.nlp.saffron.documentindex.SearchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;

public class LuceneUtils {
    public static Map<String, String> stemAll(List<String> kpList) throws SearchException {
        Map<String, String> resultMap = new HashMap<String, String>();

        Analyzer analyzer = new AnalyzerStemmedLower();

        StandardQueryParser luceneParser = new StandardQueryParser(analyzer);

        Query parsedKey;
        for (String kp : kpList) {
            try {
                parsedKey = luceneParser.parse(kp, LuceneDocument.CONTENTS_NAME);
            } catch (QueryNodeException e) {
                throw new SearchException("Failed to stem "+kp);
            }
            String parsedKeyString =
                    parsedKey.toString(LuceneDocument.CONTENTS_NAME);

            resultMap.put(kp, parsedKeyString);

        }

        return resultMap;
    }
}
