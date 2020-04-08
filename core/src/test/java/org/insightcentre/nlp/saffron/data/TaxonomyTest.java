
package org.insightcentre.nlp.saffron.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.insightcentre.nlp.saffron.exceptions.InvalidOperationException;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;
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

	private Taxonomy inputTaxonomy;

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
    	inputTaxonomy = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String data = "{\"root\":\"Root term\",\"children\":[{\"root\": \"term1\"}]}";
        final Taxonomy taxonomy = mapper.readValue(data, Taxonomy.class);
        assertEquals("Root term", taxonomy.root);
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
    	taxonomy.children.get(0).children.add(new Taxonomy("new node", 1.23456, 0.1234, null, Status.none));

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
    	taxonomy.children.get(3).children.add(new Taxonomy("new node", 1.23456, 0.1234,null, Status.none));

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
    	taxonomy.children.get(3).children.add(new Taxonomy("new node", 1.23456, 0.1234, null, Status.none));

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

    /**
     *
     * Testing method
     *
     *   removeDescendent(String termString)
     *
     */

    /**
     * The removeDescendent command should never remove the root node
     */
    @Test
    public void testRemoveRoot() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.removeDescendent("greatgrandmother");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testRemoveRootChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(new Taxonomy.Builder().root("kid1").build())
				.addChild(
						new Taxonomy.Builder().root("grandmother")
							.addChild(new Taxonomy.Builder().root("aunt").build())
							.addChild(new Taxonomy.Builder().root("uncle").build())
						.build()
				)
				.addChild(new Taxonomy.Builder().root("kid2").build())
			.build();

    	//call
    	inputTaxonomy.removeDescendent("mother");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testRemoveRootChildLeafNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	inputTaxonomy.children.add(new Taxonomy.Builder().root("childless").build());

    	//call
    	inputTaxonomy.removeDescendent("childless");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testRemoveRootGrandchildBranchNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.addChild(new Taxonomy.Builder().root("aunt").build())
					.addChild(new Taxonomy.Builder().root("uncle").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.removeDescendent("grandmother");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testRemoveRootGrandchildLeafNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.removeDescendent("kid2");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testRemoveLeafNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.removeDescendent("uncle");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testRemoveInexistentNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.removeDescendent("random");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }


    /**
     *
     * Testing method
     *
     *   setParentChildStatus(String childTerm, Status status)
     *
     */

    @Test
    public void testSetParentChildStatusBranchNodeAccepted() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother").status(Status.accepted)
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.setParentChildStatus("grandmother", Status.accepted);

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testSetParentChildStatusBranchNodeNone() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother").status(Status.none)
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.setParentChildStatus("grandmother", Status.none);

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test(expected = InvalidOperationException.class)
    public void testSetParentChildStatusBranchNodeRejected() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
    		//call
    		inputTaxonomy.setParentChildStatus("grandmother", Status.rejected);
    	} catch (InvalidOperationException e) {
    		//evaluate
    		assertEquals(expected,inputTaxonomy);
    		throw e;
    	}
    }

    @Test
    public void testSetParentChildStatusLeafNodeAccepted() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").status(Status.accepted).build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.setParentChildStatus("kid2", Status.accepted);

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testSetParentChildStatusLeafNodeNone() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").status(Status.none).build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.setParentChildStatus("kid2", Status.none);

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test(expected = InvalidOperationException.class)
    public void testSetParentChildStatusLeafNodeRejected() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
    		//call
    		inputTaxonomy.setParentChildStatus("kid2", Status.rejected);
    	} catch (InvalidOperationException e) {
    		//evaluate
    		assertEquals(expected,inputTaxonomy);
    		throw e;
    	}
    }

    @Test
    public void testSetParentChildStatusNonexistentChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.setParentChildStatus("nonexistent", Status.accepted);

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test(expected = InvalidValueException.class)
    public void testSetParentChildStatusNull() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
    		//call
    		inputTaxonomy.setParentChildStatus("kid2", null);
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected,inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testSetParentChildStatusChildEmpty() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
			//call
			inputTaxonomy.setParentChildStatus("", Status.accepted);
    	} catch (InvalidValueException e){
			//evaluate
			assertEquals(expected,inputTaxonomy);
			throw e;
    	}

    }

    @Test(expected = InvalidValueException.class)
    public void testSetParentChildStatusChildNull() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
    		//call
    		inputTaxonomy.setParentChildStatus(null, Status.accepted);
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected,inputTaxonomy);
    		throw e;
    	}
    }

    /*
     * It is currently impossible to perform such check from within the Taxonomy object.
     * The reason is that there is no way to identify the primary root from within the object.
     *
    @Test(expected = InvalidOperationException.class)
    public void testSetParentChildStatusRootAsChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
    		//call
    		inputTaxonomy.setParentChildStatus("greatgrandmother", Status.accepted);
    	} catch (InvalidOperationException e) {
    		//evaluate
    		assertEquals(expected,inputTaxonomy);
    		throw e;
    	}
    }
    */

    /*
     * It is currently impossible to perform such check from within the Taxonomy object.
     * The reason is that there is no way to identify the primary root from within the object.
     */
    /*@Test
    public void getParentRoot() {
    	//prepare

    	//call

    	//evaluate
    }*/


    /**
     *
     * Testing method
     *
     *   getParent(String termChild)
     *
     */

    @Test
    public void getParentRootChildBranchNode() {
    	//prepare
    	String expected = "greatgrandmother";

    	//call
    	String actual = inputTaxonomy.getParent("mother").getRoot();

    	//evaluate
    	assertEquals(expected, actual);
    }

    @Test
    public void getParentRootChildLeafNode() {
    	//prepare
    	inputTaxonomy = new Taxonomy.Builder(inputTaxonomy).addChild(
    				new Taxonomy.Builder().root("grandfather").build()
    			).build();
    	String expected = "greatgrandmother";

    	//call
    	String actual = inputTaxonomy.getParent("grandfather").getRoot();

    	//evaluate
    	assertEquals(expected, actual);
    }

    @Test
    public void getParentNonRootChildBranchNode() {
    	//prepare
    	String expected = "mother";

    	//call
    	String actual = inputTaxonomy.getParent("grandmother").getRoot();

    	//evaluate
    	assertEquals(expected, actual);
    }

    @Test
    public void getParentNonRootChildLeafNode() {
    	//prepare
    	String expected = "mother";

    	//call
    	String actual = inputTaxonomy.getParent("kid2").getRoot();

    	//evaluate
    	assertEquals(expected, actual);
    }

    @Test
    public void getParentInexistentChild() {
    	//prepare

    	//call
    	Taxonomy actual = inputTaxonomy.getParent("inexistent");

    	//evaluate
    	assertNull(actual);
    }

    @Test(expected=InvalidValueException.class)
    public void getParentNullParameter() {
    	//prepare

    	//call
    	inputTaxonomy.getParent(null);

    	//evaluate
    }

    @Test(expected=InvalidValueException.class)
    public void getParentEmptyParameter() {
    	//prepare

    	//call
    	inputTaxonomy.getParent("");

    	//evaluate
    }

    /**
     *
     * Testing method
     *
     *   removeChildBranch(String termString)
     *
     */

    @Test
    public void testRemoveChildBranchBranchNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.children.get(0).removeChildBranch("grandmother");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testRemoveChildBranchLeafNode() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.children.get(0).removeChildBranch("kid1");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testRemoveChildBranchInexistentChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.children.get(0).removeChildBranch("inexistent_child");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test(expected = InvalidValueException.class)
    public void testRemoveChildBranchNullChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
    		inputTaxonomy.children.get(0).removeChildBranch(null);
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testRemoveChildBranchEmptyChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
    		inputTaxonomy.children.get(0).removeChildBranch("");
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    /**
     *
     * Testing method
     *
     *   addChild(Taxonomy child)
     *
     */

    @Test
    public void testAddChildBranchNode() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("grandfather")
    			.addChild(new Taxonomy.Builder().root("aunt2").build())
				.addChild(new Taxonomy.Builder().root("uncle2").build())
			.build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
				.addChild(toAdd)
			.build();

    	//call
    	inputTaxonomy.addChild(toAdd);

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testAddChildLeafNode() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("grandfather").build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
				.addChild(toAdd)
			.build();

    	//call
    	inputTaxonomy.addChild(toAdd);

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test(expected = InvalidOperationException.class)
    public void testAddChildExistentBranchHierarchy() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("grandmother")
				.addChild(new Taxonomy.Builder().root("aunt").build())
				.addChild(new Taxonomy.Builder().root("uncle").build())
			.build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.children.get(0).addChild(toAdd);
    	} catch (InvalidOperationException e) {
	    	//evaluate
	    	assertEquals(expected,inputTaxonomy);
	    	throw e;
    	}
    }

    @Test(expected = InvalidOperationException.class)
    public void testAddChildExistentLeafNode() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("kid1").build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try{
	    	//call
	    	inputTaxonomy.children.get(0).addChild(toAdd);
	    } catch (InvalidOperationException e) {
	    	//evaluate
	    	assertEquals(expected,inputTaxonomy);
	    	throw e;
		}
    }

    @Test(expected = InvalidOperationException.class)
    public void testAddChildSameChildName() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("mother").build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.addChild(toAdd);
    	} catch (InvalidOperationException e) {
	    	//evaluate
	    	assertEquals(expected,inputTaxonomy);
	    	throw e;
    	}
    }

    @Test(expected = InvalidOperationException.class)
    public void testAddChildSameDescendentName() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("kid1").build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.addChild(toAdd);
    	} catch (InvalidOperationException e) {
	    	//evaluate
	    	assertEquals(expected,inputTaxonomy);
	    	throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testAddChildNull() {
    	//prepare
    	Taxonomy toAdd = null;

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.addChild(toAdd);
    	} catch (InvalidValueException e) {
	    	//evaluate
	    	assertEquals(expected,inputTaxonomy);
	    	throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testAddChildEmpty() {
    	//prepare
    	Taxonomy toAdd = new Taxonomy.Builder().root("").build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.addChild(toAdd);
    	} catch (InvalidValueException e) {
	    	//evaluate
	    	assertEquals(expected,inputTaxonomy);
	    	throw e;
    	}
    }

    /**
     *
     * Testing method
     *
     *   updateParent(String termChild, String termNewParent)
     *
     */

    @Test
    public void testUpdateParentUncleBranchChild() {
    	//prepare
    	inputTaxonomy.children.add(new Taxonomy.Builder().root("father").build());

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
				.addChild(new Taxonomy.Builder().root("father")
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("grandmother", "father");

    	//evaluate
    	assertEquals(expected,inputTaxonomy);
    }

    @Test
    public void testUpdateParentUncleLeafChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
							.build()
					)
					.addChild(
							new Taxonomy.Builder().root("kid2")
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("uncle", "kid2");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testUpdateParentSiblingBranchChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(
							new Taxonomy.Builder().root("kid1")
							.addChild(
									new Taxonomy.Builder().root("grandmother")
										.addChild(new Taxonomy.Builder().root("aunt").build())
										.addChild(new Taxonomy.Builder().root("uncle").build())
									.build()
							)
							.build())
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("grandmother", "kid1");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testUpdateParentSiblingLeafChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(
									new Taxonomy.Builder().root("aunt")
									.addChild(new Taxonomy.Builder().root("uncle").build())
									.build())

							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("uncle", "aunt");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testUpdateParentGrandparentBranchChild() {
    	//prepare

    	inputTaxonomy = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(
										new Taxonomy.Builder().root("uncle")
										.addChild(new Taxonomy.Builder().root("father").build())
										.build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.addChild(
							new Taxonomy.Builder().root("uncle")
							.addChild(new Taxonomy.Builder().root("father").build())
							.build()
					)
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("uncle", "mother");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testUpdateParentGrandparentLeafChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.addChild(new Taxonomy.Builder().root("uncle").build())
					.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("uncle", "mother");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testUpdateParentRootBranchChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
				.addChild(
						new Taxonomy.Builder().root("grandmother")
							.addChild(new Taxonomy.Builder().root("aunt").build())
							.addChild(new Taxonomy.Builder().root("uncle").build())
						.build()
				)
			.build();

    	//call
    	inputTaxonomy.updateParent("grandmother", "greatgrandmother");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test
    public void testUpdateParentRootLeafChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
				.addChild(new Taxonomy.Builder().root("kid1").build())
			.build();

    	//call
    	inputTaxonomy.updateParent("kid1", "greatgrandmother");

    	//evaluate
    	assertEquals(expected, inputTaxonomy);
    }

    @Test(expected = InvalidOperationException.class)
    public void testUpdateParentIsAChildBranchChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("grandmother", "aunt");
    	} catch (InvalidOperationException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = InvalidOperationException.class)
    public void testUpdateParentIsADescendantBranchChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("greatgrandmother", "aunt");
    	} catch (InvalidOperationException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateParentInexistentParent() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("grandmother", "inexistent");
    	} catch (RuntimeException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = RuntimeException.class)
    public void testUpdateParentInexistentChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("inexistent", "aunt");
    	} catch (RuntimeException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected=InvalidValueException.class)
    public void testUpdateParentNullChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent(null, "aunt");
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testUpdateParentEmptyChild() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("", "aunt");
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testUpdateParentNullParent() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("mother", null);
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }

    @Test(expected = InvalidValueException.class)
    public void testUpdateParentEmptyParent() {
    	//prepare
    	Taxonomy expected = new Taxonomy.Builder()
				.root("greatgrandmother")
				.addChild(
					new Taxonomy.Builder().root("mother")
					.addChild(new Taxonomy.Builder().root("kid1").build())
					.addChild(
							new Taxonomy.Builder().root("grandmother")
								.addChild(new Taxonomy.Builder().root("aunt").build())
								.addChild(new Taxonomy.Builder().root("uncle").build())
							.build()
					)
					.addChild(new Taxonomy.Builder().root("kid2").build())
					.build()
				)
			.build();

    	try {
	    	//call
	    	inputTaxonomy.updateParent("mother", "");
    	} catch (InvalidValueException e) {
    		//evaluate
    		assertEquals(expected, inputTaxonomy);
    		throw e;
    	}
    }


}
