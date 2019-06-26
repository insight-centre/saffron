package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class SearchWrapperResponse {

    List<SearchResponse> searchResponses = new ArrayList<>();

    public List<SearchResponse> getSearchResponses() {
        return this.searchResponses;
    }

    public void setSearchResponses(SearchResponse searchResponses) {
        this.searchResponses.add(searchResponses);
    }
}
