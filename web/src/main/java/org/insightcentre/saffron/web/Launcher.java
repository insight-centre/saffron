package org.insightcentre.saffron.web;

import java.io.File;
import java.io.IOException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Launcher {

    

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
            Browser browser = new Browser(directory);
            Executor executor = new Executor(browser.saffron);
            Welcome welcome = new Welcome(browser.saffron, executor);
            handlers.setHandlers(new Handler[]{welcome, executor, browser, resourceHandler});
            server.setHandler(handlers);

            server.start();
            server.join();
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
