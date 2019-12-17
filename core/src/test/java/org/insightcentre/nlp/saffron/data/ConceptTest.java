package org.insightcentre.nlp.saffron.data;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unitary tests for {@code Concept} class
 * 
 * @author Bianca Pereira
 *
 */
public class ConceptTest {

	@Test
    public void testJsonSerialisation() throws IOException {
		//Prepare
		final String input = "{" +
				"\""+ Concept.JSON_ID +"\":\"1234\"," +
				"\"" + Concept.JSON_PREFERRED_TERM + "\":\"term string\"," +
				"\"" + Concept.JSON_SYNONYMS + "\": [" +
					"\"synonym 1\", \"synonym 2\",\"synonym 3\"" +
				"]" +
			"}";
		
		Set<Term> expectedSynonyms = new HashSet<Term>();
		expectedSynonyms.add(new Term.Builder("synonym 1").build());
		expectedSynonyms.add(new Term.Builder("synonym 2").build());
		expectedSynonyms.add(new Term.Builder("synonym 3").build());
		
		//Call
		ObjectMapper mapper = new ObjectMapper();
		final Concept actual = mapper.readValue(input, Concept.class);
		
		//Evaluate
		assertEquals("1234", actual.getId());
		assertEquals(new Term.Builder("term string").build(), actual.getPreferredTerm());
		assertEquals(expectedSynonyms, actual.getSynonyms());
        
    }
	
	/**
	 * Test with no synonyms
	 * 
	 * @throws IOException
	 */
	@Test
    public void testJsonSerialisation2() throws IOException {
		//Prepare
		final String input = "{" +
				"\""+ Concept.JSON_ID +"\":\"1234\"," +
				"\"" + Concept.JSON_PREFERRED_TERM + "\":\"term string\"" +
			"}";
		
		//Call
		ObjectMapper mapper = new ObjectMapper();
		final Concept actual = mapper.readValue(input, Concept.class);
		
		//Evaluate
		assertEquals("1234", actual.getId());
		assertEquals(new Term.Builder("term string").build(), actual.getPreferredTerm());
        
    }
	
	/* FIXME: How to test serialisation when the order of the values in the array may change?
	@Test
    public void testJsonDeserialisation() throws IOException {
		//Prepare
		final String expected = "{" +
				"\""+ Concept.JSON_ID +"\":\"1234\"," +
				"\"" + Concept.JSON_PREFERRED_TERM + "\":\"term string\"," +
				"\"" + Concept.JSON_SYNONYMS + "\": [" +
					"\"synonym 1\", \"synonym 2\",\"synonym 3\"" +
				"]" +
			"}";
		
		Concept input = new Concept.Builder("1234", "term string")
				.addSynonym("synonym 1")
				.addSynonym("synonym 2")
				.addSynonym("synonym 3")
				.build();
		
		//Call
		ObjectMapper mapper = new ObjectMapper();
		final String actual = mapper.writeValueAsString(input);
		
		//Evaluate		
        assertEquals(expected,actual);
        
    }*/
	
	/**
	 * Test with no synonyms
	 * 
	 * @throws IOException
	 */
	@Test
    public void testJsonDeserialisation2() throws IOException {
		//Prepare
		final String expected = "{" +
				"\""+ Concept.JSON_ID +"\":\"1234\"," +
				"\"" + Concept.JSON_PREFERRED_TERM + "\":\"term string\"" +
			"}";
		
		Concept input = new Concept.Builder("1234", "term string")
				.build();
		
		//Call
		ObjectMapper mapper = new ObjectMapper();
		final String actual = mapper.writeValueAsString(input);
		
		//Evaluate		
        assertEquals(expected,actual);
    }

}
