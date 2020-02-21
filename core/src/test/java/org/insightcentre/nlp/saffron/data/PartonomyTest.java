
package org.insightcentre.nlp.saffron.data;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Andy Donald
 */
public class PartonomyTest {

	private static final String SAMPLE_PARTONOMY = "{\n" +
			"  \"components\": [\n" +
			"    {\n" +
			"      \"root\": \"five\",\n" +
			"          \"score\": 0.19068895369920114,\n" +
			"          \"linkScore\": \"NaN\",\n" +
			"          \"originalParent\": \"\",\n" +
			"          \"originalTopic\": \"\",\n" +
			"          \"children\": [\n" +
			"            {\n" +
			"              \"root\": \"american leadership\",\n" +
			"              \"score\": 0.06622569313155605,\n" +
			"              \"linkScore\": 0.9201073493745335,\n" +
			"              \"originalParent\": \"\",\n" +
			"              \"originalTopic\": \"\",\n" +
			"              \"children\": [],\n" +
			"              \"status\": \"none\",\n" +
			"              \"parent\": null\n" +
			"            },\n" +
			"            {\n" +
			"              \"root\": \"idea\",\n" +
			"              \"score\": 0.08895375457875457,\n" +
			"              \"linkScore\": 0.9185832497189467,\n" +
			"              \"originalParent\": \"\",\n" +
			"              \"originalTopic\": \"\",\n" +
			"              \"children\": [],\n" +
			"              \"status\": \"none\",\n" +
			"              \"parent\": null\n" +
			"            }\n" +
			"          ],\n" +
			"          \"status\": null,\n" +
			"          \"parent\": null\n" +
			"    }, {\n" +
			"      \"root\": \"six\",\n" +
			"          \"score\": 0.19068895369920114,\n" +
			"          \"linkScore\": \"NaN\",\n" +
			"          \"originalParent\": \"\",\n" +
			"          \"originalTopic\": \"\",\n" +
			"          \"children\": [\n" +
			"            {\n" +
			"              \"root\": \"leadership\",\n" +
			"              \"score\": 0.06622569313155605,\n" +
			"              \"linkScore\": 0.9201073493745335,\n" +
			"              \"originalParent\": \"\",\n" +
			"              \"originalTopic\": \"\",\n" +
			"              \"children\": [],\n" +
			"              \"status\": \"none\",\n" +
			"              \"parent\": null\n" +
			"            },\n" +
			"            {\n" +
			"              \"root\": \"thing\",\n" +
			"              \"score\": 0.08895375457875457,\n" +
			"              \"linkScore\": 0.9185832497189467,\n" +
			"              \"originalParent\": \"\",\n" +
			"              \"originalTopic\": \"\",\n" +
			"              \"children\": [],\n" +
			"              \"status\": \"none\",\n" +
			"              \"parent\": null\n" +
			"            }\n" +
			"          ],\n" +
			"          \"status\": null,\n" +
			"          \"parent\": null\n" +
			"    }]\n" +
			"}";

    public PartonomyTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void test() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String data = "{\"components\":[{\"root\":\"Root term\",\"children\":[{\"root\": \"term1\"}]}]}";
        final Partonomy partonomy = mapper.readValue(SAMPLE_PARTONOMY, Partonomy.class);
        assertEquals(2, partonomy.getComponents().size());
        assertEquals(1, partonomy.getComponents().get(0).children.get(0).size());
        final String json = mapper.writeValueAsString(partonomy);
        assertEquals(partonomy, mapper.readValue(json, Partonomy.class));
    }

}