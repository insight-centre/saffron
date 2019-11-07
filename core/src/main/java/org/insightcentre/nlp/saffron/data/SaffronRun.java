package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaffronRun {

    @JsonProperty("id")
    /**
     * The unique id for this run
     */
    public String id;
    /**
     * The run date for this execution
     */
    public Date runDate;
    /**
     * The original configuration for this execution
     */
    public String config;


    @JsonCreator
    public SaffronRun(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "run_date") Date runDate,
            @JsonProperty(value = "config") String config) {
        super();
        this.id = id;
        this.runDate = runDate;
        this.config = config;

    }



}

