package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;

/**
 * Handles the interface if there is a corpus loaded
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Browser extends AbstractHandler {
    final SaffronData saffron;

    public Browser(File dir) throws IOException {
        if (dir.exists()) {
            SaffronData s2;
            try {
                s2 = SaffronData.fromDirectory(dir);
            } catch(Exception x) {
                x.printStackTrace();
                System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
                s2 = new SaffronData();
            }
            saffron = s2;
        } else {
            saffron = new SaffronData();
        }
    }

    @Override
    public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        try {
            // Exposing an existing directory
            if (saffron != null && saffron.isLoaded()) {
                final ObjectMapper mapper = new ObjectMapper();
                System.err.println(target);
                if (target.equals("/taxonomy")) {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    mapper.writeValue(response.getWriter(), saffron.getTaxonomy());
                } else if (target.equals("/author-sim")) {
                    final String author1 = request.getParameter("author1");
                    final String author2 = request.getParameter("author2");
                    final List<AuthorAuthor> aas;
                    if (author1 != null) {
                        aas = saffron.getAuthorSimByAuthor1(author1);
                    } else if (author2 != null) {
                        aas = saffron.getAuthorSimByAuthor2(author2);
                    } else {
                        aas = null;
                    }
                    if (aas != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), aas);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/topic-sim")) {
                    final String topic1 = request.getParameter("topic1");
                    final String topic2 = request.getParameter("topic2");
                    final List<TopicTopic> tts;
                    if(topic1 != null) {
                        tts = saffron.getTopicByTopic1(topic1);
                    } else if (topic2 != null) {
                        tts = saffron.getTopicByTopic2(topic2);
                    } else {
                        tts = null;
                    }
                    if(tts != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), tts);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/author-topics")) {
                    final String author = request.getParameter("author");
                    final String topic = request.getParameter("topic");
                    final List<AuthorTopic> ats;
                    if(author != null) {
                        ats = saffron.getTopicByAuthor(author);
                    } else if (topic != null) {
                        ats = saffron.getAuthorByTopic(topic);
                    } else {
                        ats = null;
                    }
                    if(ats != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), ats);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/doc-topics")) {
                    final String doc = request.getParameter("doc");
                    final String topic = request.getParameter("topic");
                    if(doc != null) {
                        final List<Topic> dts = saffron.getTopicByDoc(doc);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), dts);
                        
                    } else if(topic != null) {
                        final List<Document> docs = saffron.getDocByTopic(topic);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), docs);
                        
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/topics")) {
                    final String id = request.getParameter("id");
                    final Topic t;
                    if(id != null) {
                        t = saffron.getTopic(id);
                    } else {
                        t = null;
                    }
                    if(t != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), t);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), null);
                    }
                } else if (target.equals("/top-topics")) {
                    final int n = Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 20 :
                            Integer.parseInt(request.getParameter("offset"));
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    mapper.writeValue(response.getWriter(), saffron.getTopTopics(n, offset + n));
                } else if (target.startsWith("/topic/")) {
                    final String topicString = target.substring(7);
                    final Topic topic = saffron.getTopic(topicString);
                    if(topic != null) {
                        
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/topic.html")));
                        data = data.replaceAll("\\{\\{topic\\}\\}", mapper.writeValueAsString(topic));
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/author/")) {
                    final String authorString = target.substring(8);
                    final Author author = saffron.getAuthor(authorString);
                    if(author != null) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/author.html")));
                        data = data.replaceAll("\\{\\{author\\}\\}", mapper.writeValueAsString(author));
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/doc/")) {
                    final String docId = target.substring(5);
                    final Document doc = saffron.getDoc(docId);
                    if(doc != null) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/doc.html")));
                        data = data.replaceAll("\\{\\{doc\\}\\}", mapper.writeValueAsString(doc));
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/doc_content/")) {
                    final String docId = target.substring(13);
                    final Document doc = saffron.getDoc(docId);
                    if(doc != null) {
                        File f = doc.file;
                        response.setContentType(Files.probeContentType(f.toPath()));
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        FileReader reader = new FileReader(f);
                        Writer writer = response.getWriter();
                        char[] buf = new char[4096];
                        int i = 0;
                        while((i = reader.read(buf)) >= 0) {
                            writer.write(buf, 0, i);
                        }
                    }
                }
                
            } 
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }
}
