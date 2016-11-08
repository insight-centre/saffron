package org.insightcentre.nlp.saffron.data.index;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.insightcentre.nlp.saffron.data.Document;

public interface DocumentSearcher extends Closeable {

	public List<String> analyseTerm(String term) throws IOException;
	
	public int numDocs();

	/**
	 * Compute occurrence using lucene for a given topic
	 * 
	 * @param topic
	 *            the topics string
	 * @param docsNo
	 *            maximum documents number
	 * @return the map with occurrences for all documents where the topic is
	 *         mentioned
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public Map<String, Integer> searchOccurrence(String topic, int maxDocumentResults) throws SearchException;

	/**
	 * Compute the occurrence of two terms that have spanSlop or less words
	 * between them.
	 * 
	 * e.g. term1="a", term2="b", spanSlop=5 
	 *     blah a blah blah blah blah blah b blah blah blah
	 * 
	 *     term1^ |----------------------| ^term2 
	 *         5 or less words between them
	 * 
	 * @param docsNo
	 *            the number of indexed documents
	 * @param spanSlop
	 *            the size of the window the win
	 * @return the occurrence in each file
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public Map<String, Integer> searchSpanOccurrence(String term1, String term2, int maxDocumentResults,
			int spanSlop) throws SearchException;

	/**
	 * Compute tfidf using lucene for a given topic using all morphological
	 * variations to calculate the tfidf.
	 * 
	 * @param topicList
	 *            the list of morphological variations of a topic
	 * @param docsNo
	 *            maximum documents number
	 * @return the map with tfidf values for all documents where the topic is
	 *         mentioned
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public Map<String, Float> searchTFIDF(List<String> topicList, int maxDocumentResults) throws SearchException;

	/**
	 * Compute the number of times a term occurs in the index. (Note: Case insensitive).
	 * 
	 * @param term
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public Long numberOfOccurrences(String term, int maxDocumentResults) throws SearchException;

	/**
	 * Search tfidf for a given termextraction in all indexed documents
	 * 
	 * @param keyphrase
	 * @param maxDocumentResults
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public Map<String, Float> tfidf(String keyphrase, int maxDocumentResults) throws SearchException;

	public Long spanOccurrence(String term1, String term2, int spanSlop, int maxDocumentResults)
			throws SearchException;

    /**
     * Get all document contents
     * @return The document contents
     * @throws SearchException  If an error occurs when searching
     */
    public Iterable<Document> allDocuments() throws SearchException;

}