package org.insightcentre.saffron.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
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
public class NewRun extends AbstractHandler {

    private final Executor executor;

    public NewRun(Executor executor) {
        this.executor = executor;
    }

    private static boolean advanced(HttpServletRequest request) {
        String paramString = request.getParameter("advanced");
        return paramString != null;
    }

    private static boolean advanced(List<FileItem> items) {
        for(FileItem item : items) {
            if(item.isFormField() && item.getFieldName().equals("advanced"))
                return true;
        }
        return false;
    }

    private String saffronDatasetName(String saffronDatasetName,
            Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (saffronDatasetName != null && !"".equals(saffronDatasetName)
                && !"train".equals(saffronDatasetName)
                && !"execute".equals(saffronDatasetName)
                && !"new".equals(saffronDatasetName)
                && !"static".equals(saffronDatasetName)
                && saffronDatasetName.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            if (executor.newDataSet(saffronDatasetName)) {
                return saffronDatasetName;
            } else {
                baseRequest.setHandled(true);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Dataset name already exists");

            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No or Bad Dataset Name (Must be non-empty string matching [A-Za-z][A-Za-z0-9_-]* and not 'train', 'execute', 'static' or 'new' but was " + saffronDatasetName + ")");
        }
        return null;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        try {
            if ("/new".equals(target)) {
                response.setContentType("text/html");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                FileReader reader = new FileReader(new File("static/new.html"));
                StringWriter writer = new StringWriter();
                char[] buf = new char[4096];
                int i = 0;
                while ((i = reader.read(buf)) >= 0) {
                    writer.write(buf, 0, i);
                }
                String content = writer.toString().replaceAll("\\{\\{name\\}\\}", request.getParameter("name"));
                response.getWriter().write(content);
            } else if ("/new/zip".equals(target)) {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List<FileItem> items = upload.parseRequest(request);
                String saffronDatasetName = null;
                for (FileItem fi : items) {
                    if (fi.isFormField() && "saffronDatasetName".equals(fi.getFieldName())) {
                        saffronDatasetName = saffronDatasetName(fi.getString(), baseRequest, request, response);
                    }
                }
                if (saffronDatasetName == null) {
                    return;
                }
                for (FileItem fi : items) {
                    if (!fi.isFormField()) {
                        File tmpFile = File.createTempFile("corpus", items.get(0).getName());
                        tmpFile.deleteOnExit();
                        byte[] buf = new byte[4096];
                        try (InputStream is = fi.getInputStream(); FileOutputStream fos = new FileOutputStream(tmpFile)) {
                            int i = 0;
                            while ((i = is.read(buf)) >= 0) {
                                fos.write(buf, 0, i);
                            }
                        }
                        executor.startWithZip(tmpFile, advanced(items), saffronDatasetName);
                        baseRequest.setHandled(true);
                        response.sendRedirect("/execute?name=" + saffronDatasetName);
                        return;
                    }
                }
            } else if ("/new/crawl".equals(target)) {
                String saffronDatasetName = saffronDatasetName(request.getParameter("saffronDatasetName"), baseRequest, request, response);
                if (saffronDatasetName != null) {
                    String url = request.getParameter("url");
                    Integer maxPages = request.getParameter("max_pages") == null ? null
                            : Integer.parseInt(request.getParameter("max_pages"));
                    boolean domain = request.getParameter("domain") != null;
                    if (url != null && maxPages != null) {
                        executor.startWithCrawl(url, maxPages, domain, advanced(request), saffronDatasetName);
                        baseRequest.setHandled(true);
                        response.sendRedirect("/execute?name=" + saffronDatasetName);
                    }
                }
            } else if ("/new/json".equals(target)) {

                    DiskFileItemFactory factory = new DiskFileItemFactory();
                    ServletFileUpload upload = new ServletFileUpload(factory);
                    List<FileItem> items = upload.parseRequest(request);
                    
                String saffronDatasetName = null;
                for (FileItem fi : items) {
                    if (fi.isFormField()  && "saffronDatasetName".equals(fi.getFieldName())) {
                        saffronDatasetName = saffronDatasetName(fi.getString(), baseRequest, request, response);
                    }
                }
                if (saffronDatasetName == null) {
                    return;
                }
                for (FileItem fi : items) {
                    if (!fi.isFormField()) {
                        File tmpFile = File.createTempFile("corpus", ".json");
                        tmpFile.deleteOnExit();
                        byte[] buf = new byte[4096];
                        try (InputStream is = fi.getInputStream(); FileOutputStream fos = new FileOutputStream(tmpFile)) {
                            int i = 0;
                            while ((i = is.read(buf)) >= 0) {
                                fos.write(buf, 0, i);
                            }
                        }
                        executor.startWithJson(tmpFile, advanced(items), saffronDatasetName);
                        baseRequest.setHandled(true);
                        response.sendRedirect("/execute?name=" + saffronDatasetName);
                    }
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }
}
