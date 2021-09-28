package org.insightcentre.nlp.saffron.run;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;

/**
 * Extends the Saffron run listener by providing methods that consumes results
 * 
 * @author John P. McCrae
 */
public interface SaffronRunListener extends SaffronListener {

	public void setDomainModelTerms(String saffronDatasetName, Set<Term> terms);
	
    public void setTerms(String saffronDatasetName, List<Term> terms);

    public void setDocTerms(String saffronDatasetName, List<DocumentTerm> docTerms);

    public void setCorpus(String saffronDatasetName, Corpus searcher);

    public void setAuthorTerms(String saffronDatasetName, Collection<AuthorTerm> authorTerms);

    public void setTermSim(String saffronDatasetName, List<TermTerm> termSimilarity);

    public void setAuthorSim(String saffronDatasetName, List<AuthorAuthor> authorSim);

    public void setTaxonomy(String saffronDatasetName, Taxonomy graph);

    public void setKnowledgeGraph(String saffronDatasetName, KnowledgeGraph kGraph);
    
}
