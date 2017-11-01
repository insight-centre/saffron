package org.insightcentre.nlp.saffron.data.index;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;

/**
 * For creating a document searcher
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentSearcherFactory {
    private DocumentSearcherFactory() {}
    
    public static DocumentSearcher loadSearcher(IndexedCorpus corpus) {
        if(corpus.index == null) throw new IllegalArgumentException("Corpus has no index");
        return loadSearcher(corpus, corpus.index.toFile(), false);
    }
    /**
     * Create a document searcher
     * @param corpus The corpus metadata
     * @param index Where to build the index if it does not already exist
     * @return A document searcher
     */
    public static DocumentSearcher loadSearcher(Corpus corpus, File index) {
        return loadSearcher(corpus, index, false);
    }

    /**
     * Create a document searcher
     * @param corpus The corpus metadata
     * @param index Where to build the index if it does not already exist
     * @param rebuild Rebuild the corpus even if it already exists
     * @return A document searcher
     */
    public static DocumentSearcher loadSearcher(Corpus corpus, File index, boolean rebuild) {
        try {
            Class c = Class.forName("org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory");
            Method method = c.getDeclaredMethod("loadSearcher", Corpus.class, File.class, Boolean.class);
            return (DocumentSearcher)method.invoke(null, corpus, index, rebuild);
        } catch(ClassNotFoundException x) {
            throw new RuntimeException("Could not locate document search implementation, please include document indexer on class path", x);
        } catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException x) {
            throw new RuntimeException(x);
        }
     }
}