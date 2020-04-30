package org.insightcentre.saffron.web;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Partonomy;
import org.insightcentre.nlp.saffron.data.SaffronRun;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.VirtualRootTaxonomy;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.exceptions.InvalidOperationException;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;
import org.insightcentre.saffron.web.api.AuthorTermDAO;

/**
 * Service to connect with other Saffron components and
 * perform overall operations
 * 
 * @author Bianca Pereira
 *
 */
public class SaffronService {
	
	private SaffronDataSource dataSource;

    public SaffronService(SaffronDataSource dataSource) {
    	this.dataSource = dataSource;
    }


	/**
	 * Return a Taxonomy for a given taxonomy ID.
	 *
	 * @param taxonomyId - the identifier of the taxonomy
	 */
	public Taxonomy getTaxonomy(String taxonomyId) {

		if (taxonomyId == null || taxonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}
		try {
			return dataSource.getTaxonomy(taxonomyId);
		} catch (Exception e) {
			throw new RuntimeException("The taxonomy " + taxonomyId + " could not be retrieved: " + e.getMessage());
		}
	}

	/**
	 * Return all previous Saffron Runs to the user.
	 *
	 */
	public List<SaffronRun> getAllRuns() {
		try {
			return dataSource.getAllRuns();
		} catch (Exception e) {
			throw new RuntimeException("No Saffron Runs could not be retrieved from the data source: " + e.getMessage());
		}
	}

	/**
	 * Deletes a specified Saffron run for a given Taxonomy ID
	 * @param taxonomyId - the identifier of the taxonomy
	 *
	 */
	public void deleteRun(String taxonomyId) {
        if (taxonomyId == null || taxonomyId.equals("")) {
            InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
            exception.addParameterValue("taxonomyId", "");
            throw exception;
        }
        File directory = new File(this.getExecutor().getParentDirectory().getPath() + "/" + taxonomyId );
		try {
			dataSource.deleteRun(taxonomyId);
            FileUtils.deleteDirectory(directory);
		} catch (Exception e) {
			throw new RuntimeException("The Saffron run " + taxonomyId + " could not be deleted: " + e.getMessage());
		}
	}

	/**
	 * Get all Terms for a given taxonomy ID.
	 * @param taxonomyId - the identifier of the taxonomy
	 *
	 */
	public Iterable<Term> getAllTerms(String taxonomyId) {
        if (taxonomyId == null || taxonomyId.equals("")) {
            InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
            exception.addParameterValue("taxonomyId", "");
            throw exception;
        }
		try {
			return dataSource.getAllTerms(taxonomyId);
		} catch (Exception e) {
			throw new RuntimeException(
					"No terms could be retrieved for the taxonomy " + taxonomyId + ": " + e.getMessage());
		}
	}

	/**
	 * Deletes a term from a given taxonomy
	 * @param taxonomyId - the identifier of the taxonomy
	 * @param termID - the identifier of the term to be deleted
	 *
	 */
	public void deleteTerm(String taxonomyId, String termID) {
        if (taxonomyId == null || taxonomyId.equals("")) {
            InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
            exception.addParameterValue("taxonomyId", "");
            throw exception;
        }
        if (termID.equals("") || termID == null) {
            InvalidValueException exception = new InvalidValueException("The term id cannot be empty");
            exception.addParameterValue("termID", "");
            throw exception;
        }
		try {
			dataSource.deleteTerm(taxonomyId, termID);
		} catch (Exception e) {
			throw new RuntimeException("The Saffron term " + termID + " could not be deleted from the taxonomy: " + taxonomyId + ": " + e.getMessage());
		}
	}


	/**
	 * This method updates the term status for a given term and taxonomy ID
	 * @param taxonomyId - the identifier of the taxonomy
	 * @param termID - the identifier of the term to be deleted
	 * @param status - the new status for the given term identifier
	 *
	 */
	public void updateTerm(String taxonomyId, String termID, String status) {
		if (taxonomyId == null || taxonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}

        if (termID == null || termID.equals("")) {
            InvalidValueException exception = new InvalidValueException("The term id cannot be empty");
            exception.addParameterValue("termID", "");
            throw exception;
        }
        if (status == null || status.equals("")) {
            InvalidValueException exception = new InvalidValueException("The status cannot be empty");
            exception.addParameterValue("status", "");
            throw exception;
        }

		boolean termUpdated = dataSource.updateTerm(taxonomyId, termID, status);
		if(!termUpdated)
			throw new RuntimeException("An error has ocurred when updating the term in the database.");
	}


	/**
	 * Update the Taxonomy for a given Taxonomy ID with a new Taxonomy Object
	 * @param taxonomyId - the identifier of the taxonomy
	 * @param taxonomy - The taxonomy object that should be updated
	 *
	 */
	public void updateTaxonomy(String taxonomyId, Taxonomy taxonomy) {
		if (taxonomyId == null || taxonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}
		if (taxonomy.root == null || taxonomy.root.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy root string cannot be empty or null");
			exception.addParameterValue("taxonomy.root", taxonomy.root);
			throw exception;
		}

		boolean taxonomyUpdated = dataSource.updateTaxonomy(taxonomyId, taxonomy);
		if(!taxonomyUpdated)
			throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
	}

    
	/**
	 * Update the revision status of multiple terms in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param terms - the terms to be modified
	 */
	public void updateTermStatus(String taxonomyId, List<Term> terms) {
		RuntimeException agException = null;
		
    	for(Term term: terms){
    		try {
    			updateTermStatus(taxonomyId,term);
    		} catch (Exception e) {
    			if (agException == null)
    				agException = new RuntimeException("Some terms were not updated: " + e.getMessage());
    			agException.addSuppressed(e);
    		}
		}
    	
    	if (agException != null)
    		throw agException;
	}

	/**
	 * Update the revision status of a single term in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param term - the term to be modified
	 */
	public void updateTermStatus(String taxonomyId, Term term) {
		/*
		 * 1 - Change term status in the database.
		 * 2 - If new status = "rejected" then
		 * 3 - remove the term from the taxonomy and update the taxonomy in the database
		 */
		if (taxonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}		
		if (term.getString() == null || term.getString().equals("")) {
			InvalidValueException exception = new InvalidValueException("The term string cannot be empty or null");
			exception.addParameterValue("term.string", term.getString());
			throw exception;
		}
		if (term.getStatus() == null) {
			InvalidValueException exception = new InvalidValueException("The term status cannot be null");
			exception.addParameterValue("term.status", term.getStatus());
			throw exception;
		}
		
		Term dbTerm = dataSource.getTerm(taxonomyId, term.getString());
		if (dbTerm.getStatus().equals(Status.rejected))
			throw new InvalidOperationException("The status of a 'rejected' term cannot be modified.");
		
		// 1 - Change term status in the database.
		boolean termUpdated = dataSource.updateTerm(taxonomyId, term.getString(), term.getStatus().toString());
		if(!termUpdated)
			throw new RuntimeException("An error has ocurred when updating the term in the database.");
		
		//2 - If new status = "rejected" then
		if (term.getStatus().equals(Status.rejected)) {
			Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
			taxonomy.removeDescendent(term.getString());
			boolean taxonomyUpdated = dataSource.updateTaxonomy(taxonomyId, taxonomy);
			if(!taxonomyUpdated)
				throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
				//TODO It should revert the term update in this case
		}	
	}
	
	/**
	 * Update multiple parent-child relationships in a given taxonomy.
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param parentChildStatusList - the relations to be changed. Tuples of type <Parent,Child,status>
	 */
	public void updateParentRelationshipStatus(String taxonomyId, List<Pair<String,String>> parentChildStatusList) {
		RuntimeException agException = null;
		
		for(Pair tuple: parentChildStatusList) {
			try {
				updateParentRelationshipStatus(taxonomyId, (String) tuple.getLeft(), (String) tuple.getRight());
    		} catch (Exception e) {
    			if (agException == null)
    				agException = new RuntimeException("Some parent-child relations were not updated: " + e.getMessage());
    			agException.addSuppressed(e);
    		}
		}
		
		if (agException != null)
    		throw agException;
	}

	/**
	 * Update the status of a given parent-child relationship in a given taxonomy
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param termChild - the identifier for the child term
	 * @param status - the status to be modified
	 */
	public void updateParentRelationshipStatus(String taxonomyId, String termChild, String status) {
		/*
		 * 1 - If new status = "rejected" then, throw InvalidOperationException (every node must have a parent) 
		 * 2 - Change relation status in the database otherwise
		 * 3 - If status = "accepted" then, change both term status to "accepted"
		 */
		
		if (taxonomyId == null || taxonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty or null");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}		
		if (termChild == null || termChild.equals("")) {
			InvalidValueException exception = new InvalidValueException("The termChild cannot be empty or null");
			exception.addParameterValue("termChild", termChild);
			throw exception;
		}
		if (status == null) {
			InvalidValueException exception = new InvalidValueException("The status cannot be null");
			exception.addParameterValue("status", status);
			throw exception;
		}
		
		if (status.equals(Status.rejected.toString())) {
			//1 - If new status = "rejected" then, throw InvalidOperationException (every node must have a parent)
			throw new InvalidOperationException("Parent-child relations cannot be rejected. Choose a new parent instead.");
		} else if (!status.equals(Status.accepted.toString()) && !status.equals(Status.none.toString())) {
			InvalidValueException exception = new InvalidValueException("Invalid status value");
			exception.addParameterValue("status", status);
			throw exception; 
		}
		
		// 2 - Change relation status in the database otherwise
		Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
		taxonomy.setParentChildStatus(termChild,Status.valueOf(status));
		boolean taxonomyUpdated = dataSource.updateTaxonomy(taxonomyId, taxonomy);
		if(!taxonomyUpdated)
			throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
		
		// 3 - If relation status = "accepted" then, change both term status to "accepted"
		if (status.equals(Status.accepted.toString())) {
			
			boolean termUpdated = dataSource.updateTerm(taxonomyId, termChild, Status.accepted.toString());
			if(!termUpdated)
				throw new RuntimeException("An error has ocurred when updating the status of the child term in the database.");
			
			String parentString = taxonomy.getParent(termChild).getRoot();			
			if (!parentString.equals(VirtualRootTaxonomy.VIRTUAL_ROOT)) {
				termUpdated = dataSource.updateTerm(taxonomyId, parentString, Status.accepted.toString());
				if(!termUpdated)
					throw new RuntimeException("An error has ocurred when updating the status of the parent term in the database.");
			}
		}		
	}

	/**
	 * Update the parent of multiple terms in the taxonomy
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param childNewParentList - the term-newParent pairs to be modified
	 */
	public void updateParent(String taxonomyId, List<Pair<String, String>> childNewParentList) {
		RuntimeException agException = null;
	
		for(Pair<String,String> childNewParent: childNewParentList) {
			try {
				this.updateParent(taxonomyId, (String) childNewParent.getLeft(), (String) childNewParent.getRight());
			} catch (Exception e) {
    			if (agException == null)
    				agException = new RuntimeException("Some change parent relations were not updated: " + e.getMessage());
    			agException.addSuppressed(e);
    		}
		}
		
		if (agException != null)
    		throw agException;
	}
	
	/**
	 * Update the parent of a given term and mark their relationship as accepted
	 * 
	 * @param taxonomyId - the identifier of the taxonomy to be modified
	 * @param termChild - the child term to have the parent changed
	 * @param termNewParent - the new parent for the child term
	 */
	public void updateParent(String taxonomyId, String termChild, String termNewParent) {
		/*
		 * 1 - Get taxonomy
		 * 2 - If taxonomy exists, ask it to update the parent of the term.
		 * 3 - Save the modifications performed in the taxonomy
		 * 4 - Change status of child - new parent relation to accepted.
		 */
		
		if (taxonomyId == null || taxonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The taxonomy id cannot be empty or null");
			exception.addParameterValue("taxonomyId", "");
			throw exception;
		}		
		if (termChild == null || termChild.equals("")) {
			InvalidValueException exception = new InvalidValueException("The termChild cannot be empty or null");
			exception.addParameterValue("termChild", termChild);
			throw exception;
		}
		if (termNewParent == null || termNewParent.equals("")) {
			InvalidValueException exception = new InvalidValueException("The termNewParent cannot be empty or null");
			exception.addParameterValue("termNewParent", termNewParent);
			throw exception;
		}
		
		
		Taxonomy taxonomy = dataSource.getTaxonomy(taxonomyId);
		if(taxonomy == null)
			throw new RuntimeException("There is no run with id = '" + taxonomyId + "'.");
		taxonomy.updateParent(termChild, termNewParent);
		
		boolean taxonomyUpdated =  dataSource.updateTaxonomy(taxonomyId, taxonomy);
		if(!taxonomyUpdated)
			throw new RuntimeException("An error has ocurred when updating the taxonomy in the database.");
		
		this.updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
	}

	/**
	 * Return a Partonomy for a given partonomy ID.
	 *
	 * @param partonomyId - the identifier of the partonomy
	 */
	public Partonomy getPartonomy(String partonomyId) {

		if (partonomyId == null || partonomyId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The partonomy id cannot be empty");
			exception.addParameterValue("partonomyId", "");
			throw exception;
		}
		try {
			return dataSource.getPartonomy(partonomyId);
		} catch (Exception e) {
			throw new RuntimeException("The partonomy " + partonomyId + " could not be retrieved: " + e.getMessage());
		}
	}

	/**
	 * Return a KnowledgeGraph for a given knowledgeGraph ID.
	 *
	 * @param knowledgeGraphId - the identifier of the knowledge graph
	 */
	public KnowledgeGraph getKnowledgeGraph(String knowledgeGraphId) {

		if (knowledgeGraphId == null || knowledgeGraphId.equals("")) {
			InvalidValueException exception = new InvalidValueException("The knowledgeGraph id cannot be empty");
			exception.addParameterValue("knowledgeGraphId", "");
			throw exception;
		}
		try {
			return dataSource.getKnowledgeGraph(knowledgeGraphId);
		} catch (Exception e) {
			throw new RuntimeException("The knowledgeGraph " + knowledgeGraphId + " could not be retrieved: " + e.getMessage());
		}
	}

    private Executor getExecutor() {
        return Launcher.executor;
    }
    
    /**
     * Return a list of authors and their TF-IRF score for a given term
     * 
     * @param runId - the identifier of the run
     * @param termId - the identifier of the term
     * 
     */
    public List<AuthorTermDAO> getAuthorsPerTermWithTfirf(String runId, String termId) {
    	List<AuthorTermDAO> result = new ArrayList<AuthorTermDAO>();
    	
    	List<AuthorTerm> authorTerms = dataSource.getAuthorTermRelationsPerTerm(runId, termId);
    	for(AuthorTerm authorTerm: authorTerms) {
    		Author author = dataSource.getAuthor(runId, authorTerm.getAuthorId());
    		if (author != null)
    			result.add(new AuthorTermDAO(author, authorTerm.getTfIrf()));
    	}  	
    	
    	return result;
    }

    public List<AuthorTerm> getAuthorTerms(String runId, String authorId) {
        return dataSource.getAuthorTermRelationsPerAuthor(runId, authorId);
    }

    public List<AuthorAuthor> getAuthorSimilarity(String runId, String authorId) {
        return dataSource.getAuthorSimilarity(runId, authorId);
    }
}
