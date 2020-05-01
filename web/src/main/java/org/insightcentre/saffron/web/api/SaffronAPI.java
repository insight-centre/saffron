package org.insightcentre.saffron.web.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.glassfish.jersey.server.JSONP;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Partonomy;
import org.insightcentre.nlp.saffron.data.SaffronRun;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.saffron.web.Executor;
import org.insightcentre.saffron.web.Launcher;
import org.insightcentre.saffron.web.SaffronService;
import org.insightcentre.saffron.web.mongodb.MongoDBHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;

@Path("/api/v1/run")
public class SaffronAPI {

    private final org.insightcentre.saffron.web.api.APIUtils APIUtils = new APIUtils();
    protected final SaffronService saffronService;
    protected final MongoDBHandler saffron;
    protected final Launcher launcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SaffronAPI() {
        this.launcher = new Launcher();
        this.saffron = this.launcher.saffron;
        this.saffronService = new SaffronService(this.launcher.saffron);
    }


    @GET
    @JSONP
    @Path("/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRun(@PathParam("param") String name) {

        Taxonomy taxonomy;
        try {

            taxonomy = saffronService.getTaxonomy(name);
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
        List<SaffronRun> runs;
        String json;

        try {
            runs = saffronService.getAllRuns();

            for (SaffronRun doc : runs) {
                BaseResponse entity = new BaseResponse();
                entity.setId(doc.id);
                entity.setRunDate(doc.runDate);
                runsResponse.add(entity);
            }
            json = objectMapper.writeValueAsString(runsResponse);
        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
        }
        return Response.ok(json).build();
    }

    @DELETE
    @Path("/{param}")
    public Response deleteRun(@PathParam("param") String name) {
        saffronService.deleteRun(name);
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
    @Path("/{param}/terms")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRunTerms(@PathParam("param") String runId) {
        List<TermResponse> termsResponse = new ArrayList<>();
        String json;
        Iterable<Term> terms;
        
        try {
            terms = saffronService.getAllTerms(runId);

            for (Term doc: terms) {
                TermResponse entity = new TermResponse();
                entity.setId(doc.getString());
                entity.setMatches(doc.getMatches());
                entity.setOccurrences(doc.getOccurrences());
                entity.setScore(doc.getScore());
                entity.setTermString(doc.getString());
                entity.setStatus(doc.getStatus().toString());
                termsResponse.add(entity);
            }
            json = objectMapper.writeValueAsString(termsResponse);
        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
        }


        return Response.ok(json).build();
    }

    @GET
    @JSONP
    @Path("/{param}/search/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSearch(@PathParam("param") String runId, @PathParam("term") String term) {
        List<SearchResponse> searchResponses = new ArrayList<>();
        String json = null;
        FindIterable<Document> terms;
        
        try {
            terms = saffron.searchTaxonomy(runId, term);

            for (Document doc : terms) {
                SearchResponse entity = new SearchResponse();
                entity.setId(doc.getString("_id"));
                entity.setLocation(doc.getString("document_id"));
                entity.setTermString(doc.getString("term"));

                searchResponses.add(entity);

            }
            json = objectMapper.writeValueAsString(searchResponses);
        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok(json).build();
    }

    @GET
    @JSONP
    @Path("/{param}/terms/{term_id}/children")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermChildren(@PathParam("param") String runId, @PathParam("term_id") String termId) {
    	String json;
        try {

            Taxonomy originalTaxo = saffronService.getTaxonomy(runId);
            Taxonomy descendent = originalTaxo.descendent(termId);
            json = objectMapper.writeValueAsString(descendent);
            return Response.ok(json).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }

    }

    @GET
    @JSONP
    @Path("/{param}/terms/{term_id}/parent")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermParent(@PathParam("param") String runId, @PathParam("term_id") String termId) {
        String json;
        try {
            Taxonomy originalTaxo = saffronService.getTaxonomy(runId);
            Taxonomy antecendent = originalTaxo.antecendent(termId, "", originalTaxo, null);
            json = objectMapper.writeValueAsString(antecendent);
            return Response.ok(json).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }

    }



    @DELETE
    @JSONP
    @Path("/{param}/terms/{term_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response deleteTerm(@PathParam("param") String name,
            @PathParam("term_id") String termId) {

        saffronService.deleteTerm(name, termId);

        return Response.ok("Term " + name + " " + termId + " Deleted").build();
    }

    @POST
    @JSONP
    @Path("/{param}/terms/{term_id}/{term_id2}/{status}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response rejectTerm(@PathParam("param") String name,
            @PathParam("term_id") String termId,
            @PathParam("term_id") String termId2,
            @PathParam("status") String status) {

        
        Taxonomy finalTaxon = new Taxonomy("", 0.0, 0.0, new ArrayList<>(), Status.none);
        Taxonomy originalTaxo = saffronService.getTaxonomy(name);

        try {

            if (status.equals("rejected")) {
                finalTaxon = originalTaxo.deepCopySetTermStatus(termId, Status.rejected);
            } else if (status.equals("accepted")) {
                finalTaxon = originalTaxo.deepCopySetTermStatus(termId, Status.accepted);
            } else if (status.equals("none")) {
                finalTaxon = originalTaxo.deepCopySetTermStatus(termId, Status.none);
            }
            saffronService.updateTerm(name, termId, status);
            saffron.updateTermSimilarity(name, termId, termId2, status);
            saffronService.updateTaxonomy(name, finalTaxon);
            //saffron.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to reject the term " + termId + " from the taxonomy " + name);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();

        }
        return Response.ok("Term " + name + " " + termId + " Deleted").build();
    }

    @POST
    @Path("/{param}/terms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postDeleteManyTerms(@PathParam("param") String name, InputStream incomingData) {

        
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        FindIterable<Document> terms;
        JSONObject jsonObj = new JSONObject(crunchifyBuilder.toString());
        Iterator<String> keys = jsonObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray obj = (JSONArray) jsonObj.get(key);
            for (int i = 0; i < obj.length(); i++) {
                JSONObject json = obj.getJSONObject(i);
                saffronService.deleteTerm(name, json.get("id").toString());
            }
        }
        return Response.ok("Terms " + jsonObj + " Deleted").build();
    }

    @POST
    @Path("/{param}/terms/changeroot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postChangeTermRoot(@PathParam("param") String runId, InputStream incomingData) {

    	List<Pair<String,String>> childNewParentList = new ArrayList<Pair<String,String>>();
        
    	StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());

        Iterator<String> keys = jsonRqObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray obj = (JSONArray) jsonRqObj.get(key);
            for (int i = 0; i < obj.length(); i++) {
                JSONObject json = obj.getJSONObject(i);
                String termString = json.get("id").toString();
                String newParentString = json.get("new_parent").toString();
                //FIXME Current parent does not really matter
                String oldParentString = json.get("current_parent").toString();

                childNewParentList.add(new ImmutablePair<String,String>(termString, newParentString));
            }
        }

        try {
            saffronService.updateParent(runId, childNewParentList);
        } catch (Exception e) {
        	e.printStackTrace();
        	return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        //build answer
        return Response.ok("All parents successfully updated").build();
    }


    @POST
    @JSONP
    @Path("/{param}/terms/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateTerm(@PathParam("param") String runId, InputStream incomingData) {

    	/*
    	 * 1 - Read and validate JSON input
    	 * 2 - If everything is ok continue, otherwise send an error code
    	 * 3 - Ask a Saffron service to perform the status change (the REST controller should not know or care how the changes are made.
    	 * 4 - If everything is ok return an OK code, otherwise send an error code
    	*/

    	//1 - Read and validate JSON input
    	/*List<Term> terms = null;
		try {
			terms = Arrays.asList(new ObjectMapper().readValue(incomingData, Term[].class));
		} catch (Exception e) {
			//2 - If everything is ok continue, otherwise send an error code
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("input JSON format incorrect").build();
		}*/

    	List<Term> terms = new ArrayList<Term>();

    	StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
    	JSONObject jsonRqObj = new JSONObject(crunchifyBuilder.toString());
    	Iterator<String> keys = jsonRqObj.keys();
    	while (keys.hasNext()) {
    		String key = keys.next();

	    	JSONArray obj = (JSONArray) jsonRqObj.get(key);
	    	for (int i = 0; i < obj.length(); i++) {
	    		JSONObject json = obj.getJSONObject(i);
                String termString = json.get("term").toString();
                String status = json.get("status").toString();
                try {
                	terms.add(new Term.Builder(termString).status(Status.valueOf(status)).build());
                } catch (Exception e) {
        			//2 - If everything is ok continue, otherwise send an error code
        			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("input JSON format incorrect").build();
        		}
	    	}
    	}

		//3 - Ask a Saffron service to perform the status change (the REST controller should not know or care how/if changes are made).
    	try {
    		saffronService.updateTermStatus(runId, terms);
    	} catch (Exception e) {
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    	}

		//4 - If everything is ok return an OK code, otherwise send an error code
		return Response.ok("Terms for run ID: " + runId + " Updated").build();
    }

	@POST
    @JSONP
    @Path("/{param}/terms/updaterelationship")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateTermRelationship(@PathParam("param") String runId, InputStream incomingData) {
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
	                String termChild = json.get("term_child").toString();
	                //FIXME: getting the current parent is irrelevant, unless we are considering concurrent requests, which we are not
	                String status = json.get("status").toString();

	                parentChildStatusList.add(new ImmutablePair<String,String>(termChild, status));
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
        return Response.ok("Terms for run ID: " + runId + " Updated").build();
    }

    @PUT
    @Path("/{param}/terms/{term_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response putNewTerm(InputStream incomingData) {
        StringBuilder crunchifyBuilder = APIUtils.getJsonData(incomingData);
        return Response.ok("Terms " + crunchifyBuilder.toString() + " Deleted").build();
    }

    @GET
    @Path("/{param}/authorterms/{term_id}")
    public Response getAuthorTerms(@PathParam("param") String name, @PathParam("term_id") String termId) {
        String json;
        List<AuthorTermDAO> authors;
        try {
        	authors = saffronService.getAuthorsPerTermWithTfirf(name, termId);
            json = objectMapper.writeValueAsString(authors);
            return Response.ok(json).build();

        } catch (Exception x) {
            System.err.println("Failed to get authors for term '" + termId + "'");
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to get authors for term '" + termId + "'").build();
        }
    }
    
    @GET
    @Path("/{param}/termauthors/{author_id}")
    public Response getTermAuthors(@PathParam("param") String name, @PathParam("author_id") String authorId) {
        
        try {
            List<AuthorTerm> terms = saffronService.getAuthorTerms(name, authorId);
            String json = objectMapper.writeValueAsString(terms);
            return Response.ok(json).build();
        } catch(Exception x) {
            System.err.println("Failed to get terms for author '" + authorId + "'");
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to get terms for author '" + authorId + "'").build();
        }
    }
    
    @GET
    @Path("/{param}/authorauthors/{author_id}")
    public Response getAuthorAuthors(@PathParam("param") String runId, @PathParam("author_id") String authorId) {
        try {
            return Response.ok(objectMapper.writeValueAsString(saffronService.getAuthorSimilarity(runId, authorId))).build();
        } catch(Exception x) {
            System.err.println("Failed to get similar authors to '" + authorId + "'");
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to get similar authors to '" + authorId + "'").build();
        }
    }

    @GET
    @Path("/{param}/termcorrespondence/")
    public Response getTermCorrespondence(@PathParam("param") String name) {
        String json;
        FindIterable<Document> runs;
        List<TermCorrespondenceResponse> termsResponse = new ArrayList<>();
        TermsCorrespondenceResponse returnEntity = new TermsCorrespondenceResponse();
        try {
            runs = saffron.getDocumentTermCorrespondence(name);
            APIUtils.populateTermCorrespondenceResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @GET
    @Path("/{param}/termcorrespondence/{term_id}")
    public Response getTermCorrespondenceForTerm(@PathParam("param") String name, @PathParam("term_id") String termId) {
        String json;
        FindIterable<Document> runs;
        List<TermCorrespondenceResponse> termsResponse = new ArrayList<>();
        TermsCorrespondenceResponse returnEntity = new TermsCorrespondenceResponse();
        try {
            runs = saffron.getDocumentTermCorrespondenceForTerm(name, termId);
            APIUtils.populateTermCorrespondenceResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @GET
    @Path("/{param}/docs/{document_id}")
    public Response getTermCorrespondenceForDocument(@PathParam("param") String name, @PathParam("document_id") String documentId) {
        String json;
        FindIterable<Document> runs;
        List<TermCorrespondenceResponse> termsResponse = new ArrayList<>();
        TermsCorrespondenceResponse returnEntity = new TermsCorrespondenceResponse();
        
        try {
            runs = saffron.getDocumentTermCorrespondenceForDocument(name, documentId);
            APIUtils.populateTermCorrespondenceResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }




    @GET
    @Path("/{param}/termextraction/")
    public Response getTermExtraction(@PathParam("param") String name) {
        String json;
        FindIterable<Document> runs;
        List<TermExtractionResponse> termsResponse = new ArrayList<>();
        TermsExtractionResponse returnEntity = new TermsExtractionResponse();
        try {
            runs = saffron.getTermExtraction(name);
            APIUtils.populateTermExtractionResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @GET
    @Path("/{param}/termextraction/{term_id}")
    public Response getTermExtractionForTerm(@PathParam("param") String name, @PathParam("term_id") String termId) {
        String json;
        FindIterable<Document> runs;
        List<TermExtractionResponse> termsResponse = new ArrayList<>();
        TermsExtractionResponse returnEntity = new TermsExtractionResponse();
        try {
            runs = saffron.getTermExtractionForTerm(name, termId);
            APIUtils.populateTermExtractionResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @GET
    @Path("/{param}/termsimilarity/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermSimilarity(@PathParam("param") String name) {
        String json;
        FindIterable<Document> runs;
        List<TermSimilarityResponse> termsResponse = new ArrayList<>();
        TermsSimilarityResponse returnEntity = new TermsSimilarityResponse();
        try {
            runs = saffron.getTermsSimilarity(name);

            for (Document doc : runs) {

                TermSimilarityResponse entity = new TermSimilarityResponse();
                entity.setId(doc.get("_id").toString());
                entity.setRun(doc.getString("run"));
                entity.setRunDate(doc.getDate("run_date"));
                entity.setSimilarity(doc.getDouble("similarity"));
                entity.setTermString1(doc.getString("term1"));
                entity.setTermString2(doc.getString("term2"));

                termsResponse.add(entity);
            }
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @GET
    @Path("/{param}/termsimilarity/{term1}/{term2}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermSimilarityBetweenTerms(@PathParam("param") String name, @PathParam("term1") String term1, @PathParam("term2") String term2) {
        String json;
        FindIterable<Document> runs;
        List<TermSimilarityResponse> termsResponse = new ArrayList<>();
        TermsSimilarityResponse returnEntity = new TermsSimilarityResponse();
        try {
            runs = saffron.getTermsSimilarityBetweenTerms(name, term1, term2);

            APIUtils.populateTermSimilarityResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
        }

        return Response.ok("OK").build();

    }

    @GET
    @Path("/{param}/termsimilarity/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTermSimilarityForTerm(@PathParam("param") String name, @PathParam("term") String term) {
        String json;
        FindIterable<Document> runs;
        List<TermSimilarityResponse> termsResponse = new ArrayList<>();
        TermsSimilarityResponse returnEntity = new TermsSimilarityResponse();
        try {
            runs = saffron.getTermsSimilarityForTerm(name, term);
            APIUtils.populateTermSimilarityResp(runs, termsResponse);
            returnEntity.setTerms(termsResponse);
            json = objectMapper.writeValueAsString(returnEntity);
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
    @Path("/{param}/docs")
    public Response getOriginalDocuments(@PathParam("param") String name) {
        String json;
        HashMap<String, String> runs;
        String file = "";
        try {
            runs = saffron.getCorpusFiles(name);
            List <HashMap<String, String>> returnList = new ArrayList<>();
            runs.forEach((k,v)->{
                HashMap<String, String> item = new HashMap<>();
                item.put(k, v);
                returnList.add(item);
            });
            json = objectMapper.writeValueAsString(returnList);
            return Response.ok(json).build();

        } catch (Exception x) {
            x.printStackTrace();
            System.err.println("Failed to get documents from the existing data, this may be because a previous run failed");
        }

        return Response.ok(file).build();

    }

    @GET
    @Path("/{param}/docs/doc/{document_id}")
    public Response getOriginalDocument(@PathParam("param") String name, @PathParam("document_id") String documentId) {

        FindIterable<Document> runs;
        String file = "";
        
        try {
            runs = saffron.getCorpus(name);

            for (Document doc : runs) {
                List documents = (ArrayList)doc.get("documents");
                for (Object text : documents) {
                    String json = objectMapper.writeValueAsString(text);
                    JSONObject jsonObj = new JSONObject(json);
                    if (jsonObj.get("id").equals(documentId)) {
                        file = jsonObj.get("contents").toString();
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

    @GET
    @JSONP
    @Path("/partonomy/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPartonomy(@PathParam("param") String saffronDatasetName) {
        Partonomy partonomy;
        try {
            partonomy = saffronService.getPartonomy(saffronDatasetName);
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(partonomy);
            return Response.ok(jsonString).build();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
        }
    }

    @GET
    @JSONP
    @Path("/knowledgegraph/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKnowledgeGraph(@PathParam("param") String saffronDatasetName) {
        KnowledgeGraph knowledgeGraph;
        try {
            knowledgeGraph = saffronService.getKnowledgeGraph(saffronDatasetName);
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(knowledgeGraph);
            return Response.ok(jsonString).build();
        } catch (Exception x) {
            x.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to load Saffron from the existing data, this may be because a previous run failed").build();
        }
    }

    private Executor getExecutor() {
        return Launcher.executor;
    }
}
