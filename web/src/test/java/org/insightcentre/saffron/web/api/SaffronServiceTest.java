package org.insightcentre.saffron.web.api;

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

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.saffron.web.SaffronService;
import org.insightcentre.saffron.web.exceptions.InvalidOperationException;
import org.insightcentre.saffron.web.exceptions.InvalidValueException;
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
		doNothing().when(taxonomy).removeChild(input.topicString);
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
		doNothing().when(taxonomy).removeChild(input.topicString);
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
		doNothing().when(taxonomy).removeChild(input.topicString);
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

}
