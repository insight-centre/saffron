package org.insightcentre.nlp.saffron.topic.gate;

import gate.Gate;
import gate.util.GateException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.insightcentre.nlp.saffron.topic.ExtractedTopic;
import org.insightcentre.nlp.saffron.topic.TopicExtractor;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TopicExtractorGate implements TopicExtractor {
    private final File gateHome;
    private GateProcessor proc = null;

    public TopicExtractorGate(File gateHome) {
        this.gateHome = gateHome;
    }

    private List<ExtractedTopic> getTopics(gate.Document gateDocument, List<String> domainModel)
        throws GateException {
        if(proc == null) {
            proc = new GateProcessor();
        }
        List<ExtractedTopic> response = null;
        if (gateDocument != null) {
            try {
                response = new GateProcessorConsumer(proc).process(gateDocument, domainModel);
            } catch (InterruptedException e) {
                throw new GateException(e);
            }
        }

        return response;
    }

 
    @Override
    public List<ExtractedTopic> extractTopics(String documentText, List<String> domainModel) {
        try {
            initGate();
            gate.Document gateDocument = null;

            gateDocument = gate.Factory.newDocument(documentText);

            return getTopics(gateDocument, domainModel);
        } catch (GateException x) {
            throw new RuntimeException(x);
        }
    }

    private static boolean gateInitialised;
    public void initGate() throws GateException {
        if (!gateInitialised) {
            if(!gateHome.exists() || !gateHome.isDirectory()) {
                throw new RuntimeException(gateHome.getAbsolutePath() + " does not exist, please install GATE to this path");
            }
            Gate.setGateHome(gateHome);
            File pluginsHome = new File(gateHome, "plugins");
            if(!pluginsHome.exists() || !pluginsHome.isDirectory()) { 
                throw new RuntimeException(pluginsHome.getAbsolutePath() + " does not exist");
            }
            Gate.setPluginsHome(pluginsHome);

            File annieHome = new File(pluginsHome, "ANNIE");
            if(!annieHome.exists() || !annieHome.isDirectory()) { 
                throw new RuntimeException(annieHome.getAbsolutePath() + " does not exist");
            }
            File toolsHome = new File(pluginsHome, "Tools");
            if(!toolsHome.exists() || !toolsHome.isDirectory()) { 
                throw new RuntimeException(toolsHome.getAbsolutePath() + " does not exist");
            }

            Gate.init();

            try {
                Gate.getCreoleRegister().registerDirectories(annieHome.toURI().toURL());
                Gate.getCreoleRegister().registerDirectories(toolsHome.toURI().toURL());
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
            
            gateInitialised = true;
        }
    }
//
//    /**
//     *
//     * Extract a directory in a JAR on the classpath to an output folder.
//     *
//     * Note: User's responsibility to ensure that the files are actually in a
//     * JAR.
//     *
//     * @param classInJar A class in the JAR file which is on the classpath
//     * @param resourceDirectory Path to resource directory in JAR
//     * @param outputDirectory Directory to write to
//     * @return String containing the path to the outputDirectory
//     * @throws IOException
//     */
//    private static String extractDirectoryFromClasspathJAR(Class<?> classInJar, String resourceDirectory, String outputDirectory)
//        throws IOException {
//
//        resourceDirectory = StringUtils.strip(resourceDirectory, "\\/") + File.separator;
//
//        URL jar = classInJar.getProtectionDomain().getCodeSource().getLocation();
//        File f = new File(jar.getFile());
//        if(f.isDirectory()) {
//            for(File jarEntry : FileUtils.listFiles(f, null, true)) {
//                if (jarEntry.isDirectory() || !jarEntry.getName().startsWith(resourceDirectory)) {
//                    continue;
//                }
//
//                String outputFileName = FilenameUtils.concat(outputDirectory, jarEntry.getName());
//                //Create directories if they don't exist
//                File outputFile = new File(FilenameUtils.getFullPath(outputFileName));
//                outputFile.mkdirs();
//
//                FileUtils.copyFile(jarEntry, outputFile);
// 
//            }
//        } else {
//            JarFile jarFile = new JarFile(f);
//
//            byte[] buf = new byte[1024];
//            Enumeration<JarEntry> jarEntries = jarFile.entries();
//            while (jarEntries.hasMoreElements()) {
//                JarEntry jarEntry = jarEntries.nextElement();
//                if (jarEntry.isDirectory() || !jarEntry.getName().startsWith(resourceDirectory)) {
//                    continue;
//                }
//
//                String outputFileName = FilenameUtils.concat(outputDirectory, jarEntry.getName());
//                //Create directories if they don't exist
//                new File(FilenameUtils.getFullPath(outputFileName)).mkdirs();
//
//                //Write file
//                FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);
//                int n;
//                InputStream is = jarFile.getInputStream(jarEntry);
//                while ((n = is.read(buf, 0, 1024)) > -1) {
//                    fileOutputStream.write(buf, 0, n);
//                }
//                is.close();
//                fileOutputStream.close();
//            }
//            jarFile.close();
//
//        }
//        String fullPath = FilenameUtils.concat(outputDirectory, resourceDirectory);
//        return fullPath;
//    }
//

}
