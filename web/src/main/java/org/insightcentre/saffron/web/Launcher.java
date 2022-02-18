package org.insightcentre.saffron.web;

import java.awt.Desktop;
import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.URL;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.insightcentre.saffron.web.api.SaffronAPI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Launcher {

    public static Executor executor;
    public static Home home;
    public static final SaffronInMemoryDataSource saffron = new SaffronInMemoryDataSource();

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    private static void copy(ZipInputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    private static void copyFolder(Path source, Path target, CopyOption... options)
            throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path t = target.resolve(source.relativize(file));
                if(!Files.exists(t)) {
                    Files.copy(file, t, options);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final void createStaticResources(final File saffronHomeFile) 
            throws IOException {
        final File saffronWebHome = new File(saffronHomeFile, "web");
        CodeSource src = Launcher.class.getProtectionDomain().getCodeSource();
        if (src != null) {
            URL jar = src.getLocation();
            if(jar.toString().endsWith(".jar")) {
                new File(saffronHomeFile, "configs").mkdirs();
                new File(saffronWebHome, "static/css").mkdirs();
                new File(saffronWebHome, "static/images").mkdirs();
                new File(saffronWebHome, "static/js").mkdirs();
                new File(saffronWebHome, "static/vendors/angularjs/1.5.6").mkdirs();
                new File(saffronWebHome, "static/vendors/angular_material/1.1.12").mkdirs();
                new File(saffronWebHome, "static/vendors/angular_ui/0.13.3").mkdirs();
                new File(saffronWebHome, "static/vendors/boostrap").mkdirs();
                new File(saffronWebHome, "static/vendors/css").mkdirs();
                new File(saffronWebHome, "static/vendors/d3/3.5.17").mkdirs();
                new File(saffronWebHome, "static/vendors/iconfonts/mdi/css").mkdirs();
                new File(saffronWebHome, "static/vendors/iconfonts/mdi/fonts").mkdirs();
                new File(saffronWebHome, "static/vendors/iconfonts/mdi/scss").mkdirs();

                ZipInputStream zip = new ZipInputStream(jar.openStream());
                while (true) {
                    ZipEntry e = zip.getNextEntry();
                    if (e == null) {
                        break;
                    }
                    String name = e.getName();
                    if (name.startsWith("static/") && !name.endsWith(File.separator)) {
                        try(FileOutputStream fos = new FileOutputStream(new File(saffronWebHome, name))) {
                            copy(zip, fos);
                        }
                    }
                    if (name.startsWith("configs/") && !name.endsWith(File.separator)) {
                        try(FileOutputStream fos = new FileOutputStream(new File(saffronHomeFile, name))) {
                            copy(zip, fos);
                        }
                    }
                }
            } else if(jar.toString().endsWith(File.separator)) {
                new File("static").mkdirs();
                copyFolder(new File(new File(jar.getPath()), "static").toPath(),
                        new File(saffronWebHome, "static").toPath());
                new File("configs").mkdirs();
                copyFolder(new File(new File(jar.getPath()), "configs").toPath(),
                        new File(saffronHomeFile, "configs").toPath());
            }
        } else {
            System.err.println("Could not access static files");
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("d", "The directory containing the output or where to write the output to").withRequiredArg().ofType(File.class);
                    accepts("p", "The port to run on").withRequiredArg().ofType(Integer.class);
                    accepts("l", "The log file").withOptionalArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            final String saffronHome = System.getenv("SAFFRON_HOME");
            final File saffronHomeFile;
            if(saffronHome == null) {
                saffronHomeFile = new File(System.getProperty("user.home"), ".saffron");
                if(!saffronHomeFile.mkdirs()) {
                    System.err.println("Could not create Saffron home folder at ~/.saffron");
                }
            } else {
                saffronHomeFile = new File(saffronHome);
                if(!saffronHomeFile.exists() || !saffronHomeFile.isDirectory()) {
                    System.err.println(String.format("SAFFRON_HOME set to %s, but this does not exist or is not a directory", saffronHome));
                    System.exit(-1);
                }
            }
            System.setProperty("saffron.home", saffronHomeFile.getAbsolutePath());

            int port = os.valueOf("p") == null ? 8080 : (Integer) os.valueOf("p");
            File directory = (File) os.valueOf("d");
            if (directory == null) {
                directory = new File(new File(saffronHomeFile, "web"), "data");
            } else if (directory.exists() && !directory.isDirectory()) {
                badOptions(p, "The directory exists but is not a directory");
                return;
            }

            Server server = new Server(port);
            ResourceHandler resourceHandler = new ResourceHandler();

            // This is the path on the server
            // This is the local directory that is used to
            resourceHandler.setResourceBase(new File(new File(saffronHomeFile, "web"), "static").getAbsolutePath());
            System.err.println("Serving static resources from " + new File(new File(saffronHomeFile, "web"), "static").getAbsolutePath());
            if (!new File(new File(new File(saffronHomeFile, "web"), "static"), "index.html").exists()) {
                createStaticResources(saffronHomeFile);
            }
            //scontextHandler.setHandler(resourceHandler);
            HandlerList handlers = new HandlerList();
            Browser browser = new Browser(directory, saffron);
            executor = new Executor(saffron, directory, (File) os.valueOf("l"));
            NewRun welcome = new NewRun(executor);
            Home home = new Home(saffron, directory);
            SaffronAPI context = new SaffronAPI();
            handlers.setHandlers(new Handler[]{home, welcome, executor, browser, resourceHandler, context});
            server.setHandler(handlers);

            try {
                server.start();

            } catch (BindException x) {
                for (int i = port + 1; i < port + 20; i++) {
                    try {
                        server.stop();
                        System.err.println(String.format("##### WARNING: Could not bind at port %d, incrementing to %d #####", port, i));
                        server = new Server(i);
                        server.setHandler(handlers);
                        server.start();
                        port = i;
                        break;
                    } catch (BindException x2) {
                    }

                }
            }
            // Get current size of heap in bytes
            String hostname = InetAddress.getLocalHost().getHostAddress();
            System.err.println(String.format("Started server at http://localhost:%d/ (or http://%s:%d/)", port, hostname, port));
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(String.format("http://localhost:%d/", port)));
            }
            server.join();
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
