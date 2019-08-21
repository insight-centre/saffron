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

    public static Taxonomy updateTaxonomyParentRelationship(Taxonomy originalTaxo, String topicString, String newParentString, String oldParentString, Taxonomy topic, Taxonomy newParent) {
        Taxonomy finalTaxon;
        newParent = newParent.addChild(topic, newParent, oldParentString);
        finalTaxon = originalTaxo.deepCopyNewParent(topicString, oldParentString, newParentString, topic, newParent);
        finalTaxon = finalTaxon.deepCopyNewTaxo(newParentString, topic, finalTaxon);
        finalTaxon = finalTaxon.deepCopySetTopicRelationshipStatus(topicString, Status.accepted);
        return finalTaxon;
    }


    public static Taxonomy updateTopicStatusAndRelations(Taxonomy originalTaxo, String topicString, String status) {
        Taxonomy finalTaxon;
        Taxonomy topic = originalTaxo.descendent(topicString);
        Taxonomy topicParent = originalTaxo.antecendent(topicString, "", topic, null);
        if (status.equals("rejected")) {

            finalTaxon = originalTaxo.deepCopyMoveChildTopics(topicString, topic, topicParent);
            // If we are at top root, just use the resulting taxonomy
            if (!finalTaxon.root.equals(originalTaxo.root)) {
                finalTaxon = finalTaxon.deepCopyUpdatedTaxo(topicString, finalTaxon, originalTaxo);
            }

        } else {
            finalTaxon = originalTaxo.deepCopySetTopicStatus(topicString, Status.accepted);
        }
        return finalTaxon;
    }
}
