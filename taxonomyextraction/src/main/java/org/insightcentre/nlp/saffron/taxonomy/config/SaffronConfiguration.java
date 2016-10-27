package org.insightcentre.nlp.saffron.taxonomy.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;

/**
 * The configuration for a saffron instance
 * @author John McCrae <john@mccr.ae>
 */
public class SaffronConfiguration {
    public final File outputPath, indexPath;

    public final boolean usePreviousCalc;

    public final String jdbcUrl, dbUser, dbPass;

    public final int numTopics;

    public final double simThreshold;

    public final int spanSize;

    public final int minCommonDocs;

    @JsonCreator public SaffronConfiguration(@JsonProperty(value="outputPath",required=true) File outputPath, 
                                             @JsonProperty(value="indexPath",required=true) File indexPath, 
                                             @JsonProperty(value="usePreviousCalc",required=true) boolean usePreviousCalc, 
                                             @JsonProperty(value="jdbcUrl",required=true) String jdbcUrl, 
                                             @JsonProperty(value="dbUser") String dbUser, 
                                             @JsonProperty(value="dbPass") String dbPass, 
                                             @JsonProperty(value="numTopics",required=true) int numTopics, 
                                             @JsonProperty(value="simThreshold",required=true) double simThreshold, 
                                             @JsonProperty(value="spanSize",required=true) int spanSize, 
                                             @JsonProperty(value="minCommonDocs",required=true) int minCommonDocs) {
        this.outputPath = outputPath;
        this.indexPath = indexPath;
        this.usePreviousCalc = usePreviousCalc;
        this.jdbcUrl = jdbcUrl;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.numTopics = numTopics;
        this.simThreshold = simThreshold;
        this.spanSize = spanSize;
        this.minCommonDocs = minCommonDocs;
    }

    
}