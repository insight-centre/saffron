package org.insightcentre.saffron.web;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.json.JSONObject;

/**
 * Interface for accessing Saffron data storage, used by the API
 *
 * @author John McCrae
 */
public interface SaffronDataSource extends Closeable {

    /**
     * Add an author similarity
     * @param id The ID of the run
     * @param date The date to timestamp (now)
     * @param authorSim The similarities
     * @return True if successful
     */
    boolean addAuthorSimilarity(String id, Date date, List<AuthorAuthor> authorSim);


    /**
     * Add a document topic
     * @param id The ID of the run
     * @param date The timestamp
     * @param topics The doc-topics
     * @return True if successful
     */
    boolean addDocumentTopicCorrespondence(String id, Date date, List<DocumentTerm> topics);

    /**
     * Create a new run
     * @param id The ID of the run
     * @param date The timestamp
     * @return  True if successful
     */
    boolean addRun(String id, Date date, Configuration config);

    /**
     * Update the current taxonomy
     * @param id The ID of the run
     * @param date The timestamp
     * @param graph The taxonomy to update
     * @return True if successful
     */
    boolean addTaxonomy(String id, Date date, Taxonomy graph);

    /**
     * Add the result of a topic extraction
     * @param id The ID of the run
     * @param date The timestamp
     * @param res The taxonomy to update
     * @return True if successful
     */
    boolean addTopicExtraction(String id, Date date, Set<Term> res);

    boolean addTopics(String id, Date date, List<Term> topics);

    boolean addTopicsSimilarity(String id, Date date, List<TopicTopic> topicSimilarity);

    /*
     * (non-Javadoc)
     *
     * @see java.io.Closeable#close()
     */
    void close() throws IOException;

    void deleteRun(String name);

    void deleteTopic(String runId, String topic);

    String getRun(String runId);

    Taxonomy getTaxonomy(String runId);

    List<DocumentTerm> getDocTopics(String runId);

    //public List<String> getAllRuns();

    public List<String> getTaxoParents(String runId, String topic_string);

    public List<TopicAndScore> getTaxoChildrenScored(String runId, String topic_string);

    public List<AuthorAuthor> getAuthorSimByAuthor1(String runId, String author1);

    public List<AuthorAuthor> getAuthorSimByAuthor2(String runId, String author1);

    public List<Author> authorAuthorToAuthor1(String runId, List<AuthorAuthor> aas);

    public List<Author> authorAuthorToAuthor2(String runId, List<AuthorAuthor> aas);

    public List<String> getTaxoChildren(String runId, String topic_string);

    public List<TopicTopic> getTopicByTopic1(String runId, String topic1, List<String> _ignore);

    public List<TopicTopic> getTopicByTopic2(String runId, String topic2);

    public List<AuthorTopic> getTopicByAuthor(String runId, String author);

    public List<AuthorTopic> getAuthorByTopic(String runId, String topic);

    public List<Author> authorTopicsToAuthors(String runId, List<AuthorTopic> ats);

    public List<DocumentTerm> getTopicByDoc(String runId, String doc);

    public List<org.insightcentre.nlp.saffron.data.Document> getDocByTopic(String runId, String topic);

    public Term getTopic(String runId, String topic);

    public List<org.insightcentre.nlp.saffron.data.Document> getDocsByAuthor(String runId, String authorId);

    public Collection<String> getTopTopics(String runId, int from, int to);

    public Author getAuthor(String runId, String authorId);

    public org.insightcentre.nlp.saffron.data.Document getDoc(String runId, String docId);

    public DocumentSearcher getSearcher(String runId);

    public void setDocTopics(String runId, List<DocumentTerm> docTopics);

    public void setIndex(String runId, DocumentSearcher index);

    void setCorpus(String runId, Corpus corpus);

    public void setTopics(String runId, List<Term> _topics);

    public void setAuthorTopics(String runId, Collection<AuthorTopic> authorTopics);

    public void setTopicSim(String runId, List<TopicTopic> topicSim);

    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim);

    public void setTaxonomy(String runId, Taxonomy taxonomy);

    public void remove(String runId);

    boolean updateTaxonomy(String id, Taxonomy graph);

    //FIXME It should be called "updateStatus" instead
    boolean updateTopic(String id, String topic, String status);

    boolean updateTopicName(String id, String topic, String newTopic, String status);

//    default BlackWhiteList extractBlackWhiteList(String datasetName) {
//        if(!getAllTopics(datasetName).iterator().hasNext())
//            return new BlackWhiteList();
//        else {
//            return BlackWhiteList.from(getAllTopics(datasetName), getTaxonomy(datasetName));
//
//        }
//    }

    /**
     * Is this dataset present
     * @param id The id of the dataset
     * @return True if the dataset exists
     */
    boolean containsKey(String id) throws IOException;

    /**
     * Is the dataset loaded?
     * @param id The id of the dataset
     * @return True if the system is ready
     */
    boolean isLoaded(String id);

    public Iterable<String> runs();

    public Taxonomy getTaxoDescendent(String runId, String topicString);

    public Iterable<org.insightcentre.nlp.saffron.data.Document> getAllDocuments(String datasetName);

    public Iterable<Author> getAllAuthors(String datasetName);

    public Iterable<Term> getAllTopics(String datasetName);

    public Date getDate(String doc);

    public List<AuthorTopic> getAllAuthorTopics(String name);

    public Iterable<DocumentTerm> getDocTopicByTopic(String name, String topicId);

    public Iterable<TopicTopic> getAllTopicSimilarities(String name);

    public Iterable<TopicTopic> getTopicByTopics(String name, String topic1, String topic2);

    public static class TopicAndScore {

        public final String topic;
        public final double score;

        public TopicAndScore(String topic, double score) {
            this.topic = topic;
            this.score = score;
        }

    }
}
