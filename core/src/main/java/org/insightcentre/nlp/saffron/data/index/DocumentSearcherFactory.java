package org.insightcentre.nlp.saffron.data.index;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.insightcentre.nlp.saffron.data.Corpus;

/**
 * For creating a document searcher
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class DocumentSearcherFactory {
    private DocumentSearcherFactory() {}
    /**
     * Create a document searcher
     * @param corpus The corpus metadata
     * @return A document searcher
     */
    public static DocumentSearcher loadSearcher(Corpus corpus) {
        try {
            Class c = Class.forName("org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory");
            Method method = c.getDeclaredMethod("loadSearcher", Corpus.class);
            return (DocumentSearcher)method.invoke(null, corpus);
        } catch(ClassNotFoundException x) {
            throw new RuntimeException("Could not locate document search implementation, please include document indexer on class path", x);
        } catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Create a document searcher
     * @param corpus The corpus metadata
     * @param rebuild Rebuild the corpus even if it already exists
     * @return A document searcher
     */
    public static DocumentSearcher loadSearcher(Corpus corpus, boolean rebuild) {
        try {
            Class c = Class.forName("org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory");
            Method method = c.getDeclaredMethod("loadSearcher", Corpus.class, Boolean.class);
            return (DocumentSearcher)method.invoke(null, corpus, rebuild);
        } catch(ClassNotFoundException x) {
            throw new RuntimeException("Could not locate document search implementation, please include document indexer on class path", x);
        } catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException x) {
            throw new RuntimeException(x);
        }
     }
}