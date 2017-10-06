package org.insightcentre.saffron.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Welcome extends AbstractHandler {

    private final SaffronData data;
    private final Executor executor;

    public Welcome(SaffronData data, Executor executor) {
        this.data = data;
        this.executor = executor;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!data.isLoaded() && !executor.isExecuting()) {
            try {
                if (target == null || "/".equals(target) || "".equals(target)) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File("static/welcome.html"));
                    Writer writer = response.getWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                } else if ("/zip".equals(target)) {
                    DiskFileItemFactory factory = new DiskFileItemFactory();
                    ServletFileUpload upload = new ServletFileUpload(factory);
                    List<FileItem> items = upload.parseRequest(request);
                    if(items.size() == 1) {
                        File tmpFile = File.createTempFile("corpus", ".zip");
                        tmpFile.deleteOnExit();
                        byte[] buf = new byte[4096];
                        try (InputStream is = items.get(0).getInputStream(); FileOutputStream fos = new FileOutputStream(tmpFile)) {
                            int i = 0;
                            while((i = is.read(buf)) >= 0) {
                                fos.write(buf, 0, i);
                            }
                        }
                        executor.startWithZip(tmpFile);
                        baseRequest.setHandled(true);
                        response.sendRedirect("/");
                    }
                } else if("/crawl".equals(target)) {
                    String url = request.getParameter("url");
                    Integer maxPages = request.getParameter("max_pages") == null ? null :
                            Integer.parseInt(request.getParameter("max_pages"));
                    boolean domain = request.getParameter("domain") != null;
                    if(url != null && maxPages != null) {
                        executor.startWithCrawl(url, maxPages, domain);
                        baseRequest.setHandled(true);
                        response.sendRedirect("/");
                    }
                } else if("/json".equals(target)) {
                    DiskFileItemFactory factory = new DiskFileItemFactory();
                    ServletFileUpload upload = new ServletFileUpload(factory);
                    List<FileItem> items = upload.parseRequest(request);
                    if(items.size() == 1) {
                        File tmpFile = File.createTempFile("corpus", ".zip");
                        tmpFile.deleteOnExit();
                        byte[] buf = new byte[4096];
                        try (InputStream is = items.get(0).getInputStream(); FileOutputStream fos = new FileOutputStream(tmpFile)) {
                            int i = 0;
                            while((i = is.read(buf)) >= 0) {
                                fos.write(buf, 0, i);
                            }
                        }
                        executor.startWithJson(tmpFile);
                        baseRequest.setHandled(true);
                        response.sendRedirect("/");
                    }
                }
            } catch (Exception x) {
                x.printStackTrace();
                throw new ServletException(x);
            }
        }
    }

}
