package org.insightcentre.saffron.web;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
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
     * Add a document term
     * @param id The ID of the run
     * @param date The timestamp
     * @param terms The doc-terms
     * @return True if successful
     */
    boolean addDocumentTermCorrespondence(String id, Date date, List<DocumentTerm> terms);

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
     * Add the result of a term extraction
     * @param id The ID of the run
     * @param date The timestamp
     * @param res The taxonomy to update
     * @return True if successful
     */
    boolean addTermExtraction(String id, Date date, Set<Term> res);

    boolean addTerms(String id, Date date, List<Term> terms);

    boolean addTermsSimilarity(String id, Date date, List<TermTerm> termSimilarity);

    /*
     * (non-Javadoc)
     *
     * @see java.io.Closeable#close()
     */
    void close() throws IOException;

    void deleteRun(String name);

    void deleteTerm(String runId, String term);

    String getRun(String runId);

    void updateRun(String runId, String originalRun, JSONObject json, String status);

    Taxonomy getTaxonomy(String runId);

    List<DocumentTerm> getDocTerms(String runId);

    public List<SaffronRun> getAllRuns();

    public List<String> getTaxoParents(String runId, String termString);

    public List<TermAndScore> getTaxoChildrenScored(String runId, String termString);

    public List<AuthorAuthor> getAuthorSimByAuthor1(String runId, String author1);

    public List<AuthorAuthor> getAuthorSimByAuthor2(String runId, String author1);

    public List<Author> authorAuthorToAuthor1(String runId, List<AuthorAuthor> aas);

    public List<Author> authorAuthorToAuthor2(String runId, List<AuthorAuthor> aas);

    public List<String> getTaxoChildren(String runId, String termString);

    public List<TermTerm> getTermByTerm1(String runId, String term1, List<String> _ignore);

    public List<TermTerm> getTermByTerm2(String runId, String term2);

    public List<AuthorTerm> getTermByAuthor(String runId, String author);

    public List<AuthorTerm> getAuthorByTerm(String runId, String term);

    public List<Author> authorTermsToAuthors(String runId, List<AuthorTerm> ats);

    public List<DocumentTerm> getTermByDoc(String runId, String doc);

    public List<org.insightcentre.nlp.saffron.data.Document> getDocByTerm(String runId, String term);

    public Term getTerm(String runId, String term);

    public List<org.insightcentre.nlp.saffron.data.Document> getDocsByAuthor(String runId, String authorId);

    public Collection<String> getTopTerms(String runId, int from, int to);

    public Author getAuthor(String runId, String authorId);

    public org.insightcentre.nlp.saffron.data.Document getDoc(String runId, String docId);

    public DocumentSearcher getSearcher(String runId);

    public void setDocTerms(String runId, List<DocumentTerm> docTerms);

    public void setIndex(String runId, DocumentSearcher index);

    void setCorpus(String runId, Corpus corpus);

    public void setTerms(String runId, List<Term> terms);

    public void setAuthorTerms(String runId, Collection<AuthorTerm> authorTerms);

    public void setTermSim(String runId, List<TermTerm> termSim);

    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim);

    public void setTaxonomy(String runId, Taxonomy taxonomy);

    public void remove(String runId);

    boolean updateTaxonomy(String id, Taxonomy graph);

    //FIXME It should be called "updateStatus" instead
    boolean updateTerm(String id, String term, String status);

    boolean updateTermName(String id, String term, String newTerm, String status);

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

    public Taxonomy getTaxoDescendent(String runId, String termString);

    public Iterable<org.insightcentre.nlp.saffron.data.Document> getAllDocuments(String datasetName);

    public Iterable<Author> getAllAuthors(String datasetName);

    public Iterable<Term> getAllTerms(String datasetName);

    public Date getDate(String doc);

    public List<AuthorTerm> getAllAuthorTerms(String name);

    public Iterable<DocumentTerm> getDocTermByTerm(String name, String termId);

    public Iterable<TermTerm> getAllTermSimilarities(String name);

    public Iterable<TermTerm> getTermByTerms(String name, String term1, String term2);

    public static class TermAndScore {

        public final String term;
        public final double score;

        public TermAndScore(String term, double score) {
            this.term = term;
            this.score = score;
        }

    }
}
