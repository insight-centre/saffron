
package org.insightcentre.nlp.saffron.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author jmccrae
 * @author biaper
 */
public class TaxonomyTest {

	private static final String SAMPLE_TAXONOMY = "{"
			+ "\"root\": \"root node\","
			+ "\"score\": 210.12345,"
			+ "\"linkScore\": 0.98765,"
			+ "\"children\": ["
			+ "{"
				+ "\"root\": \"node 10\","
				+ "\"score\": 210.12345,"
				+ "\"linkScore\": 0.98765,"
				+ "\"children\": []"
			+ "},{"
				+ "\"root\": \"node 11\","
				+ "\"score\": 210.12345,"
				+ "\"linkScore\": 0.98765,"
				+ "\"children\": ["
					+ "{"
						+ "\"root\": \"node 11-1\","
						+ "\"score\": 210.12345,"
						+ "\"linkScore\": 0.98765,"
						+ "\"children\": []"
					+ "}"
				+ "]"
			+ "},{"
				+ "\"root\": \"node 12\","
				+ "\"score\": 210.12345,"
				+ "\"linkScore\": 0.98765,"
				+ "\"children\": ["
					+ "{"
						+ "\"root\": \"node 12-1\","
						+ "\"score\": 210.12345,"
						+ "\"linkScore\": 0.98765,"
						+ "\"children\": []"
					+ "},{"
						+ "\"root\": \"node 12-2\","
						+ "\"score\": 210.12345,"
						+ "\"linkScore\": 0.98765,"
						+ "\"children\": []"
					+ "}"
				+ "]"
			+ "},{"
				+ "\"root\": \"node 13\","
				+ "\"score\": 210.12345,"
				+ "\"linkScore\": 0.98765,"
				+ "\"children\": ["
					+ "{"
						+ "\"root\": \"node 13-1\","
						+ "\"score\": 210.12345,"
						+ "\"linkScore\": 0.98765,"
						+ "\"children\": ["
							+ "{"
								+ "\"root\": \"node 13-1-1\","
								+ "\"score\": 210.12345,"
								+ "\"linkScore\": 0.98765,"
								+ "\"children\": []"
							+ "},{"
								+ "\"root\": \"node 13-1-2\","
								+ "\"score\": 210.12345,"
								+ "\"linkScore\": 0.98765,"
								+ "\"children\": ["
									+ "{"
										+ "\"root\": \"node 13-1-2-1\","
										+ "\"score\": 210.12345,"
										+ "\"linkScore\": 0.98765,"
										+ "\"children\": []"
									+ "}"
								+ "]"
							+ "}"
						+ "]"
					+ "},{"
						+ "\"root\": \"node 13-2\","
						+ "\"score\": 210.12345,"
						+ "\"linkScore\": 0.98765,"
						+ "\"children\": []"
					+ "},{"
						+ "\"root\": \"node 13-3\","
						+ "\"score\": 210.12345,"
						+ "\"linkScore\": 0.98765,"
						+ "\"children\": []"
					+ "}"
				+ "]"
			+ "}"
		+ "]"
	+ "}";
	
    public TaxonomyTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String data = "{\"root\":\"Root topic\",\"children\":[{\"root\": \"topic1\"}]}";
        final Taxonomy taxonomy = mapper.readValue(data, Taxonomy.class);
        assertEquals("Root topic", taxonomy.root);
        assertEquals(1, taxonomy.children.size());
        assertEquals(0, taxonomy.children.get(0).children.size());
        final String json = mapper.writeValueAsString(taxonomy);
        assertEquals(taxonomy, mapper.readValue(json, Taxonomy.class));
    }
    
    @Test
    public void testMinDepth() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals("The minimum depth is incorrect", 1,taxonomy.minDepth());
    }
    
    @Test
    public void testMinDepth2() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	taxonomy.children.get(0).children.add(new Taxonomy("new node", 1.23456, 0.1234, null, "none"));
    	
    	assertEquals("The minimum depth is incorrect", 2,taxonomy.minDepth());
    }
    
    @Test
    public void testMedianDepth() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals(2.0, taxonomy.medianDepth(), 0.000001);
    }
    
    @Test
    public void testMedianDepth2() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	taxonomy.children.remove(2);
    	taxonomy.children.remove(2);
    	
    	assertEquals(1.5, taxonomy.medianDepth(), 0.000001);
    }
    
    @Test
    public void testLeavesDepths() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	Map<String, Integer> expected = new HashMap<String, Integer>();
    	expected.put("node 10", 1);
    	expected.put("node 11-1", 2);
    	expected.put("node 12-1", 2);
    	expected.put("node 12-2", 2);
    	expected.put("node 13-1-1", 3);
    	expected.put("node 13-1-2-1", 4);
    	expected.put("node 13-2", 2);
    	expected.put("node 13-3", 2);
    	
    	Map<String, Integer> actual = taxonomy.leavesDepths(0);
    	
    	assertEquals("The number of leaves returned is incorrect", expected.size(), actual.size());
    	for(Entry<String, Integer> entry: actual.entrySet()) {
    		if(!expected.containsKey(entry.getKey())) {
    			fail("The key " + entry.getKey() + " is not expected in the result");
    		} else {
    			assertEquals("The depth of the node '" + entry.getKey() + "' is incorrect",expected.get(entry.getKey()),entry.getValue());
    		}
    	}    	
    }
    
    @Test
    public void testMaxDegree() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals("The maximum node degree is different from expected", 4,taxonomy.maxDegree());
    }
    
    @Test
    public void testMaxDegree2() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	taxonomy.children.get(3).children.add(new Taxonomy("new node", 1.23456, 0.1234, null, "none"));
    	
    	assertEquals("The maximum node degree is different from expected", 5,taxonomy.maxDegree());
    }
    
    @Test
    public void testAvgDegree() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals("The average node degree is different from expected", 26.0/14, taxonomy.avgDegree(), 0.00001);
    }
    
    @Test
    public void testAvgDegree2() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	taxonomy.children.get(3).children.add(new Taxonomy("new node", 1.23456, 0.1234, null, "none"));
    	
    	assertEquals("The average node degree is different from expected", 28.0/15, taxonomy.avgDegree(), 0.00001);
    }
    
    @Test
    public void testMedianDegree() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals("The median node degree is different from expected", 1.0, taxonomy.medianDegree(), 0.00001);
    }

    @Test
    public void testNodeDegrees() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	Map<String, Integer> expected = new HashMap<String, Integer>();
    	expected.put("root node", 4);
    	expected.put("node 10", 1);
    	expected.put("node 11", 2);
    	expected.put("node 11-1", 1);
    	expected.put("node 12", 3);
    	expected.put("node 12-1", 1);
    	expected.put("node 12-2", 1);
    	expected.put("node 13", 4);
    	expected.put("node 13-1", 3);
    	expected.put("node 13-1-1", 1);
    	expected.put("node 13-1-2", 2);
    	expected.put("node 13-1-2-1", 1);
    	expected.put("node 13-2", 1);
    	expected.put("node 13-3", 1);
    	
    	Map<String, Integer> actual = taxonomy.nodeDegrees(true);
    	
    	assertEquals("The number of nodes returned is incorrect", expected.size(), actual.size());
    	for(Entry<String, Integer> entry: actual.entrySet()) {
    		if(!expected.containsKey(entry.getKey())) {
    			fail("The key " + entry.getKey() + " is not expected in the result");
    		} else {
    			assertEquals("The degree of the node '" + entry.getKey() + "' is incorrect",expected.get(entry.getKey()),entry.getValue());
    		}
    	}    	
    }
    
    @Test
    public void testNumberOfLeafNodes() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals("The number of leaves is different from expected", 8,taxonomy.numberOfLeafNodes());
    }
    
    @Test
    public void testNumberOfLeafNodes2() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue("{"
    			+ "\"root\": \"root node\","
    			+ "\"score\": 210.12345,"
    			+ "\"linkScore\": 0.98765,"
    			+ "\"children\": []}", Taxonomy.class);
    	
    	assertEquals("The number of leaves is different from expected", 0,taxonomy.numberOfLeafNodes());
    }
    
    @Test
    public void testNumberOfBranchNodes() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue(SAMPLE_TAXONOMY, Taxonomy.class);
    	
    	assertEquals("The number of branch nodes is different from expected", 5,taxonomy.numberOfBranchNodes());
    }
    
    @Test
    public void testNumberOfBranchNodes2() throws JsonParseException, JsonMappingException, IOException {
    	ObjectMapper mapper = new ObjectMapper();
    	final Taxonomy taxonomy = mapper.readValue("{"
    			+ "\"root\": \"root node\","
    			+ "\"score\": 210.12345,"
    			+ "\"linkScore\": 0.98765,"
    			+ "\"children\": []}", Taxonomy.class);
    	
    	assertEquals("The number of branch nodes is different from expected", 0,taxonomy.numberOfBranchNodes());
    }
}