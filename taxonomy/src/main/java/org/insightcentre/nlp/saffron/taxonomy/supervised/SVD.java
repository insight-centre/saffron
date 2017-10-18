package org.insightcentre.nlp.saffron.taxonomy.supervised;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Generate a function that predicts hierarchical relations using vTAw
 * @author John McCrae <john@mccr.ae>
 */
public abstract class SVD {
    private final Object2IntMap<String> ids;
    private final List<double[]> vectors;
    private final List<DoubleList> scores;

    public SVD() {
        ids = new Object2IntOpenHashMap<>();
        vectors = new ArrayList<>();
        scores = new ArrayList<>();
    }
    
    public abstract double[] vector(String t);
    
    public void addExample(String top, String bottom, double score) {
        final int topId;
        if(ids.containsKey(top)) {
            topId = ids.getInt(top);
        } else {
            topId = ids.size();
            ids.put(top, topId);
            vectors.add(vector(top));
            scores.add(new DoubleArrayList(new double[ids.size()]));
            for(DoubleList dl : scores) {
                dl.add(0);
            }
        }
        
        final int bottomId;
        if(ids.containsKey(bottom)) {
            bottomId = ids.getInt(bottom);
        } else {
            bottomId = ids.size();
            ids.put(bottom, bottomId);
            vectors.add(vector(bottom));
            scores.add(new DoubleArrayList(new double[ids.size()]));
            for(DoubleList dl : scores) {
                dl.add(0);
            }
        }
        scores.get(topId).set(bottomId, score);
    }
    
    public Matrix solve() {
        Matrix W = new Matrix(vectors.toArray(new double[vectors.size()][]));
        double[][] bArr = new double[scores.size()][];
        for(int i = 0; i < bArr.length; i++) {
            bArr[i] = scores.get(i).toDoubleArray();
        }
        Matrix B = new Matrix(bArr);
        SingularValueDecomposition svd = W.svd();
        Matrix S = svd.getS();
        for(int i = 0; i < S.getColumnDimension(); i++) {
            S.set(i, i, 1.0 / S.get(i, i));
        }
        return svd.getU().times(S).times(svd.getV().transpose())
                .times(B).times(svd.getV()).times(S).times(svd.getU().transpose());
    }
}
