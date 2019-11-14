package org.insightcentre.nlp.saffron.taxonomy.metrics;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Set;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxoLink;
import static java.lang.Math.*;
import java.util.Arrays;

/**
 * A metric this measures a taxonomy based on the expected number of children.
 * The Bhattacharyya metric measures the similarity of two probability 
 * distributions as
 * 
 *    P(p1, p2) = sum(sqrt(p1 * p2))
 * 
 * If we assume that we have on average l children of each node we would expect
 * to see the following number of nodes with x children
 * 
 *   E[x] = choose(n,x) * (l/n)^x (1 - l/n)^ (n - x)
 *        =~ l ^ x * exp(-l) / fact(x)
 * 
 * Thus the metric should be of the form
 * 
 *  sim = sum(sqrt(f / n * l ^ x * exp(-l) / fact(x)))
 * 
 * However as every node has exactly one parent there are only (n - 1) links 
 * in the graph, so the average number of children is (n-1)/n =~ 1. Instead
 * we ignore all leaf nodes (with no children) hence the metric is
 * 
 *   sim = sum_&gt;0(sqrt(f / (n-n0) * l ^ x * exp(-l) / fact(x))) + CONSTANT
 * 
 *  We then weight this according to the size of the graph and a factor alpha
 * 
 *   sim2 = n * alpha * sim
 * 
 * @author John McCrae
 */
public class BhattacharryaPoisson implements TaxonomyScore {
    private final TaxonomyScore baseScore;
    private final Object2IntMap<String> term2index;
    private final int[] f;
    private final int[] c;
    private final double[] p;
    private final int N;
    private final double lambda;
    //private double alpha;
    private final double r;

    public BhattacharryaPoisson(TaxonomyScore baseScore, Set<String> terms, 
            double lambda, double alpha) {
        this.baseScore = baseScore;
        this.term2index = new Object2IntArrayMap<>();
        int i = 0;
        for(String t : terms) {
            this.term2index.put(t, i++);
        }
        this.f = new int[terms.size()];
        this.N = terms.size() - 1;
        this.p = dpois(lambda, N);
        f[0] = N;
        this.c = new int[terms.size()];
        this.lambda = lambda;
        this.r = alpha * N;
    }

    private BhattacharryaPoisson(TaxonomyScore baseScore, Object2IntMap<String> term2index, int[] f, int[] c, double[] p, int N, double lambda, double r) {
        this.baseScore = baseScore;
        this.term2index = term2index;
        this.f = f;
        this.c = c;
        this.p = p;
        this.N = N;
        this.lambda = lambda;
        this.r = r;
    }

    private static double[] dpois(double lambda, int N) {
        double[] d = new double[N+1];
        double el = exp(-lambda);
        for(int i = 0; i < N+1; i++) {
            d[i] = el * poisson(i, lambda);
        }
        return d;
    }
    
    @Override
    public double deltaScore(TaxoLink taxoLink) {
        int t = term2index.get(taxoLink.top);
        final double delta;
        if(c[t] > 0) {
            delta =
                    (sqrt(f[c[t] + 1] + 1) - sqrt(f[c[t] + 1])) * sqrt(p[c[t] + 1] / (N - f[0])) +
                    (sqrt(f[c[t]] - 1) - sqrt(f[c[t]])) * sqrt(p[c[t]]) / (N - f[0]);
        } else /*if(f[c[t]] == 0)*/ {
            double d = 0.0;
            d += sqrt(p[1] * (f[1] + 1) / (N - f[0] + 1))
                    - (N == f[0] ? 0.0 : sqrt(p[1] * (f[1]) / (N - f[0])));
            for(int i = 2; i < p.length; i++) {
                d += sqrt(p[i] * f[i] / (N - f[0] + 1)) 
                        - (N == f[0] ? 0.0 : sqrt(p[i] * f[i] / (N - f[0])));
            }
            delta = d;
        } 
        return delta * r + baseScore.deltaScore(taxoLink);
    }

    @Override
    public TaxonomyScore next(String top, String bottom, Solution soln) {
        int t = term2index.get(top);
        int[] newC = Arrays.copyOf(c, c.length);
        newC[t]++;
        int[] newF = Arrays.copyOf(f, f.length);
        newF[c[t]]--;
        newF[newC[t]]++;
        return new BhattacharryaPoisson(baseScore, term2index, newF, newC, p, N, lambda, r);
    }
    // Calculates y ** x / x! mostly by the Sterling approximation
    // =~ 1/sqrt(2*pi*x) (e * y / x) ** x 
    private static double poisson(int x, double y) {
        if(x < 10) {
            double f = 1.0;
            for(int i = 2; i <= x; i++) {
                f /= i;
            }
            return pow(y, x) * f;
        } else {
            return pow(E * y / x, x) / sqrt(2 * PI * x);
        }
    }
    
    
    
}
