package org.insightcentre.nlp.saffron.data;

/**
 * The status of a term in editing
 *
 * @author John McCrae
 */
public enum Status {
    accepted("accepted"),
    rejected("rejected"),
    none("none");
	
	private String status;
	
	Status(String status) {
		this.status = status;
	}
	
	public String toString() {
		return this.status;
	}
}
