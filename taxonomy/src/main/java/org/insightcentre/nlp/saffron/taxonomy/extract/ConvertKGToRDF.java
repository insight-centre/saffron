package org.insightcentre.nlp.saffron.taxonomy.extract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDFS;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.data.VirtualRootTaxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ConvertKGToRDF {

    public static final String SAFFRON_NS = "http://saffron.insight-centre.org/ontology#";

    public static final String SKOS = "http://www.w3.org/2004/02/skos/core#";

    public static enum RDFFormats {
    	XML, N3, NTRIPLES
    }
    
	public static Model convertToRDF(String baseUrl, KnowledgeGraph kg) {
		/*
		 * 1 - Create JENA model
		 * 2 - Collect all relations to appear in RDF
		 * 3 - Create triples for each relation
		 */
		
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("skos", SKOS);
		model.setNsPrefix("saffron", SAFFRON_NS);
		
		Resource termType = model.createResource(SKOS + "Concept");
		Map<String, Resource> termResources = new HashMap<String, Resource>();
		
		Map<TypedLink.Type, Property> predicates = new HashMap<TypedLink.Type, Property>();
		for(TypedLink.Type relationType: TypedLink.Type.values()) {			
			predicates.put(relationType, model.createProperty(SAFFRON_NS + relationType.toString()));
		}
		
		
		final Set<TypedLink> relations = kg.getRelationsByStatus(Status.none);		
		for(TypedLink relation: relations) {
			
			//Blocks output of virtual root
			if(relation.getSource().equals(VirtualRootTaxonomy.VIRTUAL_ROOT))
				continue;
			
			final Resource subject;
			final Resource object;			
			
			if (!termResources.containsKey(relation.getSource())) {
				subject = model.createResource(baseUrl + "/rdf/term/" + relation.getSource(), termType);
				model.add(subject, RDFS.label, relation.getSource());
			} else {
				subject = termResources.get(relation.getSource());
			}
			
			if (!termResources.containsKey(relation.getTarget())) {
				object = model.createResource(baseUrl + "/rdf/term/" + relation.getTarget(), termType);
				model.add(object, RDFS.label, relation.getTarget());
			} else {
				object = termResources.get(relation.getTarget());
			}
			
			model.add(subject,predicates.get(relation.getType()),object);
		}
		
		return model;
	}
	
	public static void writeRDFToFile(Model model, RDFFormats format, File outputFile) throws FileNotFoundException {
		Lang outputFormat = null;
        switch(format) {
        	case XML:
        		outputFormat = Lang.RDFXML;
        		break;
        	case N3:
        		outputFormat = Lang.N3;
        		break;
        	case NTRIPLES:
        		outputFormat = Lang.NTRIPLES;
        		break;
    		default:
    			throw new RuntimeException("Please provide a valid RDF output format: XML, N3 or NTRIPLES");
        }
        RDFDataMgr.write(new FileOutputStream(outputFile), model, outputFormat);
	}
    
    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
    
    public static void main(String[] args) {
        try {
            final OptionParser p = new OptionParser() {
                {
                    accepts("b", "The base url").withRequiredArg().ofType(String.class);
                    accepts("i", "The taxonomy or knowledge graph JSON file").withRequiredArg().ofType(File.class);
                    accepts("o", "The output file path (RDF)").withRequiredArg().ofType(File.class);
                    accepts("f", "The RDF format of the output. One of XML, N3, NTRIPLES (default: XML)").withRequiredArg().ofType(RDFFormats.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            
            if (os.valueOf("b") == null) {
                badOptions(p, "Base url not given");
                return;
            }
            
            if (os.valueOf("i") == null) {
                badOptions(p, "Input file not given");
                return;
            }
            
            if (os.valueOf("o") == null) {
                badOptions(p, "Output file not given");
            }
            
            RDFFormats outputFormat = (RDFFormats) os.valueOf("f");
            if (outputFormat == null) {
                System.err.println("RDF format not informed, using RDF/XML as default.");
                outputFormat = RDFFormats.XML;
            }
            
            /*
             * 1 - Read taxonomy.json OR kg.json
             * 2 - Generate RDF triples
             * 3 - Print RDF file
             */

            ObjectMapper mapper = new ObjectMapper();
            
            String baseUrl = (String) os.valueOf("b");
            KnowledgeGraph kg = mapper.readValue((File) os.valueOf("i"), KnowledgeGraph.class);
            
            if (kg.getTaxonomy() == null) {
            	Taxonomy taxo = mapper.readValue((File) os.valueOf("i"), Taxonomy.class);
            	kg.setTaxonomy(taxo);
            }
            
            Model rdfModel = convertToRDF(baseUrl, kg);
            
            writeRDFToFile(rdfModel, outputFormat,(File) os.valueOf("o"));
            
            
        } catch (Exception x) {
            x.printStackTrace();
            return;
        }
    }
}
