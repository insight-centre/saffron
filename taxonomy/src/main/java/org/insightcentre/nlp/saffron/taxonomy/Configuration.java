package org.insightcentre.nlp.saffron.taxonomy;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Configuration {
    public File corpus;
    public File index;
    public File topics;
    public boolean reuseIndex = false;
    public double simThreshold = 0.5;
    public int spanSize = 5;
    public int minCommonDocs = 1;


    public Collection<File> loadCorpus() {
        return Arrays.asList(corpus.listFiles());
    }


}
