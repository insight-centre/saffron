package org.insightcentre.nlp.saffron.documentindex;


import org.insightcentre.nlp.saffron.documentindex.lucene.AnalyzerLower;
import org.insightcentre.nlp.saffron.documentindex.lucene.AnalyzerStemmedLower;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneIndexer;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneSearcher;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

@Deprecated
public class DocumentIndexFactory {
		public static enum LuceneAnalyzer {
			LOWERCASE_ONLY(new AnalyzerLower()),
			LOWERCASE_STEMMER(new AnalyzerStemmedLower());
			
			public Analyzer analyzer;
			LuceneAnalyzer(Analyzer analyzer) {
				this.analyzer = analyzer;
			}
		}
	
        public static Directory luceneFileDirectory(File indexPath, boolean clearExistingIndex) 
        		throws IOException, IndexingException {
                if (clearExistingIndex) {
                	FileUtils.deleteDirectory(indexPath);
                }      
                return FSDirectory.open(indexPath);
        }      
               
        public static Directory luceneMemoryDirectory() {
                return new RAMDirectory();
        }
        
        public static DocumentIndexer luceneIndexer(Directory directory, LuceneAnalyzer analyzer) 
        		throws IndexingException {
                return new LuceneIndexer(directory, analyzer.analyzer);                   
        }      
               
        public static DocumentSearcher luceneSearcher(Directory directory, LuceneAnalyzer analyzer) 
        		throws IOException {
                return new LuceneSearcher(directory, analyzer.analyzer);
        }      
               
}
