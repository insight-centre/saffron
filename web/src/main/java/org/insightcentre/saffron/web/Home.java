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
    private final SaffronDataSource sites;
    private final File parentDirectory;

    public Home(SaffronDataSource sites, File parentDirectory) {
        this.sites = sites;
        this.parentDirectory = parentDirectory;
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
                for(String s : sites.runs()) {
                    if(sitesTxt.length() != 0) {
                        sitesTxt.append(",");
                    }
                    sitesTxt.append("\"").append(s).append("\"");
                }
                content = content.replaceAll("\\{\\{sites\\}\\}", sitesTxt.toString());
                Writer out = response.getWriter();
                out.write(content);
        } else if("/delete".equals(target)) {
            final String site = request.getQueryString();
            if(site != null && site.matches("[A-Za-z][A-Za-z0-9_-]*")) {
                deleteSite(site);
                response.setContentType("text/plain");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getWriter().println("OK");
            }
        }
    }

    public void deleteSite(String site) {
        File f = new File(parentDirectory, site);
        if(f.exists()) {
            delRecursive(f);
        }
        sites.remove(site);
    }

    private static void delRecursive(File f) {
        if(f.isDirectory()) {
            for(File f2 : f.listFiles()) {
                delRecursive(f2);
            }
        }
        f.delete();
    }
}
