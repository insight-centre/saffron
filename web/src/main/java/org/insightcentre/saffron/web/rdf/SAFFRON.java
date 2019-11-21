package org.insightcentre.saffron.web.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

/**
 *
 * @author John McCrae
 */
public class SAFFRON {

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static final Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://saffron.insight-centre.org/ontology#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
        
    public static final Property occurrences = m_model.createProperty( NS + "occurrences" );
    public static final Property matches = m_model.createProperty( NS + "matches" );
    public static final Property score = m_model.createProperty( NS + "score" );
    public static final Property morphologicalVariant = m_model.createProperty( NS + "morphologicalVariant" );
    public static final Property author = m_model.createProperty( NS + "author" );
    public static final Property relatedAuthor = m_model.createProperty( NS + "relatedAuthor" );
    public static final Property authorTerm = m_model.createProperty( NS + "authorTerm" );
}
