package org.insightcentre.saffron.web.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.glassfish.jersey.server.JSONP;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.saffron.web.Executor;
import org.insightcentre.saffron.web.Launcher;
import org.insightcentre.saffron.web.SaffronService;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;

@Path("/api/v1/run")
public class SaffronAPI {

    private final org.insightcentre.saffron.web.api.APIUtils APIUtils = new APIUtils();
    
//    //FIXME The REST interface should not know or care about MongoDB configuration.
//    // This information should be restricted to Model layers dealing exclusively with MongoDB
//    static String mongoUrl = System.getenv("MONGO_URL");
//    static String mongoPort = System.getenv("MONGO_PORT");
//    static String mongoDbName = System.getenv("MONGO_DB_NAME");
//
//    protected MongoDBHandler saffron = new MongoDBHandler(
//            mongoUrl, new Integer(mongoPort), mongoDbName, "saffron_runs");
//
//
    protected final SaffronService saffronService;
    protected final MongoDBHandler saffron;
    protected final Launcher launcher;

    public SaffronAPI() {
        this.launcher = new Launcher();
        this.saffron = this.launcher.saffron;
        this.saffronService = new SaffronService(saffron);
    }


    @GET
    @JSONP
    @Path("/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRun(@PathParam("param") String name) {

        Taxonomy taxonomy;
        try {

            taxonomy = saffron.getTaxonomy(name);
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(taxonomy);
            return Response.ok(jsonString).build();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllRuns() {
        List<BaseResponse> runsResponse = new ArrayList<>();
        

        FindIterable<Document> runs;

        try {
            runs = saffron.getAllRuns();

            for (Document doc : runs) {
                BaseResponse entity = new BaseResponse();
                entity.setId(doc.getString("id"));
                entity.setRunDate(doc.getDate("run_date"));
                runsResponse.add(entity);
            }
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

        //SaffronData.fromMongo(name);
        
        saffron.deleteRun(name);
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

        FindIterable<Document> topics;
        
        try {
            topics = saffron.getTopics(runId);

            for (Document doc : topics) {
                TopicResponse entity = new TopicResponse();
                entity.setId(doc.getString("_id"));
                entity.setMatches(doc.getInteger("matches"));
                entity.setOccurrences(doc.getInteger("occurences"));
                entity.setScore(doc.getDouble("score"));
                entity.setTopicString(doc.getString("topic_string"));
                entity.setMvList((List<String>) doc.get("mvList"));
                entity.setStatus(doc.getString("status"));
                topicsResponse.add(entity);
            }

            //saffron.close();
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

        FindIterable<Document> topics;
        
        try {
            topics = saffron.searchTaxonomy(runId, term);

            for (Document doc : topics) {
                SearchResponse entity = new SearchResponse();
                entity.setId(doc.getString("_id"));
                entity.setLocation(doc.getString("document_id"));
                entity.setTopicString(doc.getString("topic"));

                searchResponses.add(entity);

            }

            //saffron.close();
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

        
        try {

            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);

            originalTaxo = saffron.getTaxonomy(runId);
//            for (org.bson.Document doc : runs) {
//                JSONObject jsonObj = new JSONObject(doc.toJson());
//                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());
//
//            }
            Taxonomy descendent = originalTaxo.descendent(topic_id);
            String json = new Gson().toJson(descendent);
            //saffron.close();
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


        try {
            Taxonomy originalTaxo = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);
            
            originalTaxo = saffron.getTaxonomy(runId);
//            for (org.bson.Document doc : runs) {
//                JSONObject jsonObj = new JSONObject(doc.toJson());
//                originalTaxo = Taxonomy.fromJsonString(jsonObj.toString());
//
//            }
            Taxonomy antecendent = originalTaxo.antecendent(topic_id, "", originalTaxo, null);
            Gson gson = new GsonBuilder().serializeNulls().serializeSpecialFloatingPointValues().create();
            String json = gson.toJson(antecendent);
            //saffron.close();
            return Response.ok(json).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }

    }



    @DELETE
    @JSONP
    @Path("/{param}/topics/{topic_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteTopic(@PathParam("param") String name,
            @PathParam("topic_id") String topicId) {


        List<TopicResponse> topicsResponse = new ArrayList<>();
        TopicsResponse resp = new TopicsResponse();
        FindIterable<Document> topics;
        
        saffron.deleteTopic(name, topicId);

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

        
        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, "", "", new ArrayList<>(), Status.none);
        Taxonomy originalTaxo = saffron.getTaxonomy(name);

        try {

            if (status.equals("rejected")) {
                finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, Status.rejected);
            } else if (status.equals("accepted")) {
                finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, Status.accepted);
            } else if (status.equals("none")) {
                finalTaxon = originalTaxo.deepCopySetTopicStatus(topicId, Status.none);
            }
            saffron.updateTopic(name, topicId, status);
            saffron.updateTopicSimilarity(name, topicId, topic_id2, status);
            saffron.updateTaxonomy(name, finalTaxon);
            //saffron.close();
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

        
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        FindIterable<Document> topics;
        JSONObject jsonObj = new JSONObject(crunchifyBuilder.toString());
        Iterator<String> keys = jsonObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray obj = (JSONArray) jsonObj.get(key);
            for (int i = 0; i < obj.length(); i++) {
                JSONObject json = obj.getJSONObject(i);
                saffron.deleteTopic(name, json.get("id").toString());
            }
        }
        return Response.ok("Topics " + jsonObj + " Deleted").build();
    }

    @POST
    @Path("/{param}/topics/changeroot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postChangeTopicRoot(@PathParam("param") String runId, InputStream incomingData) {

    	List<Pair<String,String>> childNewParentList = new ArrayList<Pair<String,String>>();
        
    	StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());

        Iterator<String> keys = jsonRqObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray obj = (JSONArray) jsonRqObj.get(key);
            for (int i = 0; i < obj.length(); i++) {
                JSONObject json = obj.getJSONObject(i);
                String topicString = json.get("id").toString();
                String newParentString = json.get("new_parent").toString();
                //FIXME Current parent does not really matter
                String oldParentString = json.get("current_parent").toString();

                childNewParentList.add(new ImmutablePair<String,String>(topicString, newParentString));
            }
        }

        try {
            saffronService.updateParent(runId, childNewParentList);
        } catch (Exception e) {
        	e.printStackTrace();
        	return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        //build answer
        /*JSONArray returnJsonArray = new JSONArray();
        JSONObject returnJson = new JSONObject();
        returnJson.put("id", name);
        returnJson.put("success", true);
        returnJson.put("new_parent", newParentString);
        returnJsonArray.put(returnJson);

        return Response.ok(returnJsonArray.toString()).build();*/
        return Response.ok("All parents successfully updated").build();
    }


    @POST
    @JSONP
    @Path("/{param}/topics/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateTopic(@PathParam("param") String runId, InputStream incomingData) {

    	/*
    	 * 1 - Read and validate JSON input
    	 * 2 - If everything is ok continue, otherwise send an error code
    	 * 3 - Ask a Saffron service to perform the status change (the REST controller should not know or care how the changes are made.
    	 * 4 - If everything is ok return an OK code, otherwise send an error code
    	*/

    	//1 - Read and validate JSON input
    	/*List<Topic> topics = null;
		try {
			topics = Arrays.asList(new ObjectMapper().readValue(incomingData, Topic[].class));
		} catch (Exception e) {
			//2 - If everything is ok continue, otherwise send an error code
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("input JSON format incorrect").build();
		}*/

    	List<Topic> topics = new ArrayList<Topic>();

    	StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
    	JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());
    	Iterator<String> keys = jsonRqObj.keys();
    	while (keys.hasNext()) {
    		String key = keys.next();

	    	JSONArray obj = (JSONArray) jsonRqObj.get(key);
	    	for (int i = 0; i < obj.length(); i++) {
	    		JSONObject json = obj.getJSONObject(i);
                String topicString = json.get("topic").toString();
                String status = json.get("status").toString();
                try {
                	topics.add(new Topic.Builder(topicString).status(Status.valueOf(status)).build());
                } catch (Exception e) {
        			//2 - If everything is ok continue, otherwise send an error code
        			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("input JSON format incorrect").build();
        		}
	    	}
    	}

		//3 - Ask a Saffron service to perform the status change (the REST controller should not know or care how/if changes are made).
    	try {
    		saffronService.updateTopicStatus(runId, topics);
    	} catch (Exception e) {
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    	}

		//4 - If everything is ok return an OK code, otherwise send an error code
		return Response.ok("Topics for run ID: " + runId + " Updated").build();
    }

	@POST
    @JSONP
    @Path("/{param}/topics/updaterelationship")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateTopicRelationship(@PathParam("param") String runId, InputStream incomingData) {
		/*
		 * 1 - Read and validate JSON input
		 * 2 - If everything is ok, continue, otherwise send a code error
		 * 3 - Ask a Saffron service to perform the relationship change (the REST controller should not know or care how the changes are made.)
		 * 4 - If everything is ok return an OK code, otherwise send an error code
		 */

		//1 - Read and validate JSON input

		List<Pair<String,String>> parentChildStatusList = new ArrayList<Pair<String,String>>();

		StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());

        Iterator<String> keys = jsonRqObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();

            try {
	            JSONArray obj = (JSONArray) jsonRqObj.get(key);
	            for (int i = 0; i < obj.length(); i++) {
	            	JSONObject json = obj.getJSONObject(i);
	                String topicChild = json.get("topic_child").toString();
	                //FIXME: getting the current parent is irrelevant, unless we are considering concurrent requests, which we are not
	                String status = json.get("status").toString();

	                parentChildStatusList.add(new ImmutablePair<String,String>(topicChild, status));
	            }
            } catch (Exception e) {
            	//2 - If everything is ok continue, otherwise send an error code
    			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("input JSON format incorrect").build();
            }
        }

        //3 - Ask a Saffron service to perform the relationship change (the REST controller should not know or care how/if changes are made).
    	try {
    		saffronService.updateParentRelationshipStatus(runId, parentChildStatusList);
    	} catch (Exception e) {
    		e.printStackTrace();
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    	}

    	//4 - If everything is ok return an OK code, otherwise send an error code
        return Response.ok("Topics for run ID: " + runId + " Updated").build();
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

        FindIterable<Document> runs;
        
        List<AuthorTopicsResponse> topicsResponse = new ArrayList<>();
        AuthorsTopicsResponse returnEntity = new AuthorsTopicsResponse();
        try {
            runs = saffron.getAuthorTopics(name);
            APIUtils.populateAuthorTopicsResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<AuthorTopicsResponse> topicsResponse = new ArrayList<>();
        AuthorsTopicsResponse returnEntity = new AuthorsTopicsResponse();
        
        try {
            runs = saffron.getAuthorTopicsForTopic(name, topicId);
            APIUtils.populateAuthorTopicsResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<AuthorSimilarityResponse> topicsResponse = new ArrayList<>();
        AuthorsSimilarityResponse returnEntity = new AuthorsSimilarityResponse();
        
        try {
            runs = saffron.getAuthorSimilarity(name);
            APIUtils.populateAuthorSimilarityResponse(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<AuthorSimilarityResponse> topicsResponse = new ArrayList<>();
        AuthorsSimilarityResponse returnEntity = new AuthorsSimilarityResponse();
        
        try {
            runs = saffron.getAuthorSimilarityForTopic(name, topic1, topic2);
            APIUtils.populateAuthorSimilarityResponse(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<TopicCorrespondenceResponse> topicsResponse = new ArrayList<>();
        TopicsCorrespondenceResponse returnEntity = new TopicsCorrespondenceResponse();
        
        try {
            runs = saffron.getDocumentTopicCorrespondence(name);
            APIUtils.populateTopicCorrespondenceResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<TopicCorrespondenceResponse> topicsResponse = new ArrayList<>();
        TopicsCorrespondenceResponse returnEntity = new TopicsCorrespondenceResponse();
        
        try {
            runs = saffron.getDocumentTopicCorrespondenceForTopic(name, topicId);
            APIUtils.populateTopicCorrespondenceResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<TopicCorrespondenceResponse> topicsResponse = new ArrayList<>();
        TopicsCorrespondenceResponse returnEntity = new TopicsCorrespondenceResponse();
        
        try {
            runs = saffron.getDocumentTopicCorrespondenceForDocument(name, documentId);
            APIUtils.populateTopicCorrespondenceResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<TopicExtractionResponse> topicsResponse = new ArrayList<>();
        TopicsExtractionResponse returnEntity = new TopicsExtractionResponse();
        
        try {
            runs = saffron.getTopicExtraction(name);
            APIUtils.populateTopicExtractionResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<TopicExtractionResponse> topicsResponse = new ArrayList<>();
        TopicsExtractionResponse returnEntity = new TopicsExtractionResponse();
        
        try {
            runs = saffron.getTopicExtractionForTopic(name, topicId);
            APIUtils.populateTopicExtractionResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();
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

        FindIterable<Document> runs;
        List<TopicSimilarityResponse> topicsResponse = new ArrayList<>();
        TopicsSimilarityResponse returnEntity = new TopicsSimilarityResponse();
        
        try {
            runs = saffron.getTopicsSimilarity(name);

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
            //saffron.close();

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

        FindIterable<Document> runs;
        List<TopicSimilarityResponse> topicsResponse = new ArrayList<>();
        TopicsSimilarityResponse returnEntity = new TopicsSimilarityResponse();
        
        try {
            runs = saffron.getTopicsSimilarityBetweenTopics(name, topic1, topic2);

            APIUtils.populateTopicSimilarityResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();

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
        FindIterable<Document> runs;
        List<TopicSimilarityResponse> topicsResponse = new ArrayList<>();
        TopicsSimilarityResponse returnEntity = new TopicsSimilarityResponse();
        
        try {
            runs = saffron.getTopicsSimilarityForTopic(name, topic);

            APIUtils.populateTopicSimilarityResp(runs, topicsResponse);
            returnEntity.setTopics(topicsResponse);
            //saffron.close();

            String json = new Gson().toJson(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @POST
    @Path("/new/zip/{saffronDatasetName}")
    @Consumes("*/*")
    public Response startWithZip(@PathParam("saffronDatasetName") String saffronDatasetName, InputStream inputStream) throws IOException {
        if (saffronDatasetName.matches("[A-Za-z][A-Za-z0-9_-]*") && getExecutor().newDataSet(saffronDatasetName)) {

            File tmpFile = File.createTempFile("corpus", ".zip");
            tmpFile.deleteOnExit();
            byte[] buf = new byte[4096];
            try (InputStream is = inputStream; FileOutputStream fos = new FileOutputStream(tmpFile)) {
                int i = 0;
                while ((i = is.read(buf)) >= 0) {

                    fos.write(buf, 0, i);
                }
            }
            getExecutor().startWithZip(tmpFile, false, saffronDatasetName);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad dataset name or run already exits").build();
        }
    }

    @POST
    @Path("/new/json/{saffronDatasetName}")
    public Response startWithJson(@PathParam("saffronDatasetName") String saffronDatasetName, InputStream inputStream) throws IOException {
        if (saffronDatasetName.matches("[A-Za-z][A-Za-z0-9_-]*") && getExecutor().newDataSet(saffronDatasetName)) {

            File tmpFile = File.createTempFile("corpus", ".json");
            tmpFile.deleteOnExit();
            byte[] buf = new byte[4096];
            try (InputStream is = inputStream; FileOutputStream fos = new FileOutputStream(tmpFile)) {
                int i = 0;
                while ((i = is.read(buf)) >= 0) {
                    fos.write(buf, 0, i);
                }
            }
            getExecutor().startWithJson(tmpFile, false, saffronDatasetName);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad dataset name or run already exits").build();
        }
    }


    @GET
    @Path("/{param}/docs/doc/{document_id}")
    public Response getOriginalDocument(@PathParam("param") String name, @PathParam("document_id") String documentId) {

        FindIterable<Document> runs;
        String file = "";
        
        try {
            runs = saffron.getCorpus(name);

            for (Document doc : runs) {

                System.out.println(documentId);
                String id = doc.get("id").toString();
                List documents = (ArrayList)doc.get("documents");
                System.out.println(documents.size());
                for (Object text : documents) {
                    String json = new Gson().toJson(text);

                    JSONObject jsonObj = new JSONObject(json);
                    System.out.println(jsonObj.get("name"));
                    if (jsonObj.get("id").equals(documentId)) {
                        file = jsonObj.get("contents").toString();
                        System.out.println(file);
                    }
                }


            }

            return Response.ok(file).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok(file).build();

    }

    @GET
    @Path("/new/crawl/{saffronDatasetName}")
    public Response startWithCrawl(@PathParam("saffronDatasetName") String saffronDatasetName,
            @QueryParam("url") String url, @QueryParam("max_pages") int maxPages,
            @DefaultValue("true") @QueryParam("domain") boolean domain) throws IOException {
        if (saffronDatasetName.matches("[A-Za-z][A-Za-z0-9_-]*") && getExecutor().newDataSet(saffronDatasetName)) {
            getExecutor().startWithCrawl(url, maxPages, domain, false, saffronDatasetName);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Bad dataset name or run already exits").build();
        }
    }

    @GET
    @JSONP
    @Path("/status/{saffronDatasetName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeStatus(@PathParam("saffronDatasetName") String saffronDatasetName) throws Exception {
        try {
            Executor.Status status = getExecutor().getStatus(saffronDatasetName);
            if (status != null) {
                return Response.ok(new ObjectMapper().writeValueAsString(status)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw x;
        }

    }

    private Executor getExecutor() {
        return Launcher.executor;
    }
}
