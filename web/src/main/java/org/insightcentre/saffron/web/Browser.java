package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import static java.lang.Integer.min;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.rdf.model.Model;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TaxonomyWithSize;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.taxonomy.supervised.AddSizesToTaxonomy;
import org.insightcentre.saffron.web.rdf.RDFConversion;

/**
 * Handles the interface if there is a corpus loaded
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Browser extends AbstractHandler {

    final Map<String, SaffronData> saffron = new HashMap<String, SaffronData>();

    public Browser(File dir) throws IOException {
        if (dir.exists()) {
            for (File subdir : dir.listFiles()) {
                if (subdir.exists() && subdir.isDirectory() && new File(subdir, "taxonomy.json").exists()) {
                    try {
                        SaffronData s2;
                        s2 = SaffronData.fromDirectory(subdir);
                        saffron.put(subdir.getName(), s2);
                    } catch (Exception x) {
                        x.printStackTrace();
                        System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
                    }
                }
            }
        }
    }

    @Override
    public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        if (target != null && target.startsWith("/") && target.lastIndexOf("/") != 0) {
            String name = target.substring(1, target.indexOf("/", 1));
            if (saffron.containsKey(name)) {
                handle2(target.substring(target.indexOf("/", 1)),
                        baseRequest, request, response, saffron.get(name), name);
            }
        }
    }

    public void handle2(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response,
            SaffronData saffron,
            String saffronDatasetName)
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
                } else if (target.equals("/taxonomy_with_size")) {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    Taxonomy taxonomy = saffron.getTaxonomy();
                    TaxonomyWithSize tws = AddSizesToTaxonomy.addSizes(taxonomy, saffron.getDocTopics());
                    mapper.writeValue(response.getWriter(), tws);
                } else if ("/parents".equals(target)) {
                    final String topic = request.getParameter("topic");
                    if (topic != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), saffron.getTaxoParents(topic));
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if ("/children".equals(target)) {
                    final String topic = request.getParameter("topic");
                    if (topic != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), saffron.getTaxoChildrenScored(topic));
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/author-sim")) {
                    final String author1 = request.getParameter("author1");
                    final String author2 = request.getParameter("author2");
                    final List<Author> aas;
                    if (author1 != null) {
                        aas = saffron.authorAuthorToAuthor2(saffron.getAuthorSimByAuthor1(author1));
                    } else if (author2 != null) {
                        aas = saffron.authorAuthorToAuthor1(saffron.getAuthorSimByAuthor2(author2));
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
                    final int n = request.getParameter("n") == null ? 20 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    final List<TopicTopic> tts;
                    if (topic1 != null) {
                        tts = saffron.getTopicByTopic1(topic1, saffron.getTaxoChildren(topic1));
                    } else if (topic2 != null) {
                        tts = saffron.getTopicByTopic2(topic2);
                    } else {
                        tts = null;
                    }
                    if (tts != null) {
                        List<TopicTopic> tts2 = getTopN(tts, n, offset);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), tts2);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/author-topics")) {
                    final String author = request.getParameter("author");
                    final String topic = request.getParameter("topic");
                    final int n = request.getParameter("n") == null ? 20 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    if (author != null) {
                        final List<AuthorTopic> ats = saffron.getTopicByAuthor(author);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), getTopNAuthorTopics(ats, n, offset));
                    } else if (topic != null) {
                        final List<Author> as = saffron.authorTopicsToAuthors(getTopNAuthorTopics(saffron.getAuthorByTopic(topic), n, offset));
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), as);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/doc-topics")) {
                    final String doc = request.getParameter("doc");
                    final String topic = request.getParameter("topic");
                    final int n = request.getParameter("n") == null ? 20 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    if (doc != null) {
                        final List<DocumentTopic> dts = saffron.getTopicByDoc(doc);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), getTopNDocTopics(dts, n, offset));

                    } else if (topic != null) {
                        final List<Document> _docs = saffron.getDocByTopic(topic);
                        final List<Document> docs = new ArrayList<>();
                        int i = 0;
                        for (Document d : _docs) {
                            if (i >= offset && i < offset + n) {
                                docs.add(d.reduceContext(topic, 20));
                            }
                            i++;
                        }
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
                    if (id != null) {
                        t = saffron.getTopic(id);
                    } else {
                        t = null;
                    }
                    if (t != null) {
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
                } else if (target.equals("/author-docs")) {
                    final String authorId = request.getParameter("author");
                    final int n = request.getParameter("n") == null ? 1000 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    if (authorId != null) {
                        List<Document> docs = saffron.getDocsByAuthor(authorId);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        if (offset < docs.size()) {
                            mapper.writeValue(response.getWriter(), docs.subList(offset, min(docs.size(), n + offset)));
                        } else {
                            try (Writer w = response.getWriter()) {
                                w.write("[]");
                            }
                        }
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), null);
                    }
                } else if (target.equals("/top-topics")) {
                    final int n = Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 20
                            : Integer.parseInt(request.getParameter("offset"));
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    mapper.writeValue(response.getWriter(), saffron.getTopTopics(n, offset + n));
                } else if (target.startsWith("/topic/")) {
                    final String topicString = decode(target.substring(7));
                    final Topic topic = saffron.getTopic(topicString);

                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/topic.html")));
                    if (topic != null) {
                        data = data.replaceAll("\\{\\{topic\\}\\}", mapper.writeValueAsString(topic));
                    } else {
                        data = data.replaceAll("\\{\\{topic\\}\\}", "{}");
                    }
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                } else if (target.startsWith("/edit")) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/edit.html")));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                } else if (target.startsWith("/author/")) {
                    final String authorString = decode(target.substring(8));
                    final Author author = saffron.getAuthor(authorString);
                    if (author != null) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/author.html")));
                        data = data.replaceAll("\\{\\{author\\}\\}", mapper.writeValueAsString(author));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/doc/")) {
                    final String docId = decode(target.substring(5));
                    final Document doc = saffron.getDoc(docId);
                    if (doc != null) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get("static/doc.html")));
                        data = data.replace("{{doc}}", mapper.writeValueAsString(doc));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/doc_content/")) {
                    final String docId = decode(target.substring(13));
                    final Document doc = saffron.getDoc(docId);
                    System.err.println(doc);
                    if (doc != null && doc.file != null) {
                        File f = doc.file.toFile();
                        response.setContentType(Files.probeContentType(f.toPath()));
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        try (InputStream reader = new FileInputStream(f)) {
                            try (OutputStream writer = response.getOutputStream()) {
                                byte[] buf = new byte[4096];
                                int i = 0;
                                while ((i = reader.read(buf)) >= 0) {
                                    writer.write(buf, 0, i);
                                }
                            }
                        }
                    } else if (doc != null) {
                        String contents = doc.contents();
                        response.setContentType("text/plain");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        try (Writer writer = response.getWriter()) {
                            writer.write(contents);
                        }
                    }
                } else if (target.equals("/status")) {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getWriter().print("{\"completed\":true}");
                } else if ("/".equals(target)) {

                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File("static/index.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));

                } else if ("/treemap.html".equals(target)) {

                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File("static/treemap.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
                } else if ("/graph.html".equals(target)) {

                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File("static/graph.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
                } else if ("/search/".equals(target)) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File("static/search.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
                } else if ("/search_results".equals(target)) {
                    String queryTerm = request.getParameter("query");
                    if (queryTerm != null) {
                        try {
                            Iterable<Document> docIterable = saffron.getSearcher().search(queryTerm);
                            ArrayList<Document> docs = new ArrayList<>();
                            for (Document doc : docIterable) {
                                docs.add(doc.reduceContext(queryTerm, 20));
                            }
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_OK);
                            PrintWriter out = response.getWriter();
                            mapper.writeValue(out, docs);
                            baseRequest.setHandled(true);
                        } catch (IOException x) {
                            x.printStackTrace();
                        }
                    }
                } else if (target.startsWith("/ttl/doc/")) {
                    final String docId = decode(target.substring(9));
                    final Document doc = saffron.getDoc(docId);
                    if (doc != null) {
                        response.setContentType("text/turtle");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.documentToRDF(doc, saffron);
                        model.write(response.getWriter(), "TURTLE");
                    }
                } else if (target.startsWith("/ttl/author/")) {
                    final String authorId = decode(target.substring(12));
                    final Author author = saffron.getAuthor(authorId);
                    if (author != null) {
                        response.setContentType("text/turtle");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.authorToRdf(author, saffron);
                        model.write(response.getWriter(), "TURTLE");
                    }
                } else if (target.startsWith("/ttl/topic/")) {
                    final String topicId = decode(target.substring(11));
                    final Topic topic = saffron.getTopic(topicId);
                    if (topic != null) {
                        response.setContentType("text/turtle");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.topicToRDF(topic, saffron);
                        model.write(response.getWriter(), "TURTLE");
                    }
                } else if (target.startsWith("/rdf/doc/")) {
                    final String docId = decode(target.substring(9));
                    final Document doc = saffron.getDoc(docId);
                    if (doc != null) {
                        response.setContentType("application/rdf+xml");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.documentToRDF(doc, saffron);
                        model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                    }
                } else if (target.startsWith("/rdf/author/")) {
                    final String authorId = decode(target.substring(12));
                    final Author author = saffron.getAuthor(authorId);
                    if (author != null) {
                        response.setContentType("application/rdf+xml");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.authorToRdf(author, saffron);
                        model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                    }
                } else if (target.startsWith("/rdf/topic/")) {
                    final String topicId = decode(target.substring(11));
                    final Topic topic = saffron.getTopic(topicId);
                    if (topic != null) {
                        response.setContentType("application/rdf+xml");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.topicToRDF(topic, saffron);
                        model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                    }
                } else if (target.equals("/download/rdf")) {
                    response.setContentType("application/rdf+xml");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    String base = getBase(request, "/download/rdf");
                    Model model = RDFConversion.allToRdf(base, saffron);
                    model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                } else if (target.equals("/download/ttl")) {
                    response.setContentType("text/turtle");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    String base = getBase(request, "/download/ttl");
                    Model model = RDFConversion.allToRdf(base, saffron);
                    model.write(response.getWriter(), "TURTLE");
                }

            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    private String getBase(HttpServletRequest req, String path) {
        StringBuffer sb = req.getRequestURL();
        sb.delete(sb.length() - path.length(), sb.length());
        return sb.toString();
    }
    
    private String decode(String id) {
        try {
            return URLDecoder.decode(id, "UTF-8");
        } catch(UnsupportedEncodingException x) {
            // silly Java, you could have used an enum here and not had to throw an exception....
            return id;
        }
    }
    
    private List<TopicTopic> getTopN(final List<TopicTopic> tts, final int n, final int offset) {
        tts.sort(new Comparator<TopicTopic>() {
            @Override
            public int compare(TopicTopic o1, TopicTopic o2) {
                if (o1.similarity > o2.similarity) {
                    return -1;
                } else if (o1.similarity < o2.similarity) {
                    return +1;
                } else {
                    int i = o1.topic1.compareTo(o2.topic1);
                    int j = o1.topic2.compareTo(o2.topic2);
                    return i != 0 ? i : j;
                }
            }
        });
        if (tts.size() < offset) {
            return Collections.EMPTY_LIST;
        } else if (tts.size() < offset + n) {
            return tts.subList(offset, tts.size());
        } else {
            return tts.subList(offset, offset + n);
        }
    }

    private List<DocumentTopic> getTopNDocTopics(final List<DocumentTopic> tts, final int n, final int offset) {
        tts.sort(new Comparator<DocumentTopic>() {
            @Override
            public int compare(DocumentTopic o1, DocumentTopic o2) {
                if (o1.occurrences > o2.occurrences) {
                    return -1;
                } else if (o1.occurrences < o2.occurrences) {
                    return +1;
                } else {
                    int i = o1.document_id.compareTo(o2.document_id);
                    int j = o1.topic_string.compareTo(o2.topic_string);
                    return i != 0 ? i : j;
                }
            }
        });
        if (tts.size() < offset) {
            return Collections.EMPTY_LIST;
        } else if (tts.size() < offset + n) {
            return tts.subList(offset, tts.size());
        } else {
            return tts.subList(offset, offset + n);
        }
    }

    private List<AuthorTopic> getTopNAuthorTopics(final List<AuthorTopic> tts, final int n, final int offset) {
        tts.sort(new Comparator<AuthorTopic>() {
            @Override
            public int compare(AuthorTopic o1, AuthorTopic o2) {
                if (o1.score > o2.score) {
                    return -1;
                } else if (o1.score < o2.score) {
                    return +1;
                } else {
                    int i = o1.author_id.compareTo(o2.author_id);
                    int j = o1.topic_id.compareTo(o2.topic_id);
                    return i != 0 ? i : j;
                }
            }
        });
        if (tts.size() < offset) {
            return Collections.EMPTY_LIST;
        } else if (tts.size() < offset + n) {
            return tts.subList(offset, tts.size());
        } else {
            return tts.subList(offset, offset + n);
        }
    }
}
