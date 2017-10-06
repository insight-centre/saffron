package org.insightcentre.saffron.web;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Executor extends AbstractHandler {
    
    private final SaffronData data;

    public Executor(SaffronData data) {
        this.data = data;
    }
    
    public boolean isExecuting() {
        return false;
    }

    @Override
    public void handle(String string, Request rqst, HttpServletRequest hsr, 
            HttpServletResponse hsr1) throws IOException, ServletException {
    }

    void startWithZip(File tmpFile) {
        System.err.println("Start with zip " + tmpFile);
    }

    void startWithCrawl(String url, int maxPages, boolean domain) {
        System.err.printf("Start with crawl %s %d %s\n", url, maxPages, domain);
    }

    void startWithJson(File tmpFile) {
        System.err.println("Start with json " + tmpFile);
    }
    
}
