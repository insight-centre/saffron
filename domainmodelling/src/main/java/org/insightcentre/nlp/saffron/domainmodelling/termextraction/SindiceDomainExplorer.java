/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.termextraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;


/**
 * @author Georgeta Bordea
 * 
 */
public class SindiceDomainExplorer {

  public static final String SINDICE_URL = "http://api.sindice.com/v2/search?q=";
  public static final String JSON = "json";


  protected static final String ACM_DOMAIN_FILTER =
      "&fq=domain%3Aacm.rkbexplorer.com&interface=advanced";
  protected static final String CITESEER_DOMAIN_FILTER =
      "&fq=domain%3Aciteseer.rkbexplorer.com&interface=advanced";
  protected static final String DBLP_DOMAIN_FILTER =
      "&fq=domain%3Adblp.rkbexplorer.com&interface=advanced";

  private static final String TRIPLE = "&nq=%28*%20%3Ctitle%3E%20%27";
  private static final String END_TRIPLE = "%27%29";

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      System.out.println(searchDomainTitleHits("information retrieval",
          CITESEER_DOMAIN_FILTER));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * http://sindice.com/search?q=&nq=%28*%20%3Ctitle%3E%20%27
   * information%20retrieval%27%29
   * &fq=domain%3Aacm.rkbexplorer.com&interface=advanced
   * 
   * 
   * @param search
   * @return
   * @throws JSONException
   * @throws IOException
   */
  public static Long searchDomainTitleHits(String search, String domain) {

    String url =
      SINDICE_URL + TRIPLE + search.replace(' ', '+') + END_TRIPLE
            + domain;

    ObjectMapper mapper = new ObjectMapper();
    try {
        Map searchResults = mapper.readValue(urlConnectionDownload(url, domain), Map.class);
        return (Long)searchResults.get("totalResults");
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
  }


    public static String urlConnectionDownload(String url, String applicationType)
            throws IOException {
        URL site = new URL(url);
        URLConnection urlConn = site.openConnection();
        urlConn.setRequestProperty("Accept", "application/" + applicationType);

        BufferedReader in =
                new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        String inputLine;
        StringBuffer sb = new StringBuffer();
        try {
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
        } finally {
            in.close();
        }

        return sb.toString();
    }


}
