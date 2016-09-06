package org.insightcentre.nlp.saffron.documentindex.lucene;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

public class OccBasedSimilarity extends TFIDFSimilarity {

	public OccBasedSimilarity() {
		//Need empty constructor so we can use this with Solr in Saffron 2
	}
	
	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		return 1/sumOfSquaredWeights;
	}

	@Override
	public float sloppyFreq(int distance) {
		return 1;
	}

	@Override
	public float tf(float freq) {
		return freq;
	}

	@Override
	public float idf(long docFreq, long numDocs) {
		return 1;
	}

	@Override
	public float coord(int overlap, int maxOverlap) {
		return 1;
	}

	@Override
	public float decodeNormValue(long norm) {
		return 1;
	}

	@Override
	public long encodeNormValue(float f) {
		return 1;
	}

	@Override
	public float scorePayload(int doc, int start, int end, BytesRef payload) {
		return 1;
	}

	@Override
	public float lengthNorm(FieldInvertState state) {
		return 1;
	}

}
