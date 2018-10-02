package org.insightcentre.saffron.web;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * The home page of Saffron
 * @author John McCrae
 */
public class Home extends AbstractHandler {
    private final Map<String, SaffronData> sites;

    public Home(Map<String, SaffronData> sites) {
        this.sites = sites;
    }
    
    

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(target == null || "".equals(target) || "/".equals(target)) {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                FileReader reader = new FileReader(new File("static/home.html"));
                StringWriter writer = new StringWriter();
                char[] buf = new char[4096];
                int i = 0;
                while ((i = reader.read(buf)) >= 0) {
                    writer.write(buf, 0, i);
                }
                String content = writer.toString();
                StringBuilder sitesTxt = new StringBuilder();
                for(String s : sites.keySet()) {
                    if(sitesTxt.length() != 0) {
                        sitesTxt.append(",");
                    }
                    sitesTxt.append("\"").append(s).append("\"");
                }
                if(sites.isEmpty()) {
                    sitesTxt.append("<p>No previous analyses</p>");
                }
                content = content.replaceAll("\\{\\{sites\\}\\}", sitesTxt.toString());
                Writer out = response.getWriter();
                out.write(content);
        }
    }

}
