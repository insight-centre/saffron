package org.insightcentre.saffron.web;

import java.io.Closeable;
import java.io.IOException;
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
import org.insightcentre.saffron.web.exception.ConceptNotFoundException;
import org.insightcentre.saffron.web.exception.TermNotFoundException;
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
     * Add a Partonomy
     * @param id The ID of the run
     * @param date The timestamp
     * @param graph The taxonomy to update
     * @return True if successful
     */
    boolean addPartonomy(String id, Date date, Partonomy graph);

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
     * Concept manipulation
     */

    public List<Concept> getAllConcepts(String runId);

    public Concept getConcept(String runId, String conceptId);

    public List<Concept> getConceptsByPreferredTermString(String runId, String preferredTermString);

    public void addConcepts(String runId, List<Concept> concepts);

    public void addConcept(String runId, Concept conceptToBeAdded) throws TermNotFoundException;

    public void updateConcept(String runId, Concept conceptToBeUpdated) throws ConceptNotFoundException, TermNotFoundException;

    public void removeConcept(String runId, String conceptId) throws ConceptNotFoundException;

    /*
     * Author manipulation
     */
    
    public Iterable<Author> getAllAuthors(String datasetName);

    public Author getAuthor(String runId, String authorId);

    public void addAuthors(String runId, List<Author> authors);

    public void addAuthor(String runId, Author authorToBeAdded) throws Exception;
    
    /*
     * Author-Term relations
     */
    
    public List<AuthorTerm> getAuthorTermRelationsPerTerm(String runId, String termId);
    
    public List<AuthorTerm> getAuthorTermRelationsPerAuthor(String runId, String authorId);

    
    /*
     * (non-Javadoc)
     *
     * @see java.io.Closeable#close()
     */
    void close() throws IOException;

    void deleteRun(String name);

    void deleteTerm(String runId, String term);

    String getRun(String runId);

    void updateRun(String runId, String originalRun, String json, String status);

    Taxonomy getTaxonomy(String runId);

    Partonomy getPartonomy(String runId);

    KnowledgeGraph getKnowledgeGraph(String runId);

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

    public List<org.insightcentre.nlp.saffron.data.Document> getDocsByTerm(String runId, String term);

    public Term getTerm(String runId, String term);

    public List<org.insightcentre.nlp.saffron.data.Document> getDocsByAuthor(String runId, String authorId);

    public Collection<String> getTopTerms(String runId, int from, int to);

    public org.insightcentre.nlp.saffron.data.Document getDoc(String runId, String docId);

    public Corpus getSearcher(String runId);

    public void setDocTerms(String runId, List<DocumentTerm> docTerms);

    public void setIndex(String runId, Corpus index);

    void setCorpus(String runId, Corpus corpus);

    public void setTerms(String runId, List<Term> terms);

    public void setAuthorTerms(String runId, Collection<AuthorTerm> authorTerms);

    public void setTermSim(String runId, List<TermTerm> termSim);

    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim);

    public void setTaxonomy(String runId, Taxonomy taxonomy);

    public void setKnowledgeGraph(String runId, KnowledgeGraph knowledgeGraph);

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

    public Iterable<Term> getAllTerms(String datasetName);

    public Date getDate(String doc);

    public List<AuthorTerm> getAllAuthorTerms(String name);

    public Iterable<DocumentTerm> getDocTermByTerm(String name, String termId);

    public Iterable<TermTerm> getAllTermSimilarities(String name);

    public Iterable<TermTerm> getTermByTerms(String name, String term1, String term2);

    public List<AuthorAuthor> getAuthorSimilarity(String runId, String authorId);

    public static class TermAndScore {

        public final String term;
        public final double score;

        public TermAndScore(String term, double score) {
            this.term = term;
            this.score = score;
        }

    }
}
