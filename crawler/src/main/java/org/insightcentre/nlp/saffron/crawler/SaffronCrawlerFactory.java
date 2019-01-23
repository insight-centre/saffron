package org.insightcentre.nlp.saffron.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.insightcentre.nlp.saffron.data.Document;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class SaffronCrawlerFactory implements CrawlController.WebCrawlerFactory<SaffronCrawler> {
    private String languageFilter;
    private String urlFilter;
    private int collectionLimit = Integer.MAX_VALUE;
    private final File saveFolder;
    private final List<Document> corpus;

    public SaffronCrawlerFactory(File saveFolder) {
        this.saveFolder = saveFolder;
        this.corpus = Collections.synchronizedList(new ArrayList<Document>());
    }
    
    @Override
    public SaffronCrawler newInstance() throws Exception {
        return new SaffronCrawler(languageFilter, urlFilter, collectionLimit, saveFolder, corpus);
    }

    public String getLanguageFilter() {
        return languageFilter;
    }

    public void setLanguageFilter(String languageFilter) {
        this.languageFilter = languageFilter;
    }

    public String getUrlFilter() {
        return urlFilter;
    }

    public void setUrlFilter(String urlFilter) {
        this.urlFilter = urlFilter;
    }

    public int getCollectionLimit() {
        return collectionLimit;
    }

    public void setCollectionLimit(int collectionLimit) {
        this.collectionLimit = collectionLimit;
    }
 
    public List<Document> getDocuments() {
        return Collections.unmodifiableList(corpus);
    }
}
