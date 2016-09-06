package org.insightcentre.nlp.saffron.documentindex.lucene;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

public class OnlyTFIDFSimilarity extends TFIDFSimilarity {

	public float queryNorm(float sumOfSquaredWeights) {
		return 1;
	}

	public float tf(float freq) {
		return (float) Math.sqrt(freq);
	}

	public float sloppyFreq(int distance) {
		return 1;
	}

	/** Implemented as <code>log(numDocs/(docFreq+1)) + 1</code>. */
	public float idf(long docFreq, long numDocs) {
		return (float) (Math.log(numDocs / (double) (docFreq + 1)) + 1.0);
	}

	public float coord(int overlap, int maxOverlap) {
		return 1;
	}

	@Override
	public float lengthNorm(FieldInvertState state) {
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
}
