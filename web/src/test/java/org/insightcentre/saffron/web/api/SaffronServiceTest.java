package org.insightcentre.saffron.web.api;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.saffron.web.SaffronService;
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
	 * Topic has its status changed to "accepted"
	 */
	@Test
	public void testUpdateTopicStatus() {
		//Prepare
		Topic input = new Topic.Builder("topic_string").status(Status.accepted).build();
		String runId = "runId";
		
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
	}
	
	/**
	 * Topic has its status changed to "none"
	 */
	@Test
	public void testUpdateTopicStatus2() {
		//Prepare
		Topic input = new Topic.Builder("topic_string").status(Status.none).build();
		String runId = "runId";
		
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo, never()).updateTaxonomy(Mockito.anyString(),Mockito.any(Taxonomy.class));
	}
	
	/**
	 * Topic has its status changed to "rejected"
	 */
	@Test
	public void testUpdateTopicStatus3() {
		//Prepare
		String runId = "runId";
		Topic input = new Topic.Builder("mother").status(Status.rejected).build();
		Taxonomy taxonomyBefore = mock(Taxonomy.class);
		Taxonomy taxonomyAfter = mock(Taxonomy.class);
		
		when(mongo.updateTopic(runId, input.topicString, input.status.toString())).thenReturn(true);
		when(mongo.getTaxonomy(runId)).thenReturn(taxonomyBefore);
		when(taxonomyBefore.removeChild(input.topicString, taxonomyBefore)).thenReturn(taxonomyAfter);
		when(mongo.updateTaxonomy(runId, taxonomyAfter)).thenReturn(true);
		
		//Call
		service.updateTopicStatus(runId,input);		
		
		//Evaluate
		verify(mongo).updateTopic(runId, input.topicString, input.status.toString());
		verify(mongo).updateTaxonomy(runId, taxonomyAfter);
	}

}
