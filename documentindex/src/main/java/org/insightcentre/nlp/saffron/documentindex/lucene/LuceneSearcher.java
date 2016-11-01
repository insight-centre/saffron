package org.insightcentre.nlp.saffron.documentindex.lucene;

import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcenter.nlp.saffron.documentindex.SearchException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;

/**
 * @author Georgeta Bordea
 * 
 */
public class LuceneSearcher implements DocumentSearcher {
	private Analyzer analyzer;
	private IndexSearcher occurrence_searcher, tfidf_searcher;

	public LuceneSearcher(Directory directory, Analyzer analyzer) throws IOException {
		super();
		this.analyzer = analyzer;
		// Need two searchers to avoid race condition on
		// searcher.setSimilarity() during concurrency
		DirectoryReader reader = DirectoryReader.open(directory);
		occurrence_searcher = new IndexSearcher(reader);
		occurrence_searcher.setSimilarity(new OccBasedSimilarity());
		tfidf_searcher = new IndexSearcher(reader);
		tfidf_searcher.setSimilarity(new OnlyTFIDFSimilarity());
	}

	@Override
	public int numDocs() {
		return occurrence_searcher.getIndexReader().numDocs();
	}

	@Override
	public Map<String, Integer> searchOccurrence(String topic, int docsNo) throws SearchException {
        if(docsNo == 0) {
            return Collections.EMPTY_MAP;
        }
		Map<String, Integer> occurrenceMap = new HashMap<String, Integer>();
		try {
			StandardQueryParser luceneParser = new StandardQueryParser(analyzer);
			Query query = luceneParser.parse("\"" + QueryParserBase.escape(topic) + "\"",
					LuceneDocument.CONTENTS_NAME);

			final TopDocs docs = occurrence_searcher.search(query, docsNo);

			for (int i = 0; i < docs.scoreDocs.length; i++) {
				final Document d = occurrence_searcher.doc(docs.scoreDocs[i].doc);
				Float score = docs.scoreDocs[i].score;
				//Use Math.round instead of intValue because Lucene occasionally has floating point errors (returning 0.9999...)
				occurrenceMap.put(d.get(LuceneDocument.UID_NAME), Math.round(score.floatValue()));
			}
		} catch (IOException | QueryNodeException e) {
			throw new SearchException(e.getMessage(), e);
		}

		return occurrenceMap;
	}

	private SpanNearQuery makeSpan(String term, int slop) throws IOException {
		List<String> termWords = analyseTerm(term);

		SpanTermQuery[] query = new SpanTermQuery[termWords.size()];
		for (int i = 0; i < termWords.size(); i++) {
			String word = termWords.get(i);
			Term t = new Term(LuceneDocument.CONTENTS_NAME, word);
			query[i] = new SpanTermQuery(t);
		}
		SpanNearQuery snq = new SpanNearQuery(query, slop, true);
		return snq;
	}
	
	@Override
	public Map<String, Integer> searchSpanOccurrence(String term1, String term2, int docsNo, int spanSlop)
			throws SearchException {

		Map<String, Integer> occurrenceMap = new HashMap<String, Integer>();
		try {
			SpanNearQuery q1 = makeSpan(term1, 0);
			SpanNearQuery q2 = makeSpan(term2, 0);

			SpanNearQuery[] snqArray = new SpanNearQuery[] { q1, q2 };
			SpanNearQuery snq = new SpanNearQuery(snqArray, spanSlop, false);

			final TopDocs docs = occurrence_searcher.search(snq, docsNo);

			for (int i = 0; i < docs.scoreDocs.length; i++) {
				final Document d = occurrence_searcher.doc(docs.scoreDocs[i].doc);
				Float score = docs.scoreDocs[i].score;
				occurrenceMap.put(d.get(LuceneDocument.UID_NAME), Math.round(score.intValue()));
			}
		} catch (IOException e) {
			throw new SearchException(e.getMessage(), e);
		}

		return occurrenceMap;
	}

	public List<String> analyseTerm(String term) throws IOException {
		List<String> words = new ArrayList<String>();

		TokenStream ts = analyzer.tokenStream(LuceneDocument.CONTENTS_NAME, new StringReader(term));
		CharTermAttribute termAttr = ts.getAttribute(CharTermAttribute.class);
		try {
			ts.reset();
			while (ts.incrementToken()) {
				words.add(termAttr.toString());
			}
	    } finally {
	        ts.close(); // Release resources associated with this stream.
	    }

		return words;
	}

	@Override
	public Map<String, Float> searchTFIDF(List<String> topicList, int docsNo) throws SearchException {

		Map<String, Float> tfidfMap = new HashMap<String, Float>();
		try {
			StandardQueryParser luceneParser = new StandardQueryParser(analyzer);

			BooleanQuery blQuery = new BooleanQuery();

			for (String topic : topicList) {
				Query query = luceneParser.parse("\"" + QueryParserBase.escape(topic) + "\"",
						LuceneDocument.CONTENTS_NAME);
				blQuery.add(query, BooleanClause.Occur.SHOULD);
			}

			final TopDocs docs = tfidf_searcher.search(blQuery, docsNo);

			for (int i = 0; i < docs.scoreDocs.length; i++) {
				final Document d = tfidf_searcher.doc(docs.scoreDocs[i].doc);
				Float score = docs.scoreDocs[i].score;
				tfidfMap.put(d.get(LuceneDocument.UID_NAME), score);
			}
		} catch (IOException | QueryNodeException e) {
			throw new SearchException(e.getMessage(), e);
		}

		return tfidfMap;
	}

	@Override
	public Map<String, Float> tfidf(String keyphrase, int maxDocumentResults) throws SearchException {
		List<String> topicMorphVar = new ArrayList<String>();
		topicMorphVar.add(keyphrase.toLowerCase());

		return searchTFIDF(topicMorphVar, maxDocumentResults);
	}

	@Override
	public Long numberOfOccurrences(String term, int maxDocumentResults) throws SearchException {

		Long frequency = new Long(0);

		Map<String, Integer> occMap = searchOccurrence(term.toLowerCase(), maxDocumentResults);
		Set<String> keySet = occMap.keySet();

		for (String docId : keySet) {
			frequency += occMap.get(docId);
		}

		return frequency;
	}

	@Override
	public Long spanOccurrence(String term1, String term2, int spanSlop, int maxDocumentResults)
			throws SearchException {
		Long occurrence = new Long(0);

		Map<String, Integer> occMap = searchSpanOccurrence(term1, term2, maxDocumentResults, spanSlop);
		for (Integer occ : occMap.values()) {
			occurrence += occ;
		}

		return occurrence;
	}

	@Override
	public void close() throws IOException {
		occurrence_searcher.getIndexReader().close();
		tfidf_searcher.getIndexReader().close();
	}
}
