package org.insightcentre.nlp.saffron.taxonomy.extract;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Ontonomy;
import org.insightcentre.nlp.saffron.data.Partonomy;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.junit.Test;

public class ConvertKGtoRDFTest {
	@Test
	public void testTaxo() {		
		// Prepare
		Taxonomy taxo = new Taxonomy.Builder()
				.root("term1")
				.addChild(
						new Taxonomy.Builder()
						.root("term11")
						.addChild(
								new Taxonomy.Builder()
								.root("term111")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term112")
								.build()
						).build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term12")
						.build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term13")
						.addChild(
								new Taxonomy.Builder()
								.root("term131")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term132")
								.build()
						).build()
				)
				.build();
		
		
		
		KnowledgeGraph input = new KnowledgeGraph();
		input.setTaxonomy(taxo);
		
		String expected = "" +
				
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term12> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/2000/01/rdf-schema#label> \"term1\" .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term11> .\n" +
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term13> .\n" + 
				
				"<base/rdf/term/term11> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term111> .\n" + 
				"<base/rdf/term/term11> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term112> .\n" + 
				"<base/rdf/term/term11> <http://www.w3.org/2000/01/rdf-schema#label> \"term11\" .\n" + 
				"<base/rdf/term/term11> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term111> <http://www.w3.org/2000/01/rdf-schema#label> \"term111\" .\n" + 
				"<base/rdf/term/term111> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term112> <http://www.w3.org/2000/01/rdf-schema#label> \"term112\" .\n" + 
				"<base/rdf/term/term112> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term12> <http://www.w3.org/2000/01/rdf-schema#label> \"term12\" .\n" + 
				"<base/rdf/term/term12> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				
				"<base/rdf/term/term13> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term131> .\n" + 
				"<base/rdf/term/term13> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term132> .\n" + 
				"<base/rdf/term/term13> <http://www.w3.org/2000/01/rdf-schema#label> \"term13\" .\n" + 
				"<base/rdf/term/term13> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term131> <http://www.w3.org/2000/01/rdf-schema#label> \"term131\" .\n" + 
				"<base/rdf/term/term131> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term132> <http://www.w3.org/2000/01/rdf-schema#label> \"term132\" .\n" + 
				"<base/rdf/term/term132> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n"; 
 	
		
		// Execute
		Model result = ConvertKGToRDF.convertToRDF("base", input);
		
        // Evaluate
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RDFDataMgr.write(stream, result, Lang.NTRIPLES);

        String actual = new String(stream.toByteArray());
        
        List<String> expectedLines = Arrays.asList(expected.split("\\n"));
        List<String> actualLines = Arrays.asList(actual.split("\\n"));
        
        assertTrue("The resulting RDF has the wrong amount of statements", expectedLines.size() == actualLines.size());
        assertTrue("The resulting RDF has unexpected or wrong statements", expectedLines.containsAll(actualLines));
        assertTrue("The resulting RDF does not have all expected statements", actualLines.containsAll(expectedLines));
		
	}
	
	@Test
	public void testSynonymy() {		
		// Prepare		
		List<Set<String>> synonymyClusters = new ArrayList<Set<String>>();
		synonymyClusters.add(new HashSet<String>(Arrays.asList("term1","term01","term001")));
		synonymyClusters.add(new HashSet<String>(Arrays.asList("term232","term0232","term00232")));
				
		KnowledgeGraph input = new KnowledgeGraph();
		input.setSynonymyClusters(synonymyClusters);
		
		String expected = "" +
				"<base/rdf/term/term01> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term001> .\n" + 
				"<base/rdf/term/term01> <http://www.w3.org/2000/01/rdf-schema#label> \"term01\" .\n" + 
				"<base/rdf/term/term01> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term001> <http://www.w3.org/2000/01/rdf-schema#label> \"term001\" .\n" + 
				"<base/rdf/term/term001> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term0232> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term232> .\n" + 
				"<base/rdf/term/term0232> <http://www.w3.org/2000/01/rdf-schema#label> \"term0232\" .\n" + 
				"<base/rdf/term/term0232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				
				"<base/rdf/term/term00232> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term232> .\n" + 
				"<base/rdf/term/term00232> <http://www.w3.org/2000/01/rdf-schema#label> \"term00232\" .\n" + 
				"<base/rdf/term/term00232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/2000/01/rdf-schema#label> \"term1\" .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term001> .\n" + 
				
				"<base/rdf/term/term232> <http://www.w3.org/2000/01/rdf-schema#label> \"term232\" .\n" + 
				"<base/rdf/term/term232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> ."; 
 	
		
		// Execute
		Model result = ConvertKGToRDF.convertToRDF("base", input);
		
        // Evaluate
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RDFDataMgr.write(stream, result, Lang.NTRIPLES);

        String actual = new String(stream.toByteArray());
        
        List<String> expectedLines = Arrays.asList(expected.split("\\n"));
        List<String> actualLines = Arrays.asList(actual.split("\\n"));
        System.out.println(actualLines.size());
        assertTrue("The resulting RDF has the wrong amount of statements", expectedLines.size() == actualLines.size());
        assertTrue("The resulting RDF has unexpected or wrong statements", expectedLines.containsAll(actualLines));
        assertTrue("The resulting RDF does not have all expected statements", actualLines.containsAll(expectedLines));
		
	}
	
	@Test
	public void testOntonomy() {		
		// Prepare		
		Ontonomy ontonomy = new Ontonomy.Builder()
				.addRelation(new TypedLink("term1","term2",TypedLink.Type.atLocation))
				.addRelation(new TypedLink("term1","term2",TypedLink.Type.usedFor))
				.addRelation(new TypedLink("term1","term2",TypedLink.Type.hasPrerequisite))
				.build();
		
		KnowledgeGraph input = new KnowledgeGraph();
		input.setOntonomy(ontonomy);
		
		String expected = "" +			
				"<base/rdf/term/term1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/2000/01/rdf-schema#label> \"term1\" .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#atLocation> <base/rdf/term/term2> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#usedFor> <base/rdf/term/term2> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hasPrerequisite> <base/rdf/term/term2> .\n" + 
							
				"<base/rdf/term/term2> <http://www.w3.org/2000/01/rdf-schema#label> \"term2\" .\n" + 
				"<base/rdf/term/term2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" ; 
 	
		
		// Execute
		Model result = ConvertKGToRDF.convertToRDF("base", input);
		
        // Evaluate
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RDFDataMgr.write(stream, result, Lang.NTRIPLES);

        String actual = new String(stream.toByteArray());
        
        List<String> expectedLines = Arrays.asList(expected.split("\\n"));
        List<String> actualLines = Arrays.asList(actual.split("\\n"));
        
        assertTrue("The resulting RDF has the wrong amount of statements", expectedLines.size() == actualLines.size());
        assertTrue("The resulting RDF has unexpected or wrong statements", expectedLines.containsAll(actualLines));
        assertTrue("The resulting RDF does not have all expected statements", actualLines.containsAll(expectedLines));
		
	}
	
	@Test
	public void testPartonomy() {		
		// Prepare
		Taxonomy taxo1 = new Taxonomy.Builder()
				.root("term1")
				.addChild(
						new Taxonomy.Builder()
						.root("term11")
						.addChild(
								new Taxonomy.Builder()
								.root("term111")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term112")
								.build()
						).build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term12")
						.build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term13")
						.addChild(
								new Taxonomy.Builder()
								.root("term131")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term132")
								.build()
						).build()
				)
				.build();
		
		Taxonomy taxo2 = new Taxonomy.Builder()
				.root("term2")
				.addChild(
						new Taxonomy.Builder()
						.root("term21")
						.addChild(
								new Taxonomy.Builder()
								.root("term211")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term212")
								.build()
						).build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term22")
						.build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term23")
						.addChild(
								new Taxonomy.Builder()
								.root("term231")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term232")
								.build()
						).build()
				)
				.build();
		
		Partonomy parto = new Partonomy.Builder().addComponent(taxo1).addComponent(taxo2).build();
		
		KnowledgeGraph input = new KnowledgeGraph();
		input.setPartonomy(parto);
		
		String expected = "" +				
				"<base/rdf/term/term1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/2000/01/rdf-schema#label> \"term1\" .\n" + 
				
				"<base/rdf/term/term11> <http://www.w3.org/2000/01/rdf-schema#label> \"term11\" .\n" + 
				"<base/rdf/term/term11> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term11> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term1> .\n" + 
				
				"<base/rdf/term/term111> <http://www.w3.org/2000/01/rdf-schema#label> \"term111\" .\n" + 
				"<base/rdf/term/term111> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term111> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term11> .\n" +
				
				"<base/rdf/term/term112> <http://www.w3.org/2000/01/rdf-schema#label> \"term112\" .\n" + 
				"<base/rdf/term/term112> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term112> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term11> .\n" +
				
				"<base/rdf/term/term12> <http://www.w3.org/2000/01/rdf-schema#label> \"term12\" .\n" + 
				"<base/rdf/term/term12> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term12> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term1> .\n" + 
				
				"<base/rdf/term/term13> <http://www.w3.org/2000/01/rdf-schema#label> \"term13\" .\n" + 
				"<base/rdf/term/term13> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term13> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term1> .\n" + 
				
				"<base/rdf/term/term131> <http://www.w3.org/2000/01/rdf-schema#label> \"term131\" .\n" + 
				"<base/rdf/term/term131> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term131> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term13> .\n" + 
				
				"<base/rdf/term/term132> <http://www.w3.org/2000/01/rdf-schema#label> \"term132\" .\n" + 
				"<base/rdf/term/term132> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term132> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term13> .\n" + 
				
				"<base/rdf/term/term2> <http://www.w3.org/2000/01/rdf-schema#label> \"term2\" .\n" + 
				"<base/rdf/term/term2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term21> <http://www.w3.org/2000/01/rdf-schema#label> \"term21\" .\n" + 
				"<base/rdf/term/term21> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term21> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term2> .\n" +
				
				"<base/rdf/term/term211> <http://www.w3.org/2000/01/rdf-schema#label> \"term211\" .\n" + 
				"<base/rdf/term/term211> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term211> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term21> .\n" + 
 
				"<base/rdf/term/term212> <http://www.w3.org/2000/01/rdf-schema#label> \"term212\" .\n" + 
				"<base/rdf/term/term212> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term212> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term21> .\n" + 
				 
				"<base/rdf/term/term22> <http://www.w3.org/2000/01/rdf-schema#label> \"term22\" .\n" + 
				"<base/rdf/term/term22> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term22> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term2> .\n" +
				
				"<base/rdf/term/term23> <http://www.w3.org/2000/01/rdf-schema#label> \"term23\" .\n" + 
				"<base/rdf/term/term23> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term23> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term2> .\n" +
				
				"<base/rdf/term/term231> <http://www.w3.org/2000/01/rdf-schema#label> \"term231\" .\n" + 
				"<base/rdf/term/term231> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term231> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term23> .\n" + 
				
				"<base/rdf/term/term232> <http://www.w3.org/2000/01/rdf-schema#label> \"term232\" .\n" + 
				"<base/rdf/term/term232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term232> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term23> .\n"; 
 	
		
		// Execute
		Model result = ConvertKGToRDF.convertToRDF("base", input);
		
        // Evaluate
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RDFDataMgr.write(stream, result, Lang.NTRIPLES);

        String actual = new String(stream.toByteArray());
        
        List<String> expectedLines = Arrays.asList(expected.split("\\n"));
        List<String> actualLines = Arrays.asList(actual.split("\\n"));
        
        assertTrue("The resulting RDF has the wrong amount of statements", expectedLines.size() == actualLines.size());
        assertTrue("The resulting RDF has unexpected or wrong statements", expectedLines.containsAll(actualLines));
        assertTrue("The resulting RDF does not have all expected statements", actualLines.containsAll(expectedLines));
		
	}
	
	
	@Test
	public void testFullKG() {		
		// Prepare
		Taxonomy taxo1 = new Taxonomy.Builder()
				.root("term1")
				.addChild(
						new Taxonomy.Builder()
						.root("term11")
						.addChild(
								new Taxonomy.Builder()
								.root("term111")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term112")
								.build()
						).build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term12")
						.build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term13")
						.addChild(
								new Taxonomy.Builder()
								.root("term131")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term132")
								.build()
						).build()
				)
				.build();
		
		Taxonomy taxo2 = new Taxonomy.Builder()
				.root("term2")
				.addChild(
						new Taxonomy.Builder()
						.root("term21")
						.addChild(
								new Taxonomy.Builder()
								.root("term211")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term212")
								.build()
						).build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term22")
						.build()
				)
				.addChild(
						new Taxonomy.Builder()
						.root("term23")
						.addChild(
								new Taxonomy.Builder()
								.root("term231")
								.build()
						)
						.addChild(
								new Taxonomy.Builder()
								.root("term232")
								.build()
						).build()
				)
				.build();
		
		Partonomy parto = new Partonomy.Builder().addComponent(taxo1).addComponent(taxo2).build();
		
		List<Set<String>> synonymyClusters = new ArrayList<Set<String>>();
		synonymyClusters.add(new HashSet<String>(Arrays.asList("term1","term01","term001")));
		synonymyClusters.add(new HashSet<String>(Arrays.asList("term232","term0232","term00232")));
				
		Ontonomy ontonomy = new Ontonomy.Builder()
				.addRelation(new TypedLink("term1","term2",TypedLink.Type.atLocation))
				.addRelation(new TypedLink("term1","term2",TypedLink.Type.usedFor))
				.addRelation(new TypedLink("term1","term2",TypedLink.Type.hasPrerequisite))
				.build();
		
		KnowledgeGraph input = new KnowledgeGraph();
		input.setTaxonomy(taxo1);
		input.setPartonomy(parto);
		input.setSynonymyClusters(synonymyClusters);
		input.setOntonomy(ontonomy);
		
		String expected = "" +
				"<base/rdf/term/term01> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term001> .\n" + 
				"<base/rdf/term/term01> <http://www.w3.org/2000/01/rdf-schema#label> \"term01\" .\n" + 
				"<base/rdf/term/term01> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term001> <http://www.w3.org/2000/01/rdf-schema#label> \"term001\" .\n" + 
				"<base/rdf/term/term001> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term0232> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term232> .\n" + 
				"<base/rdf/term/term0232> <http://www.w3.org/2000/01/rdf-schema#label> \"term0232\" .\n" + 
				"<base/rdf/term/term0232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				
				"<base/rdf/term/term00232> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term232> .\n" + 
				"<base/rdf/term/term00232> <http://www.w3.org/2000/01/rdf-schema#label> \"term00232\" .\n" + 
				"<base/rdf/term/term00232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term12> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" + 
				"<base/rdf/term/term1> <http://www.w3.org/2000/01/rdf-schema#label> \"term1\" .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#atLocation> <base/rdf/term/term2> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#synonymy> <base/rdf/term/term001> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#usedFor> <base/rdf/term/term2> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term11> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hasPrerequisite> <base/rdf/term/term2> .\n" + 
				"<base/rdf/term/term1> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term13> .\n" + 
				
				"<base/rdf/term/term11> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term111> .\n" + 
				"<base/rdf/term/term11> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term112> .\n" + 
				"<base/rdf/term/term11> <http://www.w3.org/2000/01/rdf-schema#label> \"term11\" .\n" + 
				"<base/rdf/term/term11> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term11> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term1> .\n" + 
				
				"<base/rdf/term/term111> <http://www.w3.org/2000/01/rdf-schema#label> \"term111\" .\n" + 
				"<base/rdf/term/term111> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term111> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term11> .\n" +
				
				"<base/rdf/term/term112> <http://www.w3.org/2000/01/rdf-schema#label> \"term112\" .\n" + 
				"<base/rdf/term/term112> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term112> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term11> .\n" +
				
				"<base/rdf/term/term12> <http://www.w3.org/2000/01/rdf-schema#label> \"term12\" .\n" + 
				"<base/rdf/term/term12> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term12> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term1> .\n" + 
				
				"<base/rdf/term/term13> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term131> .\n" + 
				"<base/rdf/term/term13> <http://saffron.insight-centre.org/ontology#hypernymy> <base/rdf/term/term132> .\n" + 
				"<base/rdf/term/term13> <http://www.w3.org/2000/01/rdf-schema#label> \"term13\" .\n" + 
				"<base/rdf/term/term13> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term13> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term1> .\n" + 
				
				"<base/rdf/term/term131> <http://www.w3.org/2000/01/rdf-schema#label> \"term131\" .\n" + 
				"<base/rdf/term/term131> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term131> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term13> .\n" + 
				
				"<base/rdf/term/term132> <http://www.w3.org/2000/01/rdf-schema#label> \"term132\" .\n" + 
				"<base/rdf/term/term132> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term132> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term13> .\n" + 
				
				"<base/rdf/term/term2> <http://www.w3.org/2000/01/rdf-schema#label> \"term2\" .\n" + 
				"<base/rdf/term/term2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				
				"<base/rdf/term/term21> <http://www.w3.org/2000/01/rdf-schema#label> \"term21\" .\n" + 
				"<base/rdf/term/term21> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term21> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term2> .\n" +
				
				"<base/rdf/term/term211> <http://www.w3.org/2000/01/rdf-schema#label> \"term211\" .\n" + 
				"<base/rdf/term/term211> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term211> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term21> .\n" + 
 
				"<base/rdf/term/term212> <http://www.w3.org/2000/01/rdf-schema#label> \"term212\" .\n" + 
				"<base/rdf/term/term212> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term212> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term21> .\n" + 
				 
				"<base/rdf/term/term22> <http://www.w3.org/2000/01/rdf-schema#label> \"term22\" .\n" + 
				"<base/rdf/term/term22> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term22> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term2> .\n" +
				
				"<base/rdf/term/term23> <http://www.w3.org/2000/01/rdf-schema#label> \"term23\" .\n" + 
				"<base/rdf/term/term23> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term23> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term2> .\n" +
				
				"<base/rdf/term/term231> <http://www.w3.org/2000/01/rdf-schema#label> \"term231\" .\n" + 
				"<base/rdf/term/term231> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term231> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term23> .\n" + 
				
				"<base/rdf/term/term232> <http://www.w3.org/2000/01/rdf-schema#label> \"term232\" .\n" + 
				"<base/rdf/term/term232> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2004/02/skos/core#Concept> .\n" +
				"<base/rdf/term/term232> <http://saffron.insight-centre.org/ontology#meronymy> <base/rdf/term/term23> ."; 
 	
		
		// Execute
		Model result = ConvertKGToRDF.convertToRDF("base", input);
		
        // Evaluate
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RDFDataMgr.write(stream, result, Lang.NTRIPLES);

        String actual = new String(stream.toByteArray());
        
        List<String> expectedLines = Arrays.asList(expected.split("\\n"));
        List<String> actualLines = Arrays.asList(actual.split("\\n"));
        
        assertTrue("The resulting RDF has the wrong amount of statements", expectedLines.size() == actualLines.size());
        assertTrue("The resulting RDF has unexpected or wrong statements", expectedLines.containsAll(actualLines));
        assertTrue("The resulting RDF does not have all expected statements", actualLines.containsAll(expectedLines));
		
	}

}
