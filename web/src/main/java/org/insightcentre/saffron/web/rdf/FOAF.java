package org.insightcentre.saffron.web.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author John McCrae
 */
public class FOAF {

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static final Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://xmlns.com/foaf/0.1/";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    public static Resource Agent = m_model.createResource(NS + "Agent");
    public static Resource Document = m_model.createResource(NS + "Document");
    public static Resource Group = m_model.createResource(NS + "Group");
    public static Resource Image = m_model.createResource(NS + "Image");
    public static Resource OnlineAccount = m_model.createResource(NS + "OnlineAccount");
    public static Resource Organization = m_model.createResource(NS + "Organization");
    public static Resource Person = m_model.createResource(NS + "Person");
    public static Resource PersonalProfileDocument = m_model.createResource(NS + "PersonalProfileDocument");
    public static Resource Project = m_model.createResource(NS + "Project");
    public static Property account = m_model.createProperty(NS + "account");
    public static Property accountName = m_model.createProperty(NS + "accountName");
    public static Property accountServiceHomepage = m_model.createProperty(NS + "accountServiceHomepage");
    public static Property age = m_model.createProperty(NS + "age");
    public static Property based_near = m_model.createProperty(NS + "based_near");
    public static Property currentProject = m_model.createProperty(NS + "currentProject");
    public static Property depiction = m_model.createProperty(NS + "depiction");
    public static Property depicts = m_model.createProperty(NS + "depicts");
    public static Property familyName = m_model.createProperty(NS + "familyName");
    public static Property givenName = m_model.createProperty(NS + "givenName");
    public static Property homepage = m_model.createProperty(NS + "homepage");
    public static Property img = m_model.createProperty(NS + "img");
    public static Property interest = m_model.createProperty(NS + "interest");
    public static Property jabberID = m_model.createProperty(NS + "jabberID");
    public static Property knows = m_model.createProperty(NS + "knows");
    public static Property logo = m_model.createProperty(NS + "logo");
    public static Property made = m_model.createProperty(NS + "made");
    public static Property maker = m_model.createProperty(NS + "maker");
    public static Property mbox = m_model.createProperty(NS + "mbox");
    public static Property mbox_sha1sum = m_model.createProperty(NS + "mbox_sha1sum");
    public static Property member = m_model.createProperty(NS + "member");
    public static Property name = m_model.createProperty(NS + "name");
    public static Property nick = m_model.createProperty(NS + "nick");
    public static Property openid = m_model.createProperty(NS + "openid");
    public static Property pastProject = m_model.createProperty(NS + "pastProject");
    public static Property primaryTopic  = m_model.createProperty(NS + "primaryTopic ");
    public static Property primaryTopicOf = m_model.createProperty(NS + "primaryTopicOf");
    public static Property publications = m_model.createProperty(NS + "publications");
    public static Property schoolHomepage = m_model.createProperty(NS + "schoolHomepage");
    public static Property sha1 = m_model.createProperty(NS + "sha1");
    public static Property thumbnail = m_model.createProperty(NS + "thumbnail");
    public static Property tipjar = m_model.createProperty(NS + "tipjar");
    public static Property title = m_model.createProperty(NS + "title");
    public static Property topic = m_model.createProperty(NS + "topic");
    public static Property page = m_model.createProperty(NS + "page");
    public static Property topic_interest = m_model.createProperty(NS + "topic_interest");
    public static Property weblog = m_model.createProperty(NS + "weblog");
    public static Property workInfoHomepage = m_model.createProperty(NS + "workInfoHomepage");
    public static Property workplaceHomepage = m_model.createProperty(NS + "workplaceHomepage");
    public static Resource KnowledgeGraph = m_model.createResource(NS + "KnowledgeGraph");
}
