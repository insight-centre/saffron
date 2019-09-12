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
import org.insightcentre.nlp.saffron.data.Topic;
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
	 * Topic has its status changed from "none" to "accepted"
	 */
	@Test
	public void testUpdateTopicStatus() {
		//Prepare
		Topic input = new Topic.Builder("topic_string").status(Status.accepted).build();
		String runId = "runId";
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.none).build());
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
	}
	
	/**
	 * Topic has its status changed from "accepted" to "none"
	 */
	@Test
	public void testUpdateTopicStatus2() {
		//Prepare
		Topic input = new Topic.Builder("topic_string").status(Status.none).build();
		String runId = "runId";
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.accepted).build());
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
	}
	
	/**
	 * Topic has its status changed from "accepted" to "rejected"
	 */
	@Test
	public void testUpdateTopicStatus3() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.accepted).build());
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomy);
		doNothing().when(taxonomy).removeDescendent(input.topicString);
		when(mongo.updateTaxonomy(runId, taxonomy)).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo).updateTaxonomy(runId, taxonomy);
	}
	
	/**
	 * Topic has its status changed from "none" to "rejected"
	 */
	@Test
	public void testUpdateTopicStatus4() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.none).build());
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomy);
		doNothing().when(taxonomy).removeDescendent(input.topicString);
		when(mongo.updateTaxonomy(runId, taxonomy)).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo).updateTaxonomy(runId, taxonomy);
	}
	
	/**
	 * Topic has its status changed from "rejected" to "none"
	 */
	@Test(expected=InvalidOperationException.class)
	public void testUpdateTopicStatus5() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.none).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.rejected).build());
		
		try {
			//Call
			service.updateTopicStatus(runId,input);		
		} catch (InvalidOperationException e) {
			//Evaluate
			verify(mongo, never()).updateTopic(runId, input.topicString, input.status.toString());
			verify(mongo, never()).updateTaxonomy(runId, taxonomy);
			
			throw e;
		}
	}
	
	/**
	 * Topic has its status changed from "rejected" to "accepted"
	 */
	@Test(expected=InvalidOperationException.class)
	public void testUpdateTopicStatus6() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.accepted).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.rejected).build());
		
		try {
			//Call
			service.updateTopicStatus(runId,input);		
		} catch (InvalidOperationException e) {
			//Evaluate
			verify(mongo, never()).updateTopic(runId, input.topicString, input.status.toString());
			verify(mongo, never()).updateTaxonomy(runId, taxonomy);
			
			throw e;
		}
	}
	
	/**
	 * Topic has its status changed from "rejected" to "rejected"
	 */
	@Test(expected=InvalidOperationException.class)
	public void testUpdateTopicStatus7() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.rejected).build());
		
		try {
			//Call
			service.updateTopicStatus(runId,input);		
		} catch (InvalidOperationException e) {
			//Evaluate
			verify(mongo, never()).updateTopic(runId, input.topicString, input.status.toString());
			verify(mongo, never()).updateTaxonomy(runId, taxonomy);
			
			throw e;
		}
	}
	
	/**
	 * Topic has an invalid string
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateTopicStatus8() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("").status(Status.rejected).build();
		
		//Call
		try {
			service.updateTopicStatus(runId,input);		
		} catch (InvalidValueException e) {
			//Evaluate
			verify(mongo, never()).updateTopic(runId, input.topicString, input.status.toString());
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
			throw e;
		}
	}
	
	/**
	 * Topic has an invalid status
	 */
	@Test(expected = InvalidValueException.class)
	public void testUpdateTopicStatus9() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(null).build();
		
		//Call
		try {
			service.updateTopicStatus(runId,input);		
		} catch (InvalidValueException e) {
			//Evaluate
			verify(mongo, never()).updateTopic(runId, input.topicString, null);
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating the topic
	 */
	@Test(expected = Exception.class)
	public void testUpdateTopicStatus10() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.accepted).build();
		
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(false);
		
		//Call
		try {
			service.updateTopicStatus(runId,input);		
		} catch (Exception e) {
			//Evaluate
			verify(mongo, never()).updateTopic(runId, input.topicString, null);
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating the taxonomy
	 */
	@Test(expected = Exception.class)
	public void testUpdateTopicStatus11() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTopic(runId, input.topicString)).thenReturn(new Topic.Builder(input.topicString).status(Status.none).build());
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomy);
		doNothing().when(taxonomy).removeDescendent(input.topicString);
		when(mongo.updateTaxonomy(runId, taxonomy)).thenReturn(false);
		
		try{
			//Call
			service.updateTopicStatus(runId,input);
		} catch (Exception e) {		
			//Evaluate
			verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
			verify(mongo).updateTaxonomy(runId, taxonomy);
			//TODO It should verify if the status change for the topic was reverted to the original state since the overall operation failed
			throw e;
		}
	}
	
	/**
	 * Ensure all topics in a topic list are updated
	 */
	@Test
	public void testUpdateTopicStatusList() {
		//Prepare
		List<Topic> input = new ArrayList<Topic>();
		Topic topic1 = new Topic.Builder("topic1_string").status(Status.accepted).build();;
		Topic topic2 = new Topic.Builder("topic2_string").status(Status.rejected).build();
		Topic topic3 = new Topic.Builder("topic3_string").status(Status.none).build();
		input.add(topic1);
		input.add(topic2);
		input.add(topic3);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doNothing().when(spyService).updateTopicStatus(runId, topic1);
		doNothing().when(spyService).updateTopicStatus(runId, topic2);
		doNothing().when(spyService).updateTopicStatus(runId, topic3);
		
		
		//Call
		spyService.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(spyService, times(1)).updateTopicStatus(runId, topic1);
		verify(spyService, times(1)).updateTopicStatus(runId, topic2);
		verify(spyService, times(1)).updateTopicStatus(runId, topic3);
	}
	
	/**
	 * Ensure only correct topics in a topic list are updated
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateTopicStatusList2() {
		//Prepare
		List<Topic> input = new ArrayList<Topic>();
		Topic topic1 = new Topic.Builder("topic1_string").status(null).build();;
		Topic topic2 = new Topic.Builder("topic2_string").status(Status.rejected).build();
		Topic topic3 = new Topic.Builder("topic3_string").status(Status.none).build();
		input.add(topic1);
		input.add(topic2);
		input.add(topic3);
		
		String runId = "runId";
		SaffronService spyService = spy(service);
		
		doThrow(new InvalidValueException("")).when(spyService).updateTopicStatus(runId, topic1);
		doNothing().when(spyService).updateTopicStatus(runId, topic2);
		doNothing().when(spyService).updateTopicStatus(runId, topic3);
		
		try {
			//Call
			spyService.updateTopicStatus(runId,input);		
		} catch (Exception e) {
			//Evaluate
			verify(spyService, times(1)).updateTopicStatus(runId, topic1);
			verify(spyService, times(1)).updateTopicStatus(runId, topic2);
			verify(spyService, times(1)).updateTopicStatus(runId, topic3);
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
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(true);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(true);
		
		//call
		service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		
		//evaluate
		verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
		verify(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		verify(mongo).updateTopic(taxonomyId, topicChild, status);
		verify(mongo).updateTopic(taxonomyId, topicParentString, status);
	}

	
	/**
	 * Relationship status has changed to "none"
	 */
	@Test
	public void testupdateParentRelationshipStatus2() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.none.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(true);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(true);
		
		//call
		service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		
		//evaluate
		verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
		verify(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
	}
	
	
	/**
	 * Relationship status has changed to "rejected"
	 */
	@Test(expected = InvalidOperationException.class)
	public void testupdateParentRelationshipStatus3() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.rejected.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doThrow(new InvalidOperationException("")).when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(true);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(true);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidOperationException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy, never()).setParentChildStatus(topicChild, Status.valueOf(status));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParentString, status);
			throw e;
		}
	}
	
	/**
	 * Topic child does not exist
	 * (it just ignores and acts as if nothing happened - ??)
	 */
	@Test()
	public void testupdateParentRelationshipStatus4() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(true);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(true);
		
		//call
		service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		
		//evaluate
		verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
		verify(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		verify(mongo).updateTopic(taxonomyId, topicChild, status);
		verify(mongo).updateTopic(taxonomyId, topicParentString, status);
	}
	
	/**
	 * Taxonomy id does not exist
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus5() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "topicChild";
		String topicParent = "topicParent";
		String status = Status.accepted.toString();
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(null);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(taxonomyId, Mockito.any(Taxonomy.class));
			verify(Mockito.any(Taxonomy.class), never()).setParentChildStatus(topicChild, Status.valueOf(status));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
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
		String topicChild = "topicChild";
		String topicParent = "topicParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
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
		String topicChild = "topicChild";
		String topicParent = "topicParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
			throw e;
		}
	}
	
	/**
	 * Empty topicChild
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus8() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "";
		String topicParent = "topicParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
			throw e;
		}
	}
	
	/**
	 * Null topicChild
	 */
	@Test(expected=InvalidValueException.class)
	public void testupdateParentRelationshipStatus9() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = null;
		String topicParent = "topicParent";
		String status = Status.accepted.toString();
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
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
		String topicChild = "topicChild";
		String topicParent = "topicParent";
		String status = "whateverStatus";
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
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
		String topicChild = "topicChild";
		String topicParent = "topicParent";
		String status = null;
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (InvalidValueException e) {
			//evaluate
			verify(mongo, never()).updateTaxonomy(Mockito.anyString(), Mockito.any(Taxonomy.class));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParent, status);
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating child topic
	 * FIXME: If something goes wrong it should revert all operations
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus12() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(false);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(true);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
			verify(mongo).updateTopic(taxonomyId, topicChild, status);
			throw e;
		}
	}
	
	/**
	 * Something went wrong in Database when updating parent topic
	 * FIXME: If something goes wrong it should revert all operations
	 */
	@Test(expected=Exception.class)
	public void testupdateParentRelationshipStatus13() {
		//prepare
		String taxonomyId = "runId";
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		Taxonomy childTaxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(true);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(true);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(false);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
			verify(mongo).updateTopic(taxonomyId, topicParentString, status);
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
		String topicChild = "topicChild";
		Taxonomy topicParent = mock(Taxonomy.class);
		String topicParentString = "topicParent";
		String status = Status.accepted.toString();
		
		Taxonomy taxonomy = mock(Taxonomy.class);
		
		when(mongo.getTaxonomy(taxonomyId)).thenReturn(taxonomy);
		
		doNothing().when(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
		when(mongo.updateTaxonomy(taxonomyId, taxonomy)).thenReturn(false);
		
		when(topicParent.getRoot()).thenReturn(topicParentString);
		when(taxonomy.getParent(topicChild)).thenReturn(topicParent);
		when(mongo.updateTopic(taxonomyId, topicChild, status)).thenReturn(true);
		when(mongo.updateTopic(taxonomyId, topicParentString, status)).thenReturn(true);
		
		try {
			//call
			service.updateParentRelationshipStatus(taxonomyId, topicChild, status);
		} catch (Exception e) {
			//evaluate
			verify(mongo).updateTaxonomy(taxonomyId, taxonomy);
			verify(taxonomy).setParentChildStatus(topicChild, Status.valueOf(status));
			verify(mongo, never()).updateTopic(taxonomyId, topicChild, status);
			verify(mongo, never()).updateTopic(taxonomyId, topicParentString, status);
			throw e;
		}
	}
	
	/**
	 * Ensure all topics in a topic list are updated
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
	 * Ensure only correct topics in a topic list are updated
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
}
