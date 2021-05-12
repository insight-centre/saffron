package org.insightcentre.saffron.web;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.exceptions.InvalidOperationException;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unitary tests for {@link SaffronService}.
 */
public class SaffronServiceTest {

	private MongoDBHandler mongo;
	private SaffronService service;
	
	@Before
    public void setupMock() {
    	mongo = mock(MongoDBHandler.class);
    	service = new SaffronService(mongo);
    }
	
    @Test
    public void testMockCreation(){
        assertNotNull(mongo);
        assertNotNull(service);
    }
	
	/**
	 * Term has its status changed from "none" to "accepted"
	 */
	@Test
	public void testUpdateTermStatus() {
		//Prepare
		Term input = new Term.Builder("term_string").status(Status.accepted).build();
		String runId = "runId";
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.none).build());
		when(mongo.updateTerm(runId, input.getString(), input.getStatus().toString())).thenReturn(true);
		
		//Call
		service.updateTermStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTerm(runId, input.getString(), input.getStatus().toString());
		verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
	}
	
	/**
	 * Term has its status changed from "accepted" to "none"
	 */
	@Test
	public void testUpdateTermStatus2() {
		//Prepare
		Term input = new Term.Builder("term_string").status(Status.none).build();
		String runId = "runId";
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.accepted).build());
		when(mongo.updateTerm(runId, input.getString(), input.getStatus().toString())).thenReturn(true);
		
		//Call
		service.updateTermStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTerm(runId, input.getString(), input.getStatus().toString());
		verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
	}
	
	/**
	 * Term has its status changed from "accepted" to "rejected"
	 */
	@Test
	public void testUpdateTermStatus3() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.accepted).build());
		when(mongo.updateTerm(runId, input.getString(), input.getStatus().toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomy);
		doNothing().when(taxonomy).removeDescendent(input.getString());
		when(mongo.updateTaxonomy(runId, taxonomy)).thenReturn(true);
		
		//Call
		service.updateTermStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTerm(runId, input.getString(), input.getStatus().toString());
		verify(mongo).updateTaxonomy(runId, taxonomy);
	}
	
	/**
	 * Term has its status changed from "none" to "rejected"
	 */
	@Test
	public void testUpdateTermStatus4() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.none).build());
		when(mongo.updateTerm(runId, input.getString(), input.getStatus().toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomy);
		doNothing().when(taxonomy).removeDescendent(input.getString());
		when(mongo.updateTaxonomy(runId, taxonomy)).thenReturn(true);
		
		//Call
		service.updateTermStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTerm(runId, input.getString(), input.getStatus().toString());
		verify(mongo).updateTaxonomy(runId, taxonomy);
	}
	
	/**
	 * Term has its status changed from "rejected" to "none"
	 */
	@Test(expected=InvalidOperationException.class)
	public void testUpdateTermStatus5() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.none).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.rejected).build());
		
		try {
			//Call
			service.updateTermStatus(runId,input);		
		} catch (InvalidOperationException e) {
			//Evaluate
			verify(mongo, never()).updateTerm(runId, input.getString(), input.getStatus().toString());
			verify(mongo, never()).updateTaxonomy(runId, taxonomy);
			
			throw e;
		}
	}
	
	/**
	 * Term has its status changed from "rejected" to "accepted"
	 */
	@Test(expected=InvalidOperationException.class)
	public void testUpdateTermStatus6() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.accepted).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.rejected).build());
		
		try {
			//Call
			service.updateTermStatus(runId,input);		
		} catch (InvalidOperationException e) {
			//Evaluate
			verify(mongo, never()).updateTerm(runId, input.getString(), input.getStatus().toString());
			verify(mongo, never()).updateTaxonomy(runId, taxonomy);
			
			throw e;
		}
	}
	
	/**
	 * Term has its status changed from "rejected" to "rejected"
	 */
	@Test(expected=InvalidOperationException.class)
	public void testUpdateTermStatus7() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.rejected).build());
		
		try {
			//Call
			service.updateTermStatus(runId,input);		
		} catch (InvalidOperationException e) {
			//Evaluate
			verify(mongo, never()).updateTerm(runId, input.getString(), input.getStatus().toString());
			verify(mongo, never()).updateTaxonomy(runId, taxonomy);
			
			throw e;
		}
	}
	
	/**
	 * Term has an invalid string
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateTermStatus8() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("").status(Status.rejected).build();
		
		//Call
		try {
			service.updateTermStatus(runId,input);		
		} catch (InvalidValueException e) {
			//Evaluate
			verify(mongo, never()).updateTerm(runId, input.getString(), input.getStatus().toString());
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
			throw e;
		}
	}
	
	/**
	 * Term has an invalid status
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateTermStatus9() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(null).build();
		
		//Call
		try {
			service.updateTermStatus(runId,input);		
		} catch (InvalidValueException e) {
			//Evaluate
			verify(mongo, never()).updateTerm(runId, input.getString(), null);
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating the term
	 */
	@Test(expected = Exception.class)
	public void testUpdateTermStatus10() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.accepted).build();
		
		when(mongo.updateTerm(runId, input.getString(), input.getStatus().toString())).thenReturn(false);
		
		//Call
		try {
			service.updateTermStatus(runId,input);		
		} catch (Exception e) {
			//Evaluate
			verify(mongo, never()).updateTerm(runId, input.getString(), null);
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating the taxonomy
	 */
	@Test(expected = Exception.class)
	public void testUpdateTermStatus11() {
		//Prepare
		String runId = "runId";
		Term input = new Term.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTerm(runId, input.getString())).thenReturn(new Term.Builder(input.getString()).status(Status.none).build());
		when(mongo.updateTerm(runId, input.getString(), input.getStatus().toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomy);
		doNothing().when(taxonomy).removeDescendent(input.getString());
		when(mongo.updateTaxonomy(runId, taxonomy)).thenReturn(false);
		
		try{
			//Call
			service.updateTermStatus(runId,input);
		} catch (Exception e) {		
			//Evaluate
			verify(mongo).updateTerm(runId, input.getString(), input.getStatus().toString());
			verify(mongo).updateTaxonomy(runId, taxonomy);
			//TODO It should verify if the status change for the term was reverted to the original state since the overall operation failed
			throw e;
		}
	}
	
	/**
	 * Ensure all terms in a term list are updated
	 */
	@Test
	public void testUpdateTermStatusList() {
		//Prepare
		List<Term> input = new ArrayList<Term>();
		Term term1 = new Term.Builder("term1_string").status(Status.accepted).build();;
		Term term2 = new Term.Builder("term2_string").status(Status.rejected).build();
		Term term3 = new Term.Builder("term3_string").status(Status.none).build();
		input.add(term1);
		input.add(term2);
		input.add(term3);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doNothing().when(spyService).updateTermStatus(runId, term1);
		doNothing().when(spyService).updateTermStatus(runId, term2);
		doNothing().when(spyService).updateTermStatus(runId, term3);
		
		
		//Call
		spyService.updateTermStatus(runId,input);		
		
		//Evaluate
		verify(spyService, times(1)).updateTermStatus(runId, term1);
		verify(spyService, times(1)).updateTermStatus(runId, term2);
		verify(spyService, times(1)).updateTermStatus(runId, term3);
	}
	
	/**
	 * Ensure only correct terms in a term list are updated
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateTermStatusList2() {
		//Prepare
		List<Term> input = new ArrayList<Term>();
		Term term1 = new Term.Builder("term1_string").status(null).build();;
		Term term2 = new Term.Builder("term2_string").status(Status.rejected).build();
		Term term3 = new Term.Builder("term3_string").status(Status.none).build();
		input.add(term1);
		input.add(term2);
		input.add(term3);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doThrow(new InvalidValueException("")).when(spyService).updateTermStatus(runId, term1);
		doNothing().when(spyService).updateTermStatus(runId, term2);
		doNothing().when(spyService).updateTermStatus(runId, term3);
		
		try {
			//Call
			spyService.updateTermStatus(runId,input);		
		} catch (Exception e) {
			//Evaluate
			verify(spyService, times(1)).updateTermStatus(runId, term1);
			verify(spyService, times(1)).updateTermStatus(runId, term2);
			verify(spyService, times(1)).updateTermStatus(runId, term3);
			throw e;
		}
	}
	
	/**
	 * Relationship status has changed to "accepted"
	 */
	@Test
	public void testupdateParentRelationshipStatus() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(true);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(true);
		
		//call
		service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		
		//evaluate
		verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
		verify(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		verify(mongo).updateTerm(taxonomyId, termChild, status);
		verify(mongo).updateTerm(taxonomyId, termParentString, status);
	}

	
	/**
	 * Relationship status has changed to "none"
	 */
	@Test
	public void testupdateParentRelationshipStatus2() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.none.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(true);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(true);
		
		//call
		service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		
		//evaluate
		verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
		verify(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
	}
	
	
	/**
	 * Relationship status has changed to "rejected"
	 */
	@Test(expected = InvalidOperationException.class)
	public void testupdateParentRelationshipStatus3() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.rejected.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doThrow(InvalidOperationException.class).when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(true);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(true);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidOperationException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy, never()).setParentChildStatus(termChild, Status.valueOf(status));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParentString, status);
			throw e;
		}
	}
	
	/**
	 * Term child does not exist
	 * (it just ignores and acts as if nothing happened - ??)
	 */
	@Test()
	public void testupdateParentRelationshipStatus4() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(true);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(true);
		
		//call
		service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		
		//evaluate
		verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
		verify(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		verify(mongo).updateTerm(taxonomyId, termChild, status);
		verify(mongo).updateTerm(taxonomyId, termParentString, status);
	}
	
	/**
	 * Taxonomy id does not exist
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus5() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		String termParent = "termParent";
		String status = Status.accepted.toString();
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(null);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(taxonomyId, Mockito.any(Taxonomy.class));
			verify(Mockito.any(Taxonomy.class), never()).setParentChildStatus(termChild, Status.valueOf(status));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}
	}
	
	/**
	 * Empty taxonomyId
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus6() {
		//prepare
		String taxonomyId = "";
		String termChild = "termChild";
		String termParent = "termParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}
	}
	
	/**
	 * Null taxonomyId
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus7() {
		//prepare
		String taxonomyId = null;
		String termChild = "termChild";
		String termParent = "termParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}
	}
	
	/**
	 * Empty termChild
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus8() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "";
		String termParent = "termParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}
	}
	
	/**
	 * Null termChild
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus9() {
		//prepare
		String taxonomyId = "runId";
		String termChild = null;
		String termParent = "termParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}	
	}
	
	/**
	 * Invalid status
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus10() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		String termParent = "termParent";
		String status = "whateverStatus";
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}
	}
	
	/**
	 * Null status
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus11() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		String termParent = "termParent";
		String status = null;
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParent, status);
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating child term
	 * FIXME: If something goes wrong it should revert all operations
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus12() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(false);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(true);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
			verify(mongo).updateTerm(taxonomyId, termChild, status);
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating parent term
	 * FIXME: If something goes wrong it should revert all operations
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus13() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(true);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(false);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
			verify(mongo).updateTerm(taxonomyId, termParentString, status);
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating the taxonomy
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus14() {
		//prepare
		String taxonomyId = "runId";
		String termChild = "termChild";
		Taxonomy termParent = mock(Taxonomy.class);
		String termParentString = "termParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(false);
		
		when(termParent.getRoot()).thenReturn(termParentString);
		when(taxonomy.getParent(termChild)).thenReturn(termParent);
		when(mongo.updateTerm(taxonomyId, termChild, status)).thenReturn(true);
		when(mongo.updateTerm(taxonomyId, termParentString, status)).thenReturn(true);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, termChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy).setParentChildStatus(termChild, Status.valueOf(status));
			verify(mongo, never()).updateTerm(taxonomyId, termChild, status);
			verify(mongo, never()).updateTerm(taxonomyId, termParentString, status);
			throw e;
		}
	}
	
	/**
	 * Ensure all terms in a term list are updated
	 */
	@Test
	public void testupdateParentRelationshipStatusList() {
		//Prepare
		List<Pair<String,String>> input = new ArrayList<Pair<String,String>>();
		Pair<String, String> pair1 = new ImmutablePair<String, String>("child1",Status.accepted.toString());
		Pair<String, String> pair2 = new ImmutablePair<String, String>("child2",Status.none.toString());
		Pair<String, String> pair3 = new ImmutablePair<String, String>("child3",Status.accepted.toString());
		input.add(pair1);
		input.add(pair2);
		input.add(pair3);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doNothing().when(spyService).updateParentRelationshipStatus(runId, pair1.getLeft(), pair1.getRight());
		doNothing().when(spyService).updateParentRelationshipStatus(runId, pair2.getLeft(), pair2.getRight());
		doNothing().when(spyService).updateParentRelationshipStatus(runId, pair3.getLeft(), pair3.getRight());
		
		
		//Call
		spyService.updateParentRelationshipStatus(runId, input);	
		
		//Evaluate
		verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair1.getLeft(), pair1.getRight());
		verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair2.getLeft(), pair2.getRight());
		verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair3.getLeft(), pair3.getRight());
	}
	
	/**
	 * Ensure only correct terms in a term list are updated
	 */
	@Test(expected = RuntimeException.class)
	public void testupdateParentRelationshipStatusList2() {
		//Prepare
		List<Pair<String,String>> input = new ArrayList<Pair<String,String>>();
		Pair<String, String> pair1 = new ImmutablePair<String, String>("child1",Status.rejected.toString());
		Pair<String, String> pair2 = new ImmutablePair<String, String>("child2",Status.none.toString());
		Pair<String, String> pair3 = new ImmutablePair<String, String>("child3",Status.accepted.toString());
		Pair<String, String> pair4 = new ImmutablePair<String, String>("",Status.accepted.toString());
		Pair<String, String> pair5 = new ImmutablePair<String, String>("child5",Status.accepted.toString());
		Pair<String, String> pair6 = new ImmutablePair<String, String>(null,Status.accepted.toString());
		Pair<String, String> pair7 = new ImmutablePair<String, String>("child7",null);
		input.add(pair1);
		input.add(pair2);
		input.add(pair3);
		input.add(pair4);
		input.add(pair5);
		input.add(pair6);
		input.add(pair7);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doThrow(new InvalidOperationException("")).when(spyService).updateParentRelationshipStatus(runId, pair1.getLeft(), pair1.getRight());
		doNothing().when(spyService).updateParentRelationshipStatus(runId, pair2.getLeft(), pair2.getRight());
		doNothing().when(spyService).updateParentRelationshipStatus(runId, pair3.getLeft(), pair3.getRight());
		doThrow(new InvalidValueException("")).when(spyService).updateParentRelationshipStatus(runId, pair4.getLeft(), pair4.getRight());
		doNothing().when(spyService).updateParentRelationshipStatus(runId, pair5.getLeft(), pair5.getRight());
		doThrow(new InvalidValueException("")).when(spyService).updateParentRelationshipStatus(runId, pair6.getLeft(), pair6.getRight());
		doThrow(new InvalidValueException("")).when(spyService).updateParentRelationshipStatus(runId, pair7.getLeft(), pair7.getRight());
		
		try {
			//Call
			spyService.updateParentRelationshipStatus(runId, input);		
		} catch (Exception e) {
			//Evaluate
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair1.getLeft(), pair1.getRight());
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair2.getLeft(), pair2.getRight());
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair3.getLeft(), pair3.getRight());
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair4.getLeft(), pair4.getRight());
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair5.getLeft(), pair5.getRight());
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair6.getLeft(), pair6.getRight());
			verify(spyService, times(1)).updateParentRelationshipStatus(runId, pair7.getLeft(), pair7.getRight());
			throw e;
		}
	}
	
	/**
	 * Parent updated successfully
	 */
	@Test
	public void testUpdateParent() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		//call
		spyService.updateParent(taxonomyId, termChild, termNewParent);
		
		//evaluate
		verify(taxonomy, times(1)).updateParent(termChild, termNewParent);
		verify(mongo, times(1)).updateTaxonomy(taxonomyId, taxonomy);
		verify(spyService, times(1)).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
	}
	
	/**
	 * Parent update unsuccessful (exception thrown)
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateParent2() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doThrow(RuntimeException.class).when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (RuntimeException e) {
			//evaluate
			verify(taxonomy, times(1)).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Error updating taxonomy in the database
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateParent3() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(false);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (RuntimeException e) {		
			//evaluate
			verify(taxonomy, times(1)).updateParent(termChild, termNewParent);
			verify(mongo, times(1)).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Error updating the status of the terms to "accepted"
	 * FIXME: It should retry updating the term status
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateParent4() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doThrow(RuntimeException.class).when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (RuntimeException e){
			//evaluate
			verify(taxonomy, times(1)).updateParent(termChild, termNewParent);
			verify(mongo, times(1)).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, times(1)).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Inexistent taxonomy
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateParent5() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(null);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (RuntimeException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Null taxonomyId
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateParent6() {
		//prepare
		String taxonomyId = null;
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (InvalidValueException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Empty taxonomyId
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateParent7() {
		//prepare
		String taxonomyId = "";
		String termChild = "termChild";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (InvalidValueException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Null termChild 
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateParent8() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = null;
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (InvalidValueException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Empty termChild 
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateParent9() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "";
		String termNewParent = "termNewParent";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (InvalidValueException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Null termNewParent
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateParent10() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = null;
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (InvalidValueException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Empty termNewParent
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateParent11() {
		//prepare
		String taxonomyId = "taxonomyId";
		String termChild = "termChild";
		String termNewParent = "";
		SaffronService spyService = spy(service);
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		doNothing().when(taxonomy).updateParent(termChild, termNewParent);
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		doNothing().when(spyService).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
		
		try {
			//call
			spyService.updateParent(taxonomyId, termChild, termNewParent);
		} catch (InvalidValueException e) {		
			//evaluate
			verify(taxonomy, never()).updateParent(termChild, termNewParent);
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(spyService, never()).updateParentRelationshipStatus(taxonomyId, termChild, Status.accepted.toString());
			throw e;
		}
	}
	
	/**
	 * Ensure all update parent relations are updated
	 */
	@Test
	public void testupdateParentList() {
		//Prepare
		List<Pair<String,String>> input = new ArrayList<Pair<String,String>>();
		Pair<String, String> pair1 = new ImmutablePair<String, String>("child1","newParent1");
		Pair<String, String> pair2 = new ImmutablePair<String, String>("child2","newParent2");
		Pair<String, String> pair3 = new ImmutablePair<String, String>("child3","newParent3");
		input.add(pair1);
		input.add(pair2);
		input.add(pair3);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doNothing().when(spyService).updateParent(runId, pair1.getLeft(), pair1.getRight());
		doNothing().when(spyService).updateParent(runId, pair2.getLeft(), pair2.getRight());
		doNothing().when(spyService).updateParent(runId, pair3.getLeft(), pair3.getRight());
		
		
		//Call
		spyService.updateParent(runId, input);	
		
		//Evaluate
		verify(spyService, times(1)).updateParent(runId, pair1.getLeft(), pair1.getRight());
		verify(spyService, times(1)).updateParent(runId, pair2.getLeft(), pair2.getRight());
		verify(spyService, times(1)).updateParent(runId, pair3.getLeft(), pair3.getRight());
	}
	
	/**
	 * Ensure only correct update parent relations are updated
	 */
	@Test(expected = RuntimeException.class)
	public void testupdateParent2() {
		//Prepare
		List<Pair<String,String>> input = new ArrayList<Pair<String,String>>();
		Pair<String, String> pair1 = new ImmutablePair<String, String>("child1","newParent1");
		Pair<String, String> pair2 = new ImmutablePair<String, String>("child2","newParent2");
		Pair<String, String> pair3 = new ImmutablePair<String, String>("child3","newParent3");
		Pair<String, String> pair4 = new ImmutablePair<String, String>("","newParent4");
		Pair<String, String> pair5 = new ImmutablePair<String, String>("child5","newParent5");
		Pair<String, String> pair6 = new ImmutablePair<String, String>(null,"newParent6");
		Pair<String, String> pair7 = new ImmutablePair<String, String>("child7",null);
		input.add(pair1);
		input.add(pair2);
		input.add(pair3);
		input.add(pair4);
		input.add(pair5);
		input.add(pair6);
		input.add(pair7);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doThrow(new InvalidOperationException("")).when(spyService).updateParent(runId, pair1.getLeft(), pair1.getRight());
		doNothing().when(spyService).updateParent(runId, pair2.getLeft(), pair2.getRight());
		doNothing().when(spyService).updateParent(runId, pair3.getLeft(), pair3.getRight());
		doThrow(new InvalidValueException("")).when(spyService).updateParent(runId, pair4.getLeft(), pair4.getRight());
		doNothing().when(spyService).updateParent(runId, pair5.getLeft(), pair5.getRight());
		doThrow(new InvalidValueException("")).when(spyService).updateParent(runId, pair6.getLeft(), pair6.getRight());
		doThrow(new InvalidValueException("")).when(spyService).updateParent(runId, pair7.getLeft(), pair7.getRight());
		
		try {
			//Call
			spyService.updateParent(runId, input);		
		} catch (Exception e) {
			//Evaluate
			verify(spyService, times(1)).updateParent(runId, pair1.getLeft(), pair1.getRight());
			verify(spyService, times(1)).updateParent(runId, pair2.getLeft(), pair2.getRight());
			verify(spyService, times(1)).updateParent(runId, pair3.getLeft(), pair3.getRight());
			verify(spyService, times(1)).updateParent(runId, pair4.getLeft(), pair4.getRight());
			verify(spyService, times(1)).updateParent(runId, pair5.getLeft(), pair5.getRight());
			verify(spyService, times(1)).updateParent(runId, pair6.getLeft(), pair6.getRight());
			verify(spyService, times(1)).updateParent(runId, pair7.getLeft(), pair7.getRight());
			throw e;
		}
	}
}
