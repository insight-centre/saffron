package org.insightcentre.saffron.web.api;

public class RequestObj {

    private final long id;
    private final String content;

    public RequestObj(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}