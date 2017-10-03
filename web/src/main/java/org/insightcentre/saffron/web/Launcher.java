package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Launcher extends AbstractHandler {

    private final File dir;
    private final SaffronData saffron;
    private final ResourceHandler staticHandler;

    public Launcher(File dir, ResourceHandler staticHandler) throws IOException {
        this.dir = dir;
        this.staticHandler = staticHandler;
        if (dir.exists()) {
            saffron = SaffronData.fromDirectory(dir);
        } else {
            dir.mkdirs();
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
            if (saffron.isLoaded()) {
                final ObjectMapper mapper = new ObjectMapper();
                System.err.println(target);
                if (request.getPathInfo().equals("/taxonomy")) {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    mapper.writeValue(response.getWriter(), saffron.getTaxonomy());
                } else if (request.getPathInfo().equals("/author-sim")) {
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
                } else if (request.getPathInfo().equals("/topic-sim")) {
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
                } else if (request.getPathInfo().equals("/author-topics")) {
                    final String author = request.getParameter("author");
                    final String topic = request.getParameter("topic2");
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
                } else if (request.getPathInfo().equals("/doc-topics")) {
                    final String doc = request.getParameter("doc");
                    final String topic = request.getParameter("topic");
                    final List<DocumentTopic> dts;
                    if(doc != null) {
                        dts = saffron.getTopicByDoc(doc);
                    } else if (topic != null) {
                        dts = saffron.getDocByTopic(topic);
                    } else {
                        dts = null;
                    }
                    if(dts != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), dts);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (request.getPathInfo().equals("/topics")) {
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
                }
                // Running a new Saffron instance
            } else {
                if(target.equals("/") || target.equals("")) {
                    baseRequest.setHandled(true);
                    response.sendRedirect("/welcome.html");
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("d", "The directory containing the output or where to write the output to").withRequiredArg().ofType(File.class);
                    accepts("p", "The port to run on").withRequiredArg().ofType(Integer.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            final int port = os.valueOf("p") == null ? 8080 : (Integer) os.valueOf("p");
            File directory = (File) os.valueOf("d");
            if (directory == null) {
                // TODO: Change this
                directory = new File("../tmp");
                //badOptions(p, "The directory was not specified");
                //return;
            } else if (directory.exists() && !directory.isDirectory()) {
                badOptions(p, "The directory exists but is not a directory");
                return;
            }

            Server server = new Server(8080);
            ResourceHandler resourceHandler = new ResourceHandler();
            //resourceHandler.setDirectoriesListed(true);
            resourceHandler.setWelcomeFiles(new String[]{"index.html"});

            // This is the path on the server
            //ContextHandler contextHandler = new ContextHandler("/static");
            // This is the local directory that is used to 
            resourceHandler.setResourceBase("static");
            //scontextHandler.setHandler(resourceHandler);
            HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[]{new Launcher(directory,resourceHandler), resourceHandler});
            server.setHandler(handlers);

            server.start();
            server.join();
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
