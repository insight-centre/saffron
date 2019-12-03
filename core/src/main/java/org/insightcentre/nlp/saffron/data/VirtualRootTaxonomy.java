package org.insightcentre.nlp.saffron.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A taxonomy of terms where the root node is a virtual node rather than an existing term
 * 
 * @author Bianca Pereira
 */
public class VirtualRootTaxonomy extends Taxonomy{

	public static final String VIRTUAL_ROOT = "HEAD_TERM";
	
	public VirtualRootTaxonomy() {
		super();
		this.setRoot(VIRTUAL_ROOT);
	}
	
	public VirtualRootTaxonomy(Taxonomy taxonomy) {
		super();
		this.setRoot(VIRTUAL_ROOT);
		this.addChild(taxonomy);
	}
	
	@JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public VirtualRootTaxonomy(@JsonProperty("root") String root,
                    @JsonProperty("score") double score,
                    @JsonProperty("linkScore") double linkScore,
                    @JsonProperty("children") List<Taxonomy> children,
                    @JsonProperty("status") Status status) {
		
		super();
		this.setRoot(VIRTUAL_ROOT);
		this.addChild(new Taxonomy(root, score, linkScore, children, status));
	}
}
