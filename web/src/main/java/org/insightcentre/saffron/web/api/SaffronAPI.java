package org.insightcentre.saffron.web.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.glassfish.jersey.server.JSONP;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.saffron.web.*;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("/api/v1/run")
public class SaffronAPI{

    private final org.insightcentre.saffron.web.api.APIUtils APIUtils = new APIUtils();

    @GET
    @JSONP
    @Path("/{param}")
    @Produces(MediaType.APPLICATION_JSON)
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
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


            for (Document doc : runs) {
                BaseResponse entity = new BaseResponse();
                entity.setId(doc.getString("id"));
                entity.setRunDate(doc.getDate("run_date"));
                runsResponse.add(entity);
            }
            mongo.close();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
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
    @Path("/rerun/{param}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postRun(InputStream incomingData, @PathParam("param") String name) {
        BaseResponse resp = new BaseResponse();
        try {
            resp.setId(name);
            resp.setRunDate(new Date());

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("This saffron object cannot be rerun").build();
        }

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


            for (Document doc : topics) {
                TopicResponse entity = new TopicResponse();
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
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

            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);

            FindIterable<Document> runs = mongo.getTaxonomy(runId);
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

            }

            Taxonomy descendent = originalTaxo.descendent(topic_id);
            String json = new Gson().toJson(descendent);
            mongo.close();
            return Response.ok(json).build();


        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }

    }


    @GET
    @JSONP
    @Path("/{param}/topics/{topic_id}/parent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopicParent(@PathParam("param") String runId, @PathParam("topic_id") String topic_id) {
        MongoDBHandler mongo = getMongoDBHandler();


        try {
            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);

            FindIterable<Document> runs = mongo.getTaxonomy(runId);
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

            }
            Taxonomy antecendent = originalTaxo.antecendent(topic_id,"", originalTaxo, null);
            String json = new Gson().toJson(antecendent);
            mongo.close();
            return Response.ok(json).build();


        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }

    }

    private MongoDBHandler getMongoDBHandler() {
        String mongoUrl = System.getenv("MONGO_URL");
        String mongoPort = System.getenv("MONGO_PORT");
        String mongoDbName = System.getenv("MONGO_DB_NAME");
        return  new MongoDBHandler(mongoUrl, new Integer(mongoPort), mongoDbName, "saffron_runs");
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
    @Path("/{param}/topics/{topic_id}/{topic_id2}/{status}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response rejectTopic(@PathParam("param") String name,
                                @PathParam("topic_id") String topicId,
                                @PathParam("topic_id") String topic_id2,
                                @PathParam("status") String status) {

        MongoDBHandler mongo = getMongoDBHandler();
        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);
        FindIterable<Document> runs = mongo.getTaxonomy(name);

        try {
            for (org.bson.Document doc : runs) {
                JSONObject jsonObj = new JSONObject(doc.toJson());
                Taxonomy originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());

                if (status.equals("rejected")) {
                    finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, Status.rejected);
                } else if (status.equals("accepted")) {
                    finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, Status.accepted);
                } else if (status.equals("none")) {
                    finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, Status.none);
                }

                mongo.updateTopic(name, topicId, status);
                mongo.updateTopicSimilarity(name, topicId, topic_id2, status);
            }

            mongo.updateTaxonomy(name, new Date(), finalTaxon);
            mongo.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the topic " + topicId + " from the taxonomy " + name);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }
     return Response.ok("Topic " + name + " " + topicId + " Deleted").build();
    }


    @POST
    @Path("/{param}/topics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postDeleteManyTopics(@PathParam("param") String name, InputStream incomingData) {


        MongoDBHandler mongo = getMongoDBHandler();
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        FindIterable<Document> topics;

        JSONObject jsonObj = new JSONObject(crunchifyBuilder.toString());

        Iterator<String> keys = jsonObj.keys();

            while(keys.hasNext()) {
                String key = keys.next();

                JSONArray obj = (JSONArray) jsonObj.get(key);
                for (int i = 0; i < obj.length(); i++) {
                    JSONObject json = obj.getJSONObject(i);
                    mongo.deleteTopic(name, json.get("id").toString());


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
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);

        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());

        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);
        Iterator<String> keys = jsonRqObj.keys();

        try {
            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);

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
                        Taxonomy oldParent = originalTaxo.descendent(oldParentString);
                        oldParent.hasDescendent(newParentString);
                        if (oldParent.hasDescendentParent(newParentString)) {
                            return Response.status(Response.Status.BAD_REQUEST).entity("The selected move parent target is a member of a child topic and cannot be moved").build();
                        }
                        newParent = newParent.addChild(topic, newParent, oldParentString);
                        finalTaxon = originalTaxo.deepCopyNewParent(topicString, newParentString, topic, newParent);
                        finalTaxon = finalTaxon.deepCopyNewTaxo(newParentString, topic, finalTaxon);
                        returnJson.put("id", name);
                        returnJson.put("success", true);
                        returnJson.put("new_parent", newParentString);
                        returnJsonArray.put(returnJson);
                    }
                    else {
                        Taxonomy topic = originalTaxo.descendent(topicString);
                        topic.setRoot(newTopicString);
                        finalTaxon = originalTaxo.deepCopyNewTopic(topicString, newTopicString);
                        finalTaxon.originalParent = oldParentString;
                        finalTaxon.originalTopic = topicString;
                        mongo.updateTopicName(name, topicString, newTopicString, "accepted");
                        mongo.updateTopicSimilarityName(name, topicString, newTopicString, "accepted");
                        mongo.updateAuthorTopicName(name, topicString, newTopicString, "accepted");
                        mongo.updateDocumentTopicName(name, topicString, newTopicString, "accepted");
                        returnJson.put("id", name);
                        returnJson.put("success", true);
                        returnJson.put("new_id", newTopicString);
                        returnJsonArray.put(returnJson);
                    }


                }
            }
            mongo.updateTaxonomy(name, new Date(), finalTaxon);
            mongo.close();
            return Response.ok(returnJsonArray.toString()).build();

        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }

    }


    @POST
    @JSONP
    @Path("/{param}/topics/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateTopic(@PathParam("param") String name, InputStream incomingData) {

        MongoDBHandler mongo = getMongoDBHandler();
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);

        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());
        Iterator<String> keys = jsonRqObj.keys();

        try {

            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);
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

//
                    mongo.updateTopic(name, topicString, status);
                }
            }

            mongo.updateTaxonomy(name, new Date(), finalTaxon);
            mongo.close();


        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to update topic").build();

        }

        return Response.ok("Topics for run ID: " + name + " Updated").build();
    }


    @PUT
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putNewTopic(InputStream incomingData) {
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        return Response.ok("Topics " + crunchifyBuilder.toString() + " Deleted").build();
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
            APIUtils.populateAuthorTopicsResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            mongo.close();
            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to get author topics").build();
        }

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
            APIUtils.populateAuthorTopicsResp(runs, topicsResponse);
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
            APIUtils.populateAuthorSimilarityResponse(runs, topicsResponse);
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
            APIUtils.populateAuthorSimilarityResponse(runs, topicsResponse);
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
            APIUtils.populateTopicCorrespondenceResp(runs, topicsResponse);
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
            APIUtils.populateTopicCorrespondenceResp(runs, topicsResponse);
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
            APIUtils.populateTopicCorrespondenceResp(runs, topicsResponse);
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
            APIUtils.populateTopicExtractionResp(runs, topicsResponse);
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
            APIUtils.populateTopicExtractionResp(runs, topicsResponse);
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

            APIUtils.populateTopicSimilarityResp(runs, topicsResponse);
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

            APIUtils.populateTopicSimilarityResp(runs, topicsResponse);
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