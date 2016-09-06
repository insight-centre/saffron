package org.insightcentre.nlp.saffron.documentindex.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.EnglishStemmer;

public final class AnalyzerStemmedLower extends Analyzer {
	public static final Version MATCH_VERSION = LuceneConfig.LUCENE_VERSION;

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		StandardTokenizer source = new StandardTokenizer(MATCH_VERSION, reader);

		TokenStream filter = new StandardFilter(MATCH_VERSION, source);
		filter = new LowerCaseFilter(MATCH_VERSION, filter);
		filter = new SnowballFilter(filter, new EnglishStemmer());
		return new TokenStreamComponents(source, filter);
	}

}
