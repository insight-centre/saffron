package org.insightcentre.nlp.saffron.term.lda;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
    private final int[][][] N_lkw;
    private final int[][] N_lk;
    private final AssignmentBuffer corpus;
    private final double[] P;
    private final double alpha, beta;
    private int iterDelta = 0;

    public LDA(File corpus, int K, int J, int W, double alpha, double beta) throws IOException {
        this.K = K;
        this.J = J;
        this.W = W;
        this.alpha = alpha;
        this.beta = beta;
        this.N_kj = new int[K][J];
        this.N_lkw = new int[2][K][W];
        this.N_lk = new int[2][K];
        this.corpus = AssignmentBuffer.interleavedFrom(corpus);
        this.P = new double[K];
    }

    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    
    public void train(int iterations) {
        try {
            System.err.println("Initializing");
            initialize();
            for (int i = 0; i < iterations; i++) {
                //System.err.print(i % 10 == 9 ? "O" : ".");
                final long begin = System.currentTimeMillis();
                iterate();
                final long time = System.currentTimeMillis() - begin;
                final long eta = time * (iterations - i - 1) / 1000;
                System.err.println("Iteration " + (i+1) + " ETA " + String.format("%dh%02dm%02ds", eta/3600, (eta%3600)/60, (eta%60)));
            }
            //System.err.println();
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    public void writeModel(File outFile) throws IOException {
        final DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile));
        out.writeInt(2);
        out.writeInt(J);
        out.writeInt(W);
        out.writeInt(K);
        out.writeDouble(alpha);
        out.writeDouble(beta);
        for (int l = 0; l < 2; l++) {
            for (int k = 0; k < K; k++) {
                for (int w = 0; w < W; w++) {
                    out.writeInt(N_lkw[l][k][w]);
                }
            }
        }
        for (int l = 0; l < 2; l++) {
            for (int k = 0; k < K; k++) {
                out.writeInt(N_lk[l][k]);
            }
        }
        out.flush();
        out.close();
    }

    private void initialize() throws IOException {
        int N = 0;
        while (corpus.hasNext()) {
            int w = corpus.getNext() - 1;
            int oldK = corpus.getNext();
            assert (oldK == 0);
            if (w == -1) {
                N++;
            } else {
                final int j = N / 2;
                final int l = N % 2;
                final int k = random.nextInt(K);
                assignZ(l, j, w, k, -1);
                corpus.update(k);
            }
        }
    }

    private void iterate() throws IOException {
        corpus.reset();
        iterDelta = 0;
        int N = 0;
        while (corpus.hasNext()) {
            int w = corpus.getNext() - 1;
            int oldK = corpus.getNext();
            if (w == -1) {
                N++;
            } else {
                final int j = N / 2;
                final int l = N % 2;
                final int k = sample(w, j, l, oldK);
                assignZ(l, j, w, k, oldK);
                corpus.update(k);
            }
        }
    }
    private final Random random = new Random();

    private int sample(int w, int j, int l, int prevK) {
        double u = random.nextDouble();
        double sum = 0.0;
        double bestPk = Double.NEGATIVE_INFINITY;
        int bestK = 0;
        for (int k = 0; k < K; k++) {
            final int dec = prevK == k ? 1 : 0;
            P[k] = a_kj(k, j, dec) * b_lwk(l, w, k, dec) / c_lk(l, k, dec);
            assert (P[k] >= 0);
            if (P[k] > bestPk) {
                bestPk = P[k];
                bestK = k;
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

    private void assignZ(int l, int j, int w, int k, int oldK) {
        if (k != oldK) {
            if (oldK >= 0) {
                N_kj[oldK][j]--;
                N_lkw[l][oldK][w]--;
                N_lk[l][oldK]--;
            }

            N_kj[k][j]++;
            N_lkw[l][k][w]++;
            N_lk[l][k]++;

            iterDelta++;
        }
    }

    private double a_kj(int k, int j, int dec) {
        return (double) N_kj[k][j] + alpha - dec;
    }

    private double b_lwk(int l, int w, int k, int dec) {
        return ((double) N_lkw[l][k][w] + beta - dec);
    }

    private double c_lk(int l, int k, int dec) {
        return ((double) N_lk[l][k] + W * beta - dec);
    }

//    public static void main(String[] args) throws Exception {
//        final CLIOpts opts = new CLIOpts(args);
//        double alpha = opts.doubleValue("alpha", -1, "The alpha parameter");
//        final double beta = opts.doubleValue("beta", 0.01, "The beta parameter");
//        final File corpus = opts.roFile("corpus[.gz|.bz2]", "The corpus file");
//        final int W = opts.intValue("W", "The number of distinct tokens");
//        final int J = opts.intValue("J", "The number of documents (per language)");
//        final int K = opts.intValue("K", "The number of topics");
//        final int N = opts.intValue("N", "The number of iterations");
//        final File outFile = opts.woFile("model[.gz|.bz2]", "The file to write the model to");
//
//        if (!opts.verify(LDATrain.class)) {
//            return;
//        }
//        if (alpha == -1.0) {
//            alpha = 2.0 / K;
//        }
//        if (alpha < 0 || beta < 0) {
//            throw new IllegalArgumentException("Alpha and beta cannot be negative");
//        }
//        final LDATrain ldaTrain = new LDATrain(corpus, K, J, W, alpha, beta);
//        ldaTrain.train(N);
//        ldaTrain.writeModel(outFile);
//    }
}
