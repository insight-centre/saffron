package org.insightcentre.saffron.web;

import java.util.HashSet;
import java.util.Set;

import org.bson.Document;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxoLink;
import org.insightcentre.nlp.saffron.taxonomy.search.TypedLink;

import com.mongodb.client.FindIterable;

/**
 * Contains all the accepted and rejected suggestions packaged so that they can
 * be used in term/taxonomy extraction.
 *
 * @author John McCrae
 * @author Bianca Pereira
 */
public class BlackWhiteList {

    public final Set<String> termWhiteList, termBlackList;
    public final Set<TypedLink> taxoWhiteList, taxoBlackList;

    public BlackWhiteList(Set<String> termWhiteList, Set<String> termBlackList, Set<TypedLink> taxoWhiteList, Set<TypedLink> taxoBlackList) {
        this.termWhiteList = termWhiteList;
        this.termBlackList = termBlackList;
        this.taxoWhiteList = taxoWhiteList;
        this.taxoBlackList = taxoBlackList;
    }

    public BlackWhiteList() {
        this.termWhiteList = new HashSet<>();
        this.termBlackList = new HashSet<>();
        this.taxoWhiteList = new HashSet<>();
        this.taxoBlackList = new HashSet<>();
    }

    public static BlackWhiteList from(FindIterable<Document> allTerms, Taxonomy taxonomy) {

        Set<String> termWhiteList = new HashSet<>();
        Set<String> termBlackList = new HashSet<>();

        for (Document t : allTerms) {
            if (t.getString("status").equals("accepted")) {
                termWhiteList.add(t.getString("term"));

                if (!t.getString("term").equals(t.getString("original_term")) && t.getString("original_term") != null) {
                    termBlackList.add(t.getString("original_term"));
                }
            } else if (t.getString("status").equals("rejected")) {
                termBlackList.add(t.getString("term"));
            }
        }
        termBlackList.removeAll(termWhiteList);
        Set<TypedLink> taxoWhiteList = new HashSet<>();
        Set<TypedLink> taxoBlackList = new HashSet<>();
//        Taxonomy graph = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);
//
//        for (Document taxo : taxonomy) {
//            JSONObject jsonObj = new JSONObject(taxo.toJson());
//            try {
//                graph = Taxonomy.fromJsonString(jsonObj.toString());
//            } catch(Exception e ) {
//
//            }
//        }

        buildTaxoBWList(taxonomy, taxoWhiteList, taxoBlackList);
        taxoBlackList.removeAll(taxoWhiteList);
        return new BlackWhiteList(termWhiteList, termBlackList, taxoWhiteList, taxoBlackList);
    }

    /**
     * Builds a list of accepted and denied relationships to be part of a
     * taxonomy.
     *
     * The children terms are the ones to keep the "accepted" or "none"
     * statuses with their parents. "Rejected" parents are kept as the
     * "originalParent" of a term
     *
     * @param taxonomy - The taxonomy used as source for the acceptance and
     * denial lists
     * @param taxoAcceptanceList - the list to be populated with accepted
     * parent-child relations
     * @param taxoDenialList - the list to be populated with denied parent-child
     * relations
     */
    private static void buildTaxoBWList(Taxonomy taxonomy, Set<TypedLink> taxoAcceptanceList, Set<TypedLink> taxoDenialList) {
        for (Taxonomy child : taxonomy.children) {
            if (child.getStatus().equals(Status.accepted)) {
                taxoAcceptanceList.add(new TaxoLink(taxonomy.root, child.root));
            }
            buildTaxoBWList(child, taxoAcceptanceList, taxoDenialList);
        }
        if (taxonomy.originalParent != null && !taxonomy.originalParent.isEmpty()) {
            taxoDenialList.add(new TaxoLink(taxonomy.originalParent, taxonomy.root));
        }
    }

}
