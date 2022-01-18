package org.insightcentre.saffron.web;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

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

            int port = os.valueOf("p") == null ? 8080 : (Integer) os.valueOf("p");
            File directory = (File) os.valueOf("d");
            if (directory == null) {
                // TODO: Change this
                directory = new File("data");
                //badOptions(p, "The directory was not specified");
                //return;
            } else if (directory.exists() && !directory.isDirectory()) {
                badOptions(p, "The directory exists but is not a directory");
                return;
            }

            Server server = new Server(port);
            ResourceHandler resourceHandler = new ResourceHandler();

            // This is the path on the server
            // This is the local directory that is used to
            resourceHandler.setResourceBase("static");
            if (!new File("static/index.html").exists()) {
                System.err.println("No static folder, please run the command in the right folder.");
                System.exit(-1);
            }
            //scontextHandler.setHandler(resourceHandler);
            HandlerList handlers = new HandlerList();
            Browser browser = new Browser(directory, saffron);
            executor = new Executor(saffron, directory, (File)os.valueOf("l"));
            NewRun welcome = new NewRun(executor);
            Home home = new Home(saffron, directory);
            handlers.setHandlers(new Handler[]{home, welcome, executor, browser, resourceHandler});
            server.setHandler(handlers);


            try {
                server.start();

            } catch(BindException x) {
                for(int i = port + 1; i < port + 20; i++) {
                    try {
                        server.stop();
                        System.err.println(String.format("##### WARNING: Could not bind at port %d, incrementing to %d #####", port, i));
                        server = new Server(i);
                        server.setHandler(handlers);
                        server.start();
                        port = i;
                        break;
                    } catch(BindException x2) {
                    }

                }
            }
            // Get current size of heap in bytes
            String hostname = InetAddress.getLocalHost().getHostAddress();
            System.err.println(String.format("Started server at http://localhost:%d/ (or http://%s:%d/)", port, hostname, port));
            server.join();
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }
}
