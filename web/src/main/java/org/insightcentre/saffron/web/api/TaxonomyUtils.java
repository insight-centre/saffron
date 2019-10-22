package org.insightcentre.saffron.web.api;

import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.json.JSONObject;

import java.io.IOException;

public class TaxonomyUtils {

    public static Taxonomy getTaxonomyFromDocs(FindIterable<Document> docs, Taxonomy graph) {
        for (Document doc : docs) {
            JSONObject jsonObj = new JSONObject(doc.toJson());
            try {
                graph = Taxonomy.fromJsonString(jsonObj.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return graph;
    }

    public static Taxonomy updateTaxonomyParentRelationship(Taxonomy originalTaxo, String termString, String newParentString, String oldParentString, Taxonomy term, Taxonomy newParent) {
        Taxonomy finalTaxon;
        newParent = newParent.addChild(term, newParent, oldParentString);
        finalTaxon = originalTaxo.deepCopyNewParent(termString, oldParentString, newParentString, term, newParent);
        finalTaxon = finalTaxon.deepCopyNewTaxo(newParentString, term, finalTaxon);
        finalTaxon = finalTaxon.deepCopySetTermRelationshipStatus(termString, Status.accepted);
        return finalTaxon;
    }
}
