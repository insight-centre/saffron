package org.insightcentre.saffron.web.api;


import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

public class BaseResponse {


    private String id;
    private Date runDate;


    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }


    public Date getRunDate() {
        return runDate;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getId() {
        return id;
    }
}
