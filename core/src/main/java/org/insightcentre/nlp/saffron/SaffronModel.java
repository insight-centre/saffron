package org.insightcentre.nlp.saffron;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.SaffronData;


@JsonIgnoreProperties(ignoreUnknown=true)
public class SaffronModel {

    @JsonProperty("config") Configuration configuration;
    @JsonProperty("data") SaffronData input;

    @JsonCreator
    public SaffronModel(@JsonProperty("config") Configuration configuration,
                        @JsonProperty("data") SaffronData input) {
        this.configuration = configuration;
        this.input = input;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
    public SaffronData getInput() {
        return input;
    }

}
