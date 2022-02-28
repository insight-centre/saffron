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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Partonomy;
import org.insightcentre.nlp.saffron.data.SaffronRun;
import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.topic.topicsim.TermSimilarity;
import org.insightcentre.saffron.web.Executor;
import org.insightcentre.saffron.web.Launcher;
import org.insightcentre.saffron.web.SaffronInMemoryDataSource;
import org.insightcentre.saffron.web.SaffronService;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;

import org.eclipse.jetty.server.handler.AbstractHandler;
import java.util.regex.*;
import org.eclipse.jetty.server.Request;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;


public class SaffronAPI extends AbstractHandler {
    private static final Pattern GET_ALL_RUNS = Pattern.compile("^/?api/v1/run/$");
    private static final Pattern GET_RUN = Pattern.compile("^/?api/v1/run/([^/]+)$");
    private static final Pattern RERUN = Pattern.compile("^/?api/v1/run/rerun/([^/]+)$");
    private static final Pattern TERMS = Pattern.compile("^/?api/v1/run/([^/]+)/terms$");
    private static final Pattern TERM_CHILDREN = Pattern.compile("^/?api/v1/run/([^/]+)/terms/([^/]+)/children$");
    private static final Pattern TERM_PARENT = Pattern.compile("^/?api/v1/run/([^/]+)/terms/([^/]+)/parent$");
    private static final Pattern AUTHOR_TERM = Pattern.compile("^/?api/v1/run/([^/]+)/authorterms/([^/]+)$");
    private static final Pattern TERM_AUTHOR = Pattern.compile("^/?api/v1/run/([^/]+)/termauthors/([^/]+)$");
    private static final Pattern AUTHOR_AUTHOR = Pattern.compile("^/?api/v1/run/([^/]+)/authorauthors/([^/]+)$");
    private static final Pattern TERM_SIM = Pattern.compile("^/?api/v1/run/([^/]+)/termsimilarity/$");
    private static final Pattern TERM_SIM2 = Pattern.compile("^/?api/v1/run/([^/]+)/termsimilarity/([^/]+)$");
    private static final Pattern DOC_TERM = Pattern.compile("^/?api/v1/run/([^/]+)/docs/term/([^/]+)$");

    @Override
    public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        try {
            Matcher m1 = GET_ALL_RUNS.matcher(target);
            Matcher m2 = GET_RUN.matcher(target);
            Matcher m3 = RERUN.matcher(target);
            Matcher m4 = TERMS.matcher(target);
            Matcher m5 = TERM_CHILDREN.matcher(target);
            Matcher m6 = TERM_PARENT.matcher(target);
            Matcher m7 = AUTHOR_TERM.matcher(target);
            Matcher m8 = TERM_AUTHOR.matcher(target);
            Matcher m9 = AUTHOR_AUTHOR.matcher(target);
            Matcher ma = TERM_SIM.matcher(target);
            Matcher mb = TERM_SIM2.matcher(target);
            Matcher mc = DOC_TERM.matcher(target);

            if(m1.matches() && request.getMethod().equals("GET")) {
                getAllRuns(response);
                baseRequest.setHandled(true);
            } else if(m2.matches() && request.getMethod().equals("GET")) {
                getRun(m2.group(1), response);
                baseRequest.setHandled(true);
            } else if(m2.matches() && request.getMethod().equals("DELETE")) {
                deleteRun(m2.group(1), response);
                baseRequest.setHandled(true);
            } else if(m3.matches() && request.getMethod().equals("POST")) {
                postRun(request.getInputStream(), m3.group(1), response);
                baseRequest.setHandled(true);
            } else if(m4.matches() && request.getMethod().equals("GET")) {
                getRunTerms(m4.group(1), response);
                baseRequest.setHandled(true);
            } else if(m5.matches() && request.getMethod().equals("GET")) {
                getTermChildren(m5.group(1), m5.group(2), response);
                baseRequest.setHandled(true);
            } else if(m6.matches() && request.getMethod().equals("GET")) {
                getTermParent(m6.group(1), m6.group(2), response);
                baseRequest.setHandled(true);
            } else if(m7.matches() && request.getMethod().equals("GET")) {
                getAuthorTerms(m7.group(1), m7.group(2), response);
                baseRequest.setHandled(true);
            } else if(m8.matches() && request.getMethod().equals("GET")) { 
                getTermAuthors(m8.group(1), m8.group(2), response);
                baseRequest.setHandled(true);
            } else if(m9.matches() && request.getMethod().equals("GET")) { 
                getAuthorAuthors(m9.group(1), m9.group(2), response);
                baseRequest.setHandled(true);
            } else if(ma.matches() && request.getMethod().equals("GET")) { 
                getTermSimilarity(ma.group(1), response);
                baseRequest.setHandled(true);
            } else if(mb.matches() && request.getMethod().equals("GET")) { 
                getTermSimilarityForTerm(mb.group(1), mb.group(2), response);
                baseRequest.setHandled(true);
            } else if(mc.matches() && request.getMethod().equals("GET")) { 
                int offset = request.getParameterMap().containsKey("offset") ?
                    Integer.parseInt(request.getParameterMap().get("offset")[0]) : -1;
                int n = request.getParameterMap().containsKey("n") ?
                    Integer.parseInt(request.getParameterMap().get("n")[0]) : 20;
                getDocumentsForTerm(mc.group(1), mc.group(2), offset, n, response);
                baseRequest.setHandled(true);
            }


        } catch(Throwable t) {
            t.printStackTrace();
        }
    }
 
    private final org.insightcentre.saffron.web.api.APIUtils APIUtils = new APIUtils();
    protected final SaffronService saffronService;
    protected final SaffronInMemoryDataSource saffron;
    protected final Launcher launcher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SaffronAPI() {
        this.launcher = new Launcher();
        this.saffron = this.launcher.saffron;
        this.saffronService = new SaffronService(this.launcher.saffron);
    }

    public void getRun(String name, HttpServletResponse response) throws IOException {

        Taxonomy taxonomy;
        try {

            taxonomy = saffron.getTaxonomy(name);
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(taxonomy);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(jsonString);
        } catch (Exception x) {
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR, 
                    "Failed to load Saffron from the existing data, this may be because a previous run failed");
        }
    }

    public void getAllRuns(HttpServletResponse response) throws IOException {
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
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                "Failed to load Saffron from the existing data, this may be because a previous run failed");
            return;
        }
        response.setStatus(SC_OK);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(json);
    }


    public void deleteRun(String name, HttpServletResponse response) throws IOException {
        saffronService.deleteRun(name);
        response.setStatus(SC_OK);
        response.getWriter().write("Run " + name + " Deleted");
    }

    public void postRun(InputStream incomingData, String name,
            HttpServletResponse response) throws IOException {
        BaseResponse resp = new BaseResponse();
        try {
            resp.setId(name);
            resp.setRunDate(new Date());

        } catch (Exception e) {
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                "This saffron object cannot be rerun");
            return;
        }

        response.setStatus(SC_OK);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(resp));
    }

    public void getRunTerms(String runId, HttpServletResponse response) throws IOException {
        List<TermResponse> termsResponse = new ArrayList<>();
        String json;
        Iterable<Term> terms;

        try {
            terms = saffronService.getAllTerms(runId);

            for (Term doc : terms) {
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
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Failed to load Saffron from the existing data, this may be because a previous run failed");
            return;
        }

        response.setStatus(SC_OK);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(json);
    }


    public void getTermChildren(String runId, 
            String termId, HttpServletResponse response) throws IOException {
        String json;
        try {

            Taxonomy originalTaxo = saffronService.getTaxonomy(runId);
            Taxonomy descendent = originalTaxo.descendent(termId);
            json = objectMapper.writeValueAsString(descendent);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Failed to load Saffron from the existing data, this may be because a previous run failed");

        }

    }

    public void getTermParent(String runId, 
            String termId, HttpServletResponse response) throws IOException {
        String json;
        try {
            Taxonomy originalTaxo = saffronService.getTaxonomy(runId);
            Taxonomy antecendent = originalTaxo.antecendent(termId, "", originalTaxo, null);
            json = objectMapper.writeValueAsString(antecendent);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Failed to load Saffron from the existing data, this may be because a previous run failed");

        }

    }

    public void getAuthorTerms(String name, String termId,
            HttpServletResponse response) throws IOException {
        String json;
        List<AuthorTermDAO> authors;
        try {
        	authors = saffronService.getAuthorsPerTermWithTfirf(name, termId);
            json = objectMapper.writeValueAsString(authors);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);

        } catch (Exception x) {
            System.err.println("Failed to get authors for term '" + termId + "'");
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Failed to get authors for term '" + termId + "'");
        }
    }

    public void getTermAuthors(String name, 
            String authorId,
            HttpServletResponse response) throws IOException {

        try {
            List<AuthorTerm> terms = saffronService.getAuthorTerms(name, authorId);
            String json = objectMapper.writeValueAsString(terms);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);
        } catch (Exception x) {
            System.err.println("Failed to get terms for author '" + authorId + "'");
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Failed to get terms for author '" + authorId + "'");
        }
    }

    public void getAuthorAuthors(String runId, String authorId,
            HttpServletResponse response) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(saffronService.getAuthorSimilarity(runId, authorId));
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);
        } catch (Exception x) {
            System.err.println("Failed to get similar authors to '" + authorId + "'");
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                "Failed to get similar authors to '" + authorId + "'");
        }
    }

    public void getTermSimilarity(String name,
            HttpServletResponse response) throws IOException {

        try {
            List<TermTerm> terms = saffron.getTermsSimilarity(name);
            String json = objectMapper.writeValueAsString(terms);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);

        } catch (Exception x) {
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                "Failed to load Saffron from the existing data, this may be because a previous run failed");
        }
    }

    public void getTermSimilarityForTerm(String name, String term,
            HttpServletResponse response) throws IOException {
        try {
            List<TermTerm> terms = saffron.getTermsSimilarity(name);
            List<TermSimilarityResponse> termsResponse = new ArrayList<>();
            for(TermTerm term1 : terms) {
                if(term1.getTerm1().equals(term)){
                    TermSimilarityResponse res = new TermSimilarityResponse();
                    res.setId(name);
                    res.setTermString1(term1.getTerm1());
                    res.setTermString2(term1.getTerm2());
                    res.setSimilarity(term1.getSimilarity());
                    res.setRun(name);
                    termsResponse.add(res);
                } else if (term1.getTerm2().equals(term)){
                    TermSimilarityResponse res = new TermSimilarityResponse();
                    res.setId(name);
                    res.setTermString1(term1.getTerm1());
                    res.setTermString2(term1.getTerm2());
                    res.setSimilarity(term1.getSimilarity());
                    res.setRun(name);
                    termsResponse.add(res);
                }
            }
            String json = objectMapper.writeValueAsString(termsResponse);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);

        } catch (Exception x) {
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                "Failed to load Saffron from the existing data, this may be because a previous run failed");
        }
    }

    public void getDocumentsForTerm(
    		String runId,
    		String termId,
    		int offsetStart,
    		int numberOfDocuments,
                HttpServletResponse response) throws IOException {

    	// Bad implementation of working with offset. Ideally it should work with offsets directly in the
    	// connection with the database
    	String json;
        List<org.insightcentre.nlp.saffron.data.Document> documents;
        try {
        	documents = saffronService.getDocumentsForTermWithReducedContext(runId, termId, 20);
        	if (offsetStart > -1) {
	        	if (offsetStart <= documents.size()-1) {
	        		if (offsetStart + numberOfDocuments <= documents.size() - 1) {
	        			documents = documents.subList(offsetStart, offsetStart+numberOfDocuments);
	        		} else {
	        			documents = documents.subList(offsetStart, documents.size() - 1);
	        		}
	        	} else {
	        		documents = new ArrayList<org.insightcentre.nlp.saffron.data.Document>();
	        	}
        	}
            json = objectMapper.writeValueAsString(documents);
            response.setStatus(SC_OK);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(json);

        } catch (Exception x) {
            System.err.println("Failed to get documents for term '" + termId + "'");
            x.printStackTrace();
            response.sendError(SC_INTERNAL_SERVER_ERROR,
                    "Failed to get documents for term '" + termId + "'");
        }
    }
}
