package org.insightcentre.saffron.web.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.glassfish.jersey.server.JSONP;
import org.insightcentre.saffron.web.SaffronData;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;

@Path("/api/v1/run")
public class SaffronAPI{

    @GET
    @JSONP
    @Path("/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRun(@PathParam("param") String name) {
        SaffronData saffronData;
        try {

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }
        BaseResponse resp = new BaseResponse();
        resp.setId(name);
        resp.setRunDate(new Date());
        return Response.ok(resp).build();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRuns(InputStream incomingData) {
        List<BaseResponse> runsResponse = new ArrayList<>();

        MongoDBHandler mongo = new MongoDBHandler("localhost", 27017, "saffron", "saffron_runs");
        FindIterable<Document> runs;

        try {
            runs = mongo.getAllRuns();

            System.out.println("HERE");
            for (Document doc : runs) {
                BaseResponse entity = new BaseResponse();
                System.out.println(doc.getInteger("occurences"));
                entity.setId(doc.getString("id"));
                entity.setRunDate(doc.getDate("run_date"));
                runsResponse.add(entity);
            }
            mongo.close();
        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }


        String json = new Gson().toJson(runsResponse);
        return Response.ok(json).build();
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postRun(InputStream incomingData) {
        StringBuilder crunchifyBuilder = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(incomingData));
            String line = null;
            while ((line = in.readLine()) != null) {
                crunchifyBuilder.append(line);
                System.out.println(line);
            }
        } catch (Exception e) {
            System.out.println("Error Parsing: - ");
        }
        System.out.println("Data Received: " + crunchifyBuilder.toString());


        BaseResponse resp = new BaseResponse();
        resp.setId("1234");
        resp.setRunDate(new Date());
        return Response.ok(resp).build();
    }

    @GET
    @JSONP
    @Path("/{param}/topics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRunTopics(@PathParam("param") String runId) {
        List<TopicResponse> topicsResponse = new ArrayList<>();
        TopicsResponse resp = new TopicsResponse();
        MongoDBHandler mongo = new MongoDBHandler("localhost", 27017, "saffron", "saffron_runs");
        FindIterable<Document> topics;

        try {
            topics = mongo.getTopics(runId);

            System.out.println("HERE");
            for (Document doc : topics) {
                TopicResponse entity = new TopicResponse();
                System.out.println(doc.getInteger("occurences"));
                entity.setId(doc.getString("_id"));
                entity.setMatches(doc.getInteger("matches"));
                entity.setOccurrences(doc.getInteger("occurences"));
                entity.setScore(doc.getDouble("score"));
                entity.setTopicString(doc.getString("topicString"));
                entity.setMvList((List<String>) doc.get("mvList"));
                topicsResponse.add(entity);
            }

            mongo.close();
        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }


        String json = new Gson().toJson(topicsResponse);
        return Response.ok(json).build();
    }


    @DELETE
    @JSONP
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteTopic(@PathParam("param") String name,
                                @PathParam("topic_id") String topicId) {

        return Response.ok("Topic " + name + " " + topicId + " Deleted").build();
    }


    @POST
    @Path("/{param}/topics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postDeleteManyTopics(InputStream incomingData) {
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

        System.out.println("Data Received: " + crunchifyBuilder.toString());


        return Response.ok("Topics " + crunchifyBuilder.toString() + " Deleted").build();
    }


    @POST
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postChangeTopicRoot(InputStream incomingData) {
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

        System.out.println("Data Received: " + crunchifyBuilder.toString());


        return Response.ok("Topics " + crunchifyBuilder.toString() + " Deleted").build();
    }

    @PUT
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putNewTopic(InputStream incomingData) {
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

        System.out.println("Data Received: " + crunchifyBuilder.toString());


        return Response.ok("Topics " + crunchifyBuilder.toString() + " Deleted").build();
    }


}