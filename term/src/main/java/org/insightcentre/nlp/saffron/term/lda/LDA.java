package org.insightcentre.nlp.saffron.term.lda;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Random;

/**
 *
 * @author John McCrae
 */
public class LDA {

    /**
     * Number of topics
     */
    private final int K;
    /**
     * Number of documents
     */
    private final int J;
    /**
     * Number of words in vocabulary
     */
    private final int W;
    /**
     * Counts
     */
    private final int[][] N_kj;
    final int[][] N_kw;
    final int[] N_k;
    private final AssignmentBuffer corpus;
    private final double[] P;
    private final double alpha, beta;
    private int iterDelta = 0;

    public LDA(AssignmentBuffer corpus, int K, int J, int W, double alpha, double beta) throws IOException {
        this.K = K;
        this.J = J;
        this.W = W;
        this.alpha = alpha;
        this.beta = beta;
        this.N_kj = new int[K][J];
        this.N_kw = new int[K][W];
        this.N_k = new int[K];
        this.corpus = corpus;
        this.P = new double[K];
    }

    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public void train(int iterations, boolean verbose) {
        try {
            System.err.println("Initializing");
            initialize();
            for (int i = 0; i < iterations; i++) {
                //System.err.print(i % 10 == 9 ? "O" : ".");
                final long begin = System.currentTimeMillis();
                iterate();
                final long time = System.currentTimeMillis() - begin;
                final long eta = time * (iterations - i - 1) / 1000;
                if(verbose)
                System.err.println("Iteration " + (i + 1) + " ETA " + String.format("%dh%02dm%02ds", eta / 3600, (eta % 3600) / 60, (eta % 60)));
            }
            //System.err.println();
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    private void initialize() throws IOException {
        int N = 0;
        while (corpus.hasNext()) {
            int w = corpus.getNext();
            int oldK = corpus.getNext();
            assert (oldK == 0);
            if (w == -1) {
                N++;
            } else {
                final int j = N;
                final int k = random.nextInt(K);
                assignZ(j, w, k, -1);
                corpus.update(k);
            }
        }
    }

    private void iterate() throws IOException {
        corpus.reset();
        iterDelta = 0;
        int N = 0;
        while (corpus.hasNext()) {
            int w = corpus.getNext();
            int oldK = corpus.getNext();
            if (w == -1) {
                N++;
            } else {
                final int j = N;
                final int k = sample(w, j, oldK);
                assignZ(j, w, k, oldK);
                corpus.update(k);
            }
        }
    }
    
    public void printAssignment(Int2ObjectMap<String> dictionary) throws IOException {
        corpus.reset();
        System.out.println("Assignment:");
        while (corpus.hasNext()) {
            int w = corpus.getNext();
            int k = corpus.getNext();
            if (w == -1) {
                System.out.println();
            } else {
                System.out.print(String.format("%s=%d ", dictionary.getOrDefault(w, "???"), k));
            }
        }
    }
    
    private final Random random = new Random();

    private int sample(int w, int j, int prevK) {
        double u = random.nextDouble();
        double sum = 0.0;
        double bestPk = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < K; k++) {
            final int dec = prevK == k ? 1 : 0;
            P[k] = a_kj(k, j, dec) * b_wk(w, k, dec) / c_k(k, dec);
            assert (P[k] >= 0);
            if (P[k] > bestPk) {
                bestPk = P[k];
            }
            //if (cooling > 0) {
//                P[k] = fastpow(P[k], 1.0 + cooling * iterNo);
            //          }
            sum += P[k];
        }
        //    if (P[bestK] / sum > freezingPoint) {
        //frozen[j][i] = true;
        //temp--;
        //return bestK;
        //} else {
        for (int k = 0; k < K; k++) {
            if (u < (P[k] / sum)) {
                return k;
            }
            P[k + 1] += P[k];
        }
        //}
        throw new RuntimeException("P[K] = " + P[K] + " sum= " + sum);
    }

    private void assignZ(int j, int w, int k, int oldK) {
        if (k != oldK) {
            if (oldK >= 0) {
                N_kj[oldK][j]--;
                N_kw[oldK][w]--;
                N_k[oldK]--;
            }

            N_kj[k][j]++;
            N_kw[k][w]++;
            N_k[k]++;

            iterDelta++;
        }
    }

    private double a_kj(int k, int j, int dec) {
        return (double) N_kj[k][j] + alpha - dec;
    }

    private double b_wk(int w, int k, int dec) {
        return ((double) N_kw[k][w] + beta - dec);
    }

    private double c_k(int k, int dec) {
        return ((double) N_k[k] + W * beta - dec);
    }
}
