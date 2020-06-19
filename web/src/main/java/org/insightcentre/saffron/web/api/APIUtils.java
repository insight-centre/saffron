package org.insightcentre.saffron.web.api;

import com.mongodb.client.FindIterable;
import joptsimple.OptionParser;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class APIUtils {
    public APIUtils() {
    }

    protected static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    protected StringBuilder getJsonData(InputStream incomingData) {
        StringBuilder crunchifyBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
            String line = null;
            while ((line = in.readLine()) != null) {
                crunchifyBuilder.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return crunchifyBuilder;
    }

    protected void populateAuthorSimilarityResponse(FindIterable<Document> runs, List<AuthorSimilarityResponse> termsResponse) {
        for (Document doc : runs) {

            AuthorSimilarityResponse entity = new AuthorSimilarityResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setSimilarity(doc.getDouble("similarity"));
            entity.setTermString1(doc.getString("term1"));
            entity.setTermString2(doc.getString("term2"));

            termsResponse.add(entity);
        }
    }

    protected void populateTermCorrespondenceResp(FindIterable<Document> runs, List<TermCorrespondenceResponse> termsResponse) {
        for (Document doc : runs) {

            TermCorrespondenceResponse entity = new TermCorrespondenceResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setTerm(doc.getString("term_string"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setAcronym(doc.getString("acronym"));
            entity.setOccurrences(doc.getInteger("occurences"));
            entity.setPattern(doc.getString("pattern"));
            entity.setTfidf(doc.getDouble("tfidf"));

            entity.setDocumentId(doc.getString("document_id"));

            termsResponse.add(entity);
        }
    }

    protected void populateTermExtractionResp(FindIterable<Document> runs, List<TermExtractionResponse> termsResponse) {
        for (Document doc : runs) {

            TermExtractionResponse entity = new TermExtractionResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setScore(doc.getDouble("score"));
            entity.setTerm(doc.getString("term"));
            entity.setDbpediaUrl(doc.getString("dbpedia_url"));
            entity.setMvList((List<String>) doc.get("mvList"));
            entity.setOccurrences(doc.getInteger("occurrences"));
            entity.setMatches(doc.getInteger("matches"));

            termsResponse.add(entity);
        }
    }

    protected void populateTermSimilarityResp(FindIterable<Document> runs, List<TermSimilarityResponse> termsResponse) {
        for (Document doc : runs) {

            TermSimilarityResponse entity = new TermSimilarityResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setSimilarity(doc.getDouble("similarity"));
            entity.setTermString1(doc.getString("term1_id"));
            entity.setTermString2(doc.getString("term2_id"));

            termsResponse.add(entity);
        }
    }
}
