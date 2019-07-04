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
            System.out.println("Error Parsing: - ");
        }
        return crunchifyBuilder;
    }

    protected void populateAuthorTopicsResp(FindIterable<Document> runs, List<AuthorTopicsResponse> topicsResponse) {
        for (Document doc : runs) {

            AuthorTopicsResponse entity = new AuthorTopicsResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setAuthorTopic(doc.getString("author_topic"));
            entity.setMvList((List<String>) doc.get("mvList"));
            entity.setTopicString(doc.getString("topicString"));
            entity.setOccurrences(doc.getInteger("occurences"));
            entity.setMatches(doc.getInteger("matches"));
            entity.setScore(doc.getDouble("score"));
            entity.setDbpediaUrl(doc.getString("dbpedia_url"));

            topicsResponse.add(entity);
        }
    }

    protected void populateAuthorSimilarityResponse(FindIterable<Document> runs, List<AuthorSimilarityResponse> topicsResponse) {
        for (Document doc : runs) {

            AuthorSimilarityResponse entity = new AuthorSimilarityResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setSimilarity(doc.getDouble("similarity"));
            entity.setTopicString1(doc.getString("topic1"));
            entity.setTopicString2(doc.getString("topic2"));

            topicsResponse.add(entity);
        }
    }

    protected void populateTopicCorrespondenceResp(FindIterable<Document> runs, List<TopicCorrespondenceResponse> topicsResponse) {
        for (Document doc : runs) {

            TopicCorrespondenceResponse entity = new TopicCorrespondenceResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setAcronym(doc.getString("acronym"));
            entity.setOccurrences(doc.getInteger("occurences"));
            entity.setPattern(doc.getString("pattern"));
            entity.setTfidf(doc.getString("tfidf"));
            entity.setTopic(doc.getString("topic"));
            entity.setDocumentId(doc.getString("document_id"));

            topicsResponse.add(entity);
        }
    }

    protected void populateTopicExtractionResp(FindIterable<Document> runs, List<TopicExtractionResponse> topicsResponse) {
        for (Document doc : runs) {

            TopicExtractionResponse entity = new TopicExtractionResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setScore(doc.getDouble("score"));
            entity.setTopic(doc.getString("topic"));
            entity.setDbpediaUrl(doc.getString("dbpedia_url"));
            entity.setMvList((List<String>) doc.get("mvList"));
            entity.setOccurrences(doc.getInteger("occurrences"));
            entity.setMatches(doc.getInteger("matches"));

            topicsResponse.add(entity);
        }
    }

    protected void populateTopicSimilarityResp(FindIterable<Document> runs, List<TopicSimilarityResponse> topicsResponse) {
        for (Document doc : runs) {

            TopicSimilarityResponse entity = new TopicSimilarityResponse();
            entity.setId(doc.getString("_id"));
            entity.setRun(doc.getString("run"));
            entity.setRunDate(doc.getDate("run_date"));
            entity.setSimilarity(doc.getDouble("similarity"));
            entity.setTopicString1(doc.getString("topic1"));
            entity.setTopicString2(doc.getString("topic2"));

            topicsResponse.add(entity);
        }
    }
}