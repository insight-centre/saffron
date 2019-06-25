package org.insightcentre.saffron.web.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import io.swagger.annotations.*;
import org.bson.Document;
import org.glassfish.jersey.server.JSONP;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.json.JSONArray;
import org.json.JSONObject;
@SwaggerDefinition(


        info = @Info(
                title = "Saffron API",
                version = "1.0.0",
                description = "API for creating and interacting with Saffron taxonomies"
        ),
        basePath = "/",
        consumes = {"application/json"},
        produces = {"application/json"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        tags = {@Tag(name = "users", description = "CRUD operations on user datatype")}
)
@Path("/api/v1/run")
public class SaffronAPI{

    @GET
    @JSONP
    @Path("/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(httpMethod = "GET", value = "Returns a list of the user profile datatype", notes = "", response = String.class, nickname = "getUser", tags = ("User"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Succssful retrieval of user profiles", response = String.class),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "param", value = "profile id", required = false, dataType = "String", paramType = "query"),
    })
    public Response getRun(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        try {
            runs = mongo.getTaxonomy(name);
            for (Document doc : runs) {
                return Response.ok(doc.toJson()).build();
            }
            mongo.close();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRuns(InputStream incomingData) {
        List<BaseResponse> runsResponse = new ArrayList<>();

        MongoDBHandler mongo = getMongoDBHandler();
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




    @DELETE
    @Path("/{param}")
    public Response deleteRun(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        mongo.deleteRun(name);
        return Response.ok("Run " + name + " Deleted").build();
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
        MongoDBHandler mongo = getMongoDBHandler();
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
                entity.setStatus(doc.getString("status"));
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

    @GET
    @JSONP
    @Path("/{param}/search/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSearch(@PathParam("param") String runId, @PathParam("term") String term) {
        List<SearchResponse> searchResponses = new ArrayList<>();
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> topics;

        try {
            topics = mongo.searchTaxonomy(runId, term);

            for (Document doc : topics) {
                SearchResponse entity = new SearchResponse();
                entity.setId(doc.getString("_id"));
                entity.setLocation(doc.getString("document_id"));
                entity.setTopicString(doc.getString("topic"));

                searchResponses.add(entity);

            }

            mongo.close();
        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }


        String json = new Gson().toJson(searchResponses);
        return Response.ok(json).build();
    }


    @GET
    @JSONP
    @Path("/{param}/topics/{topic_id}/children")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopicChildren(@PathParam("param") String runId, @PathParam("topic_id") String topic_id) {
        MongoDBHandler mongo = getMongoDBHandler();

        try {

            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");

            FindIterable<Document> runs = mongo.getTaxonomy(runId);
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

            }

            Taxonomy descendent = originalTaxo.descendent(topic_id);
            String json = new Gson().toJson(descendent);
            return Response.ok(json).build();


        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok().build();
    }


    @GET
    @JSONP
    @Path("/{param}/topics/{topic_id}/parent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopicParent(@PathParam("param") String runId, @PathParam("topic_id") String topic_id) {
        MongoDBHandler mongo = getMongoDBHandler();


        try {
            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");

            FindIterable<Document> runs = mongo.getTaxonomy(runId);
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

            }
            Taxonomy antecendent = originalTaxo.antecendent(topic_id,"", originalTaxo, null);
            String json = new Gson().toJson(antecendent);
            return Response.ok(json).build();


        } catch (Exception e) {

        }

        return Response.ok().build();
    }

    private MongoDBHandler getMongoDBHandler() {
        return new MongoDBHandler("localhost", 27017, "saffron", "saffron_runs");
    }


    @DELETE
    @JSONP
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteTopic(@PathParam("param") String name,
                                @PathParam("topic_id") String topicId) {

        MongoDBHandler mongo = getMongoDBHandler();
        List<TopicResponse> topicsResponse = new ArrayList<>();
        TopicsResponse resp = new TopicsResponse();
        FindIterable<Document> topics;

        topics = mongo.deleteTopic(name, topicId);

        return Response.ok("Topic " + name + " " + topicId + " Deleted").build();
    }

    @POST
    @JSONP
    @Path("/{param}/topics/{topic_id}/{status}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response rejectTopic(@PathParam("param") String name,
                                @PathParam("topic_id") String topicId,
                                @PathParam("status") String status) {

        MongoDBHandler mongo = getMongoDBHandler();
        System.out.println("Here1");
        //mongo.updateTopic(name, topicId, "rejected");
        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");
        FindIterable<Document> runs = mongo.getTaxonomy(name);
        System.out.println("Here2");
        try {
            System.out.println("Here3");
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                System.out.println(jsonObj);
                Taxonomy originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());
                finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, status);
                mongo.updateTopic(name, topicId, status);
                mongo.updateTopicSimilarity(name, topicId, status);
            }

            mongo.updateTaxonomy(name, new Date(), finalTaxon);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topicId + " from the taxonomy " + name);
        }
     return Response.ok("Topic " + name + " " + topicId + " Deleted").build();
    }


    @POST
    @Path("/{param}/topics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postDeleteManyTopics(@PathParam("param") String name, InputStream incomingData) {


        MongoDBHandler mongo = getMongoDBHandler();
        StringBuilder crunchifyBuilder = getJsonData(incomingData);
        FindIterable<Document> topics;

        System.out.println("Data Received: " + crunchifyBuilder.toString());
        JSONObject jsonObj = new JSONObject(crunchifyBuilder.toString());

        Iterator<String> keys = jsonObj.keys();

            while(keys.hasNext()) {
                String key = keys.next();

                JSONArray obj = (JSONArray) jsonObj.get(key);
                System.out.print("Here:" + obj.toString());
                for (int i = 0; i < obj.length(); i++) {
                    JSONObject json = obj.getJSONObject(i);
                    System.out.println("Here1:" + json.get("id").toString());
                    topics = mongo.deleteTopic(name, json.get("id").toString());


                }
            }



        return Response.ok("Topics " + jsonObj + " Deleted").build();
    }


    @POST
    @Path("/{param}/topics/changeroot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postChangeTopicRoot(@PathParam("param") String name, InputStream incomingData) {

        MongoDBHandler mongo = getMongoDBHandler();
        StringBuilder crunchifyBuilder = getJsonData(incomingData);

        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());

        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");
        Iterator<String> keys = jsonRqObj.keys();

        try {
            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");

            FindIterable<Document> runs = mongo.getTaxonomy(name);
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

            }
            JSONObject returnJson = new JSONObject();

            JSONArray returnJsonArray = new JSONArray();
            while(keys.hasNext()) {
                String key = keys.next();

                JSONArray obj = (JSONArray) jsonRqObj.get(key);
                for (int i = 0; i < obj.length(); i++) {
                    JSONObject json = obj.getJSONObject(i);
                    String topicString = json.get("id").toString();
                    String newTopicString = json.get("new_id").toString();
                    String newParentString = json.get("new_parent").toString();
                    String oldParentString = json.get("current_parent").toString();
                    if(!newParentString.equals(oldParentString)) {
                        Taxonomy topic = originalTaxo.descendent(topicString);
                        Taxonomy newParent = originalTaxo.descendent(newParentString);
                        newParent = newParent.addChild(topic, newParent);

                        finalTaxon = originalTaxo.deepCopyNewParent(topicString, newParentString, newParent);
                        finalTaxon.originalParent = oldParentString;
                        finalTaxon.originalTopic = topicString;

                        returnJson.put("id", name);
                        returnJson.put("success", true);
                        returnJson.put("new_parent", newParentString);
                        returnJsonArray.put(returnJson);
                    } else {
                        Taxonomy topic = originalTaxo.descendent(topicString);
                        topic.setRoot(newTopicString);
                        finalTaxon = originalTaxo.deepCopyNewTopic(topicString, newTopicString);
                        finalTaxon.originalParent = oldParentString;
                        finalTaxon.originalTopic = topicString;
                        mongo.updateTopicName(name, topicString, newTopicString, "accepted");
                        returnJson.put("id", name);
                        returnJson.put("success", true);
                        returnJson.put("new_id", newTopicString);
                        returnJsonArray.put(returnJson);
                    }


                }
            }
            mongo.updateTaxonomy(name, new Date(), finalTaxon);
            return Response.ok(returnJsonArray.toString()).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok(finalTaxon.toString()).build();

    }


    @POST
    @JSONP
    @Path("/{param}/topics/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateTopic(@PathParam("param") String name, InputStream incomingData) {

        MongoDBHandler mongo = getMongoDBHandler();
        StringBuilder crunchifyBuilder = getJsonData(incomingData);
        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");

        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());
        Iterator<String> keys = jsonRqObj.keys();

        try {

            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), "none");
            FindIterable<Document> runs = mongo.getTaxonomy(name);
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

            }

            while(keys.hasNext()) {
                String key = keys.next();

                JSONArray obj = (JSONArray) jsonRqObj.get(key);
                for (int i = 0; i < obj.length(); i++) {
                    JSONObject json = obj.getJSONObject(i);
                    String topicString = json.get("topic").toString();
                    String status = json.get("status").toString();
                    finalTaxon = originalTaxo.deepCopySetTopicStatus(topicString, status);

                    mongo.updateTopic(name, topicString, status);
                    mongo.updateTopicSimilarity(name, topicString, status);
                }
            }

            mongo.updateTaxonomy(name, new Date(), finalTaxon);



        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to update topic");
        }

        return Response.ok("Topics for run ID: " + name + " Updated").build();
    }


    @PUT
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putNewTopic(InputStream incomingData) {
        StringBuilder crunchifyBuilder = getJsonData(incomingData);
        return Response.ok("Topics " + crunchifyBuilder.toString() + " Deleted").build();
    }


    private StringBuilder getJsonData(InputStream incomingData) {
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


    @GET
    @Path("/{param}/authortopics/")
    public Response getAuthorTopics(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<AuthorTopicsResponse> topicsResponse = new ArrayList<>();
        AuthorsTopicsResponse returnEntity = new AuthorsTopicsResponse();
        try {
            runs = mongo.getAuthorTopics(name);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();
    }

    @GET
    @Path("/{param}/authortopics/{topic_id}")
    public Response getAuthorTopics(@PathParam("param") String name, @PathParam("topic_id") String topicId) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<AuthorTopicsResponse> topicsResponse = new ArrayList<>();
        AuthorsTopicsResponse returnEntity = new AuthorsTopicsResponse();
        try {
            runs = mongo.getAuthorTopicsForTopic(name, topicId);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();
    }


    @GET
    @Path("/{param}/authorsimilarity/")
    public Response getAuthorSimilarity(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<AuthorSimilarityResponse> topicsResponse = new ArrayList<>();
        AuthorsSimilarityResponse returnEntity = new AuthorsSimilarityResponse();
        try {
            runs = mongo.getAuthorSimilarity(name);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();


        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }


    @GET
    @Path("/{param}/authorsimilarity/{topic1}/{topic2}")
    public Response getAuthorSimilarityForTopics(@PathParam("param") String name, @PathParam("topic1") String topic1, @PathParam("topic2") String topic2) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<AuthorSimilarityResponse> topicsResponse = new ArrayList<>();
        AuthorsSimilarityResponse returnEntity = new AuthorsSimilarityResponse();
        try {
            runs = mongo.getAuthorSimilarityForTopic(name, topic1, topic2);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();


        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }


    @GET
    @Path("/{param}/topiccorrespondence/")
    public Response getTopicCorrespondence(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicCorrespondenceResponse> topicsResponse = new ArrayList<>();
        TopicsCorrespondenceResponse returnEntity = new TopicsCorrespondenceResponse();
        try {
            runs = mongo.getDocumentTopicCorrespondence(name);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }


    @GET
    @Path("/{param}/topiccorrespondence/{topic_id}")
    public Response getTopicCorrespondenceForTopic(@PathParam("param") String name, @PathParam("topic_id") String topicId) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicCorrespondenceResponse> topicsResponse = new ArrayList<>();
        TopicsCorrespondenceResponse returnEntity = new TopicsCorrespondenceResponse();
        try {
            runs = mongo.getDocumentTopicCorrespondenceForTopic(name, topicId);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }

    @GET
    @Path("/{param}/docs/{document_id}")
    public Response getTopicCorrespondenceForDocument(@PathParam("param") String name, @PathParam("document_id") String documentId) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicCorrespondenceResponse> topicsResponse = new ArrayList<>();
        TopicsCorrespondenceResponse returnEntity = new TopicsCorrespondenceResponse();
        try {
            runs = mongo.getDocumentTopicCorrespondenceForDocument(name, documentId);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }


    @GET
    @Path("/{param}/topicextraction/")
    public Response getTopicExtraction(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicExtractionResponse> topicsResponse = new ArrayList<>();
        TopicsExtractionResponse returnEntity = new TopicsExtractionResponse();
        try {
            runs = mongo.getTopicExtraction(name);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }



    @GET
    @Path("/{param}/topicextraction/{topic_id}")
    public Response getTopicExtractionForTopic(@PathParam("param") String name, @PathParam("topic_id") String topicId) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicExtractionResponse> topicsResponse = new ArrayList<>();
        TopicsExtractionResponse returnEntity = new TopicsExtractionResponse();
        try {
            runs = mongo.getTopicExtractionForTopic(name, topicId);
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
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }


    @GET
    @Path("/{param}/topicsimilarity/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopicSimilarity(@PathParam("param") String name) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicSimilarityResponse> topicsResponse = new ArrayList<>();
        TopicsSimilarityResponse returnEntity = new TopicsSimilarityResponse();
        try {
            runs = mongo.getTopicsSimilarity(name);

            for (Document doc : runs) {

                TopicSimilarityResponse entity = new TopicSimilarityResponse();
                System.out.println(doc);
                entity.setId(doc.get("_id").toString());
                entity.setRun(doc.getString("run"));
                entity.setRunDate(doc.getDate("run_date"));
                entity.setSimilarity(doc.getDouble("similarity"));
                entity.setTopicString1(doc.getString("topic1"));
                entity.setTopicString2(doc.getString("topic2"));

                topicsResponse.add(entity);
            }
            returnEntity.setTopics(topicsResponse);
            mongo.close();


            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }

    @GET
    @Path("/{param}/topicsimilarity/{topic1}/{topic2}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopicSimilarityBetweenTopics(@PathParam("param") String name, @PathParam("topic1") String topic1, @PathParam("topic2") String topic2) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicSimilarityResponse> topicsResponse = new ArrayList<>();
        TopicsSimilarityResponse returnEntity = new TopicsSimilarityResponse();
        try {
            runs = mongo.getTopicsSimilarityBetweenTopics(name, topic1, topic2);

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
            returnEntity.setTopics(topicsResponse);
            mongo.close();


            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }

    @GET
    @Path("/{param}/topicsimilarity/{topic}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopicSimilarityForTopic(@PathParam("param") String name, @PathParam("topic") String topic) {
        MongoDBHandler mongo = getMongoDBHandler();
        FindIterable<Document> runs;
        List<TopicSimilarityResponse> topicsResponse = new ArrayList<>();
        TopicsSimilarityResponse returnEntity = new TopicsSimilarityResponse();
        try {
            runs = mongo.getTopicsSimilarityForTopic(name, topic);

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
            returnEntity.setTopics(topicsResponse);
            mongo.close();


            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();


    }


}