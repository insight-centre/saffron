/*
 * TopicExtractionImpl.java, provides topic extraction as a Java program/API
 * Copyright (C) 2008  Alexander Schutz
 * National University of Ireland, Galway
 * Digital Enterprise Research Institute
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package org.insightcentre.nlp.saffron.topicextraction.topicextractor.gate;

import gate.Gate;
import gate.util.GateException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.insightcentre.nlp.saffron.topicextraction.data.DomainModel;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.TopicBearer;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.TopicExtractor;

public class TopicExtractorGate implements TopicExtractor {

    private GateProcessorPool gateProcessorPool = null;
    private boolean gateInitialised = false;
    private Logger logger = Logger.getLogger(TopicExtractorGate.class);

    private static TopicExtractorGate instance = null;

    public static TopicExtractorGate getInstance() throws GateException {
        if (instance == null) {
            instance = new TopicExtractorGate();
        }
        return instance;
    }

    private TopicExtractorGate() throws GateException {
        initGate();
    }

    public TopicBearer extractTopics(String documentText, DomainModel domainModel) {
        try {
            initGate();
            gate.Document gateDocument = null;

            gateDocument = gate.Factory.newDocument(documentText);

            return getTopics(gateDocument, domainModel);
        } catch (GateException x) {
            throw new RuntimeException(x);
        }
    }

    private TopicBearer getTopics(gate.Document gateDocument, DomainModel domainModel)
        throws GateException {
        TopicBearer response = null;
        if (gateDocument != null) {
            try {
                response = new GateProcessorConsumer(gateProcessorPool).process(gateDocument, domainModel);
            } catch (InterruptedException e) {
                throw new GateException(e);
            }
        }

        return response;
    }

    public void initGate() throws GateException {
        if (!gateInitialised) {
            logger.info("Initializing GATE");
            try {
                URI url = getClass().getResource("/gate").toURI();
                logger.info("URL to GATE resources: " + url);

                File gateHome;
                if (url.isOpaque()) {
                    /*
                     * GATE only has an interface for File objects, which means the files have to be on the filesystem.
                     * If we're running from a JAR we first have to extract the gate/ folder from the JAR. 
                     */

                    logger.info("Unpacking GATE resources from JAR");
                    String tempDirectoryPath = FileUtils.getTempDirectoryPath();
                    //Delete any existing directory
                    String gateResource = "gate";
                    FileUtils.deleteDirectory(new File(FilenameUtils.concat(tempDirectoryPath, gateResource)));
                    String gateHomePath = extractDirectoryFromClasspathJAR(getClass(), gateResource, tempDirectoryPath);
                    gateHome = new File(gateHomePath);

                } else {
                    gateHome = new File(url);
                }

                Gate.setGateHome(gateHome);
                Gate.setSiteConfigFile(new File(gateHome, "gate-site.xml"));
                Gate.setUserConfigFile(new File(gateHome, "gate-user.xml"));
                // Gate.setUserSessionFile(new File(gateHome, GATE_SESSION));

                Gate.setPluginsHome(new File(gateHome, "plugins/"));

                Gate.init();

                Iterator<URL> pluginItr = Gate.getKnownPlugins().iterator();
                while (pluginItr.hasNext()) {
                    URL pluginURL = pluginItr.next();
                    Gate.getCreoleRegister().registerDirectories(pluginURL);
                }

                logger.log(Level.INFO, "configuring GATE processor pool..");

                gateProcessorPool = GateProcessorPool.getInstance();

                gateInitialised = true;

            } catch (IOException | URISyntaxException | GateException exn) {
                throw new GateException("Exception while initializing GATE resources", exn);
            }
            logger.info("GATE Initialized");
        }
    }

    /**
     *
     * Extract a directory in a JAR on the classpath to an output folder.
     *
     * Note: User's responsibility to ensure that the files are actually in a
     * JAR.
     *
     * @param classInJar A class in the JAR file which is on the classpath
     * @param resourceDirectory Path to resource directory in JAR
     * @param outputDirectory Directory to write to
     * @return String containing the path to the outputDirectory
     * @throws IOException
     */
    private static String extractDirectoryFromClasspathJAR(Class<?> classInJar, String resourceDirectory, String outputDirectory)
        throws IOException {

        resourceDirectory = StringUtils.strip(resourceDirectory, "\\/") + File.separator;

        URL jar = classInJar.getProtectionDomain().getCodeSource().getLocation();
        JarFile jarFile = new JarFile(new File(jar.getFile()));

        byte[] buf = new byte[1024];
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            if (jarEntry.isDirectory() || !jarEntry.getName().startsWith(resourceDirectory)) {
                continue;
            }

            String outputFileName = FilenameUtils.concat(outputDirectory, jarEntry.getName());
            //Create directories if they don't exist
            new File(FilenameUtils.getFullPath(outputFileName)).mkdirs();

            //Write file
            FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);
            int n;
            InputStream is = jarFile.getInputStream(jarEntry);
            while ((n = is.read(buf, 0, 1024)) > -1) {
                fileOutputStream.write(buf, 0, n);
            }
            is.close();
            fileOutputStream.close();
        }
        jarFile.close();

        String fullPath = FilenameUtils.concat(outputDirectory, resourceDirectory);
        return fullPath;
    }
}
