package org.insightcentre.nlp.saffron.authors;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.insightcentre.nlp.saffron.data.Author;

/**
 *
 * @author Hugues Lerebours Pigeonnière <hugues@lereboursp.net>
 * @author John McCrae <john@mccr.ae>
 */
public class ConsolidateAuthors {
    public static final double min_similarity = 0.3;
    public static final double UNASSIGNED_COST = 0.1;
    private final static PyAuthorSim sim = new PyAuthorSim();
    
    public static Map<Author, Set<Author>> consolidate(List<Author> authors) {
        Map<Author, Set<Author>> consolidated = new HashMap<>();
        Set<Author> marked = new HashSet<>();
        for(Author author : authors) {
            if(marked.contains(author))
                continue;
            Set<Author> similar = new HashSet<>();
            similar.add(author);
            marked.add(author);
            for(Author author2 : authors) {
                if(!marked.contains(author2)) {
                    if(isSimilar(author, author2)) {
                        similar.add(author2);
                        marked.add(author2);
                    }
                }
            }
            consolidated.put(_choose_author(similar), similar);
//
//            List<Researcher> researchers = new ArrayList<>();
//            for(Author author2: authors) {
//                researchers.add(new Researcher(author2, author2.name));
//            }
//            Researcher best = _cluster(researchers);
//
//            Set<Author> as = new HashSet<Author>();
//            for(Researcher r : researchers) {
//                as.add(r.author);
//            }
//            consolidated.put(best.author, as);
        }
        return consolidated;
    }

    public static boolean isSimilar(Author author, Author author2) {
        return sim.similar(author.getName(), author2.getName());
    }

    static int _max(List<Integer> labels) {
        int max = Integer.MIN_VALUE;
        for(int i : labels) {
            if(i > max) {
                max = i;
            }
        }
        return max;
    }

    static String join(List<NameElem> firstnames) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(NameElem ne : firstnames) {
            if(first) {
                first = false;
            } else {
                sb.append(" ");
            }
            sb.append(ne.e1);
        }
        return sb.toString();
    }

    static Author _choose_author(Collection<Author> authors) {
        Author best = null;
        double bestScore = 0.0;
        for(Author author : authors) {
            PyAuthorSim.RuleMatcher.probas_for pa = PyAuthorSim.RuleMatcher.get_probas_for(author.getName());
            double score = 0.0;
            for(int i = 0; i < pa.words.length; i++) {
                double wordScore = (pa.words[i].length() - count(pa.words[i], '_')) * 
                    (pa.probas[i][0] > pa.probas[i][1] ? pa.probas[i][0] : pa.probas[i][1]);
                score += wordScore;
            }
            score = Math.pow(score, 1.0 / pa.words.length);
            if(score > bestScore) {
                best = author;
                bestScore = score;
            }
        }
        return best;
    }
    
    static String _choose(Collection<String> names) {
        Map<String, List<Object>> name_word = new HashMap<>();
        Object2DoubleMap word_occ = new Object2DoubleOpenHashMap<>();
        for(String w : names) {
            List<Object> name;
            if(name_word.containsKey(w)) {
                name = name_word.get(w);
            } else {
                name = new ArrayList<Object>();
                name_word.put(w, name);
            }
            boolean _new = name.isEmpty();
            for(String s : w.replaceAll("-", " ").split("\\s+")) {
                if(_new) {
                    name.add(s);
                }
                word_occ.put(s, word_occ.getDouble(s) + s.length() - count(s, '_'));
            }
        }

        double m = 0;
        String best = "";
        for(Map.Entry<String, List<Object>> e : name_word.entrySet()) {
            String w = e.getKey();
            List<Object> name = e.getValue();
            double score = sum_word_occ(word_occ, name) + count(w, '-');
            if(score > m || (score == m && w.length() > best.length())) {
                m = score;
                best = w;
            }
        }

        return best;
    }

    static int count(String w, char c) {
        int count = 0;
        for(int i = 0; i < w.length(); i++)
            if(c == w.charAt(i))
                count++;
        return count;
    }

    static int _min(Set<Integer> s) {
        int min = Integer.MAX_VALUE;
        for(int i : s) 
            if(i < min)
                min = i;
        return min;
    }

    static double sum_word_occ(Object2DoubleMap word_occ, List<Object> name) {
        double sum = 0.0;
        for(Object o : name) {
            sum += word_occ.getDouble(o);
        }
        return sum;
    }

    static class Researcher {
        public Set<Researcher> e0;
        public boolean e1;
        public Set<Object> e2 = new HashSet<>();
        public Set<Object> e3 = new HashSet<>();
        public Set<Object> spec = new HashSet<>();
        public List<Word> words;
        public final Author author;

        public final Map<Researcher, Link> links = new HashMap<>();
        public int papers = 1;

        public Researcher(Author author, String original) {
            this.author = author;
            PyAuthorSim.RuleMatcher.probas_for p = PyAuthorSim.RuleMatcher.get_probas_for(original);
            String[] words = p.words;
            double[][] probas = p.probas;
            this.words = Arrays.asList(PyAuthorSim.makeWords(words, probas));
        }
        
        public Researcher(Author author, Set<Researcher> e0, boolean e1, Set<Object> e2) {
            this.author = author;
            this.e0 = e0;
            this.e1 = e1;
            this.e2 = e2;
        }

        public double weight(Researcher r) { return 0.0; }

    }

    static class WordLink {
        public final List<Word> e0, e2;
        public final List<Integer> e1, e3;
        public final int e4;

        public WordLink(List<Word> e0, List<Integer> e1, List<Word> e2, List<Integer> e3, int e4) {
            this.e0 = e0;
            this.e2 = e2;
            this.e1 = e1;
            this.e3 = e3;
            this.e4 = e4;
        }



    }
    
    static class Link {
        public double _proba;
        public final WordLink[] word_links;

        public Link(double _proba, WordLink[] word_links) {
            this._proba = _proba;
            this.word_links = Arrays.copyOf(word_links, word_links.length);
        }

        

        boolean browsed() {
            return  _proba < 0;
        }

        void flag() {
            _proba = -_proba;
        }

    }
    
    static void browse_links(Researcher self, Set<Researcher> researchers, Map<Researcher, Map<Researcher, Link>> alllinks) {
        for(Map.Entry<Researcher, Link> l : alllinks.get(self).entrySet()) {
            Researcher author = l.getKey();
            Link links = l.getValue();
            if(!links.browsed()) {
                boolean common = false;
                for(Researcher researcher : researchers) {
                    Researcher fr = null;
                    Researcher to = author;
                    if(researcher.e0.contains(self)) {
                        if(researcher.e0.contains(author)) {
                            common = true;
                        } else {
                            fr = self;
                        }
                    } else if(researcher.e0.contains(author)) {
                        fr = author;
                        to = self;
                    }

                    if(fr != null) {  // `fr' is in `researcher', `to' is not
                        boolean linked = true;
                        for(Researcher node : researcher.e0) {
                            if(!node.equals(fr) && !alllinks.get(to).containsKey(node)) {
                                linked = false;
                            }
                        }
                        if(linked) {
                            researcher.e0.add(to);
                            common = true;
                        }
                    }
                }

                if(!common) {
                    Set<Researcher> s1 = new HashSet<>();
                    s1.add(self);
                    s1.add(author);
                    researchers.add(new Researcher(self.author, s1, false, new HashSet<>()));
                }
                links.flag();
            }
        } 
        if(alllinks.get(self).isEmpty()) {
            researchers.add(new Researcher(self.author, Collections.singleton(self), false, new HashSet<>()));
        }
    }

    static <A> boolean intersection(Set<A> s1, Set<A> s2) {
        Set<A> s3 = new HashSet<>(s1);
        s3.retainAll(s2);
        return !s3.isEmpty();
    }
    
    static Researcher _cluster(List<Researcher> cluster) {
        boolean b_best_researcher = true;
        Researcher best_researcher = null;
        while(b_best_researcher) {
            double best_score = -1;
            double best_bonus = 0;
            best_researcher = null;
            b_best_researcher = false;

            for(Researcher researcher : cluster) {
                if(!researcher.e0.isEmpty() && !researcher.e1) {
                    double score = 0;
                    double bonus = 0;
                    int nb_lk = 0;
                    for(Researcher node : researcher.e0) {
                        for(Researcher neighb : researcher.e0) {
                            if(!node.equals(neighb)) {
                                // always > 0: _proba is < 0
                                score += node.weight(neighb) 
                                    - node.links.get(neighb)._proba * 100;
                                nb_lk += 1;
                            }
                        }
                        bonus += node.papers;
                    }

                    if(nb_lk > 0) {
                        score = Math.floor(score / nb_lk);
                    }
                    if(score > best_score 
                            || (score == best_score && bonus >= best_bonus)) {
                        best_researcher = researcher;
                        b_best_researcher = true;
                        best_score = score;
                        best_bonus = bonus;
                    }
                }
            }

            if(b_best_researcher) {
                best_researcher.e1 = true;

                if(!best_researcher.e0.isEmpty()) {
                    best_researcher.e3 = new HashSet<>();
                    for(Researcher node : best_researcher.e0) {
                        best_researcher.e3.addAll(node.spec);
                        for(Researcher researcher : cluster) {
                            if(!researcher.e1 && researcher.e0.contains(node)) {
                                researcher.e0.remove(node);
                            }
                        }
                    }

                    // get firstnames and lastnames;
                    // it has to be done before a possible merge
                    Object first_last = _match_words(best_researcher.e0);
                    if(first_last != null) 
                        best_researcher.e2.add(first_last);

                    for(Researcher researcher : cluster) {
                        if(researcher.e1 && !researcher.e0.isEmpty() 
                                && !researcher.equals(best_researcher)) {
                            boolean linked = false;
                            if(!researcher.e1
                                    && !best_researcher.e1) {
                                linked = (researcher.e1 == best_researcher.e1);
                            } else if(intersection(researcher.e3, best_researcher.e3)) {
                                if(!linked) {
                                    for(Researcher item : best_researcher.e0) {
                                        if(intersection(researcher.e0, item.links.keySet())) {
                                            linked = true;
                                        }
                                    }
                                }
                            }
                            if(linked) {
                                best_researcher.e0.addAll(researcher.e0);
                                if(best_researcher.e1) // not a number
                                    best_researcher.e1 = researcher.e1;
                                best_researcher.e2.addAll(researcher.e2); // firstnames
                                best_researcher.e3.addAll(researcher.e3); // spec

                                researcher.e0.clear();
                            }
                        }
                    }
                }
            }
        }

        int p = 0;
        while(p < cluster.size()) {
            if(!cluster.get(p).e0.isEmpty()) {
                p += 1;
            } else {
                cluster.remove(p);
            }
        }

        return best_researcher;
    }

    static class ProbArray {
        public double e0, e1, e3;
        public Collection<String> e2;

        public ProbArray() {
            e2 = new ArrayList<>();
        }

    }

    static class NameElem implements Comparable<NameElem> {
        public double e0;
        public String e1;

        public NameElem(double e0, String e1) {
            this.e0 = e0;
            this.e1 = e1;
        }

        @Override
        public int compareTo(NameElem o) {
            int i1 = Double.compare(e0, o.e0);
            if(i1 == 0) {
                return e1.compareTo(o.e1);
            }
            return i1;
        }
    }

    static final double min_f_l_proba_diff = .15;

 
    static Object _match_words(Collection<Researcher> researcher) {
        // associate the words together by labels
        List<Integer> labels = new ArrayList<>(Arrays.asList(0));
        for(Researcher author1 : researcher) {
            for(Researcher author2 : researcher) {
                if(!author1.equals(author2)) {
                    Link link = author1.links.get(author2);
                    if(link.browsed()) { // it's unvisited at this stage!
                        link.flag();
                        int i = 0;
                        while(i < link.word_links.length) {
                            List<Word> words = link.word_links[i].e0;
                            List<Integer> ind = link.word_links[i].e1;
                            List<Word> t = new ArrayList<>();
                            for(int j : ind) 
                                t.add(words.get(j));
                            List<Word> u = new ArrayList<>();
                            boolean cont = (t.get(t.size()-1).pout == null);
                            while(cont) {
                                Word w = words.get(ind.get(0) + ind.size() + u.size());
                                u.add(w);
                                cont = (w.pout == null);
                            }
                            words = link.word_links[i].e2;
                            ind = link.word_links[i].e3;
                            for(int j : ind) 
                                t.add(words.get(j));
                            int l = ind.size();
                            cont = (t.get(t.size()-1).pout == null);
                            while(cont) {
                                Word w = words.get(ind.get(0) + l);
                                u.add(w);
                                l += 1;
                                cont = (w.pout == null);
                            }

                            Set<Integer> s = new HashSet<>();
                            for(Word w : t) 
                                s.add(w.label);
                            for(Word w : u)
                                s.add(w.label);

                            int m = labels.size();
                            if(s.size() > 0) {
                                m = _min(s);
                                for(int j : s) {
                                    if(j != m) {
                                        labels.set(j, m);
                                    }
                                }
                            } else {
                                labels.add(m);
                            }

                            for(Word w : t) {
                                w.new_ln += link.word_links[i].e4;
                                w.new_ln_nb += 2;
                                w.label = m;
                            }
                            for(Word w : u) {
                                w.label = m;
                            }

                            i += link.word_links[i].e1.size();
                        }
                    }
                }
            }

            for(Link link : author1.links.values()) {
                if(link.browsed()) {
                    link.flag();
                }
            }
        }

        // consolidate labels
        for(int i = 0; i < labels.size(); i++) {
            int j = i;
            while(j != labels.get(j)) {
                j = labels.get(j);
            }
            labels.set(i, j);
        }

        // compute probabilities (first name or last name) for each label
        List<ProbArray> probas = new ArrayList<>();
        for(int i = 0; i < _max(labels) + 1; i++) 
            probas.add(new ProbArray());
        for(Researcher node : researcher) {
            int i = 0;
            List<Word> ws = node.words;
            while(i < ws.size()) {
                StringBuilder s = new StringBuilder();
                int lbl = labels.get(ws.get(i).label);
                if(lbl == 0) {
                    lbl = probas.size();
                    probas.add(new ProbArray());
                }
                ProbArray p = probas.get(lbl);
                int nb = 0;
                double p0 = .0;
                double p1 = .0;
                boolean cont = true;
                while(cont) {
                    s.append(new String(ws.get(i).word).replaceAll("_", "?"));
                    p0 += ws.get(i).prob_firstname;
                    p1 += ws.get(i).new_ln != 0.0 ? ws.get(i).new_ln / ws.get(i).new_ln_nb : ws.get(i).prob_lastname;
                    nb += 1;
                    i += 1;
                    if(i == node.words.size() || labels.get(ws.get(i).label) != lbl) 
                        cont = false;
                    else
                        s.append(ws.get(i-1).pout == null ? '-' : ' ');
                }
                p.e0 += p0 / nb;
                p.e1 += p1 / nb;
                p.e2.add(s.toString());
                p.e3 += i;
            }
        }

        List<NameElem> firstnames = new ArrayList<>();
        List<NameElem> lastnames = new ArrayList<>();
        for(ProbArray p : probas) {
            if(!p.e2.isEmpty()) {
                if(Math.abs(p.e0 - p.e1) > min_f_l_proba_diff * p.e2.size()) {
                    // the words in p[2] are either first names or last names
                    String name = _choose(p.e2);
                    if(p.e0 > p.e1) // first name
                        firstnames.add(new NameElem(p.e3 / p.e2.size(), name));
                    else // last name
                        lastnames.add(new NameElem(p.e3 / p.e2.size(), name));
                }
            }
        }
        Collections.sort(firstnames); // JMC: I am guessing a lexicographic sorting here...
        Collections.sort(lastnames);

        String firstname = join(firstnames);
        String lastname = join(lastnames);

        return new String[] { firstname, lastname };
    }


 

    static class Word {
        Double pout;
        char[] word;
        double prob_firstname;
        double prob_lastname;
        public int label = 0, new_ln = 0, new_ln_nb = 0;


        char[] append(char[] s, char t) {
            char[] s2 = new char[s.length + 1];
            System.arraycopy(s, 0, s2, 0, s.length);
            s2[s.length] = t;
            return s2;
        }
        char[] append(char[] s, char[] t) {
            char[] s2 = new char[s.length + t.length];
            System.arraycopy(s, 0, s2, 0, s.length);
            System.arraycopy(t, 0, s2, s.length, t.length);
            return s2;
        }

        public Word(CharSequence s, double[] prob_first_last, Object opt2) {
            this.word = new char[s.length()];
            for(int i = 0; i < s.length(); i++) {
                this.word[i] = s.charAt(i);
            }
            this.prob_firstname = prob_first_last[0];
            this.prob_lastname  = prob_first_last[1];

        }
         
        public Word(Word word, double[] prob_first_last, Word opt2) {
            if(prob_first_last == null) {
                this.word = word.word;
                if(word.word.length == 1) {
                    this.word = append(this.word, '.');
                }
                this.word = append(this.word, opt2.word);
                if(opt2.word.length == 1) {
                    this.word = append(this.word, '.');
                }
                this.prob_firstname = opt2.prob_firstname;
                this.prob_lastname = opt2.prob_lastname;
                this.pout = null;
            } else {
                this.word = word.word;
                this.prob_firstname = prob_first_last[0];
                this.prob_lastname = prob_first_last[1];
                //this.pout = opt2; ??
            }

//            this.label = 0;// # to recognize words
//            this.new_ln = 0;// # last name probability is recomputed later
//            this.new_ln_nb = 0;
//
//            f_occ = firstnames.get(word, 0);
//            l_occ = lastnames.get(word, 0);
//            if(!f_occ && firstname_occ_min < l_occ) {
//                f_occ = firstname_occ_min;
//            }
//            if(not l_occ && lastname_occ_min < f_occ) {
//                l_occ = lastname_occ_min;
//            }
//            if(f_occ && l_occ) {
//                if(f_occ > 100 * l_occ) {
//                    this.prob_firstname = 1. - (1. - this.prob_firstname) * 100. * l_occ / f_occ;
//                    this.prob_lastname *= 100. * l_occ / f_occ;
//                } else if(l_occ > 100 * f_occ) {
//                    this.prob_firstname *= 100. * f_occ / l_occ;
//                    this.prob_lastname = 1. - (1. - this.prob_lastname) * 100. * f_occ / l_occ;
//                }
//            }
        }
        
        boolean initial() {
            return (this.word.length == 1 
                 || (this.word.length == 3 && this.word[2] == '.'));
        }

    }

    static class PyAuthorSim {
        int max_nb;
        int min_nb;
        Word[] words;
        double[] probas;
        boolean[] assoc;
        List<L2R> t_l2r = new ArrayList<>();
        List<L2R> best_l2r = new ArrayList<>();
        double res;
        double mean;
        static final double min_similarity = 0.3;


        public double[] range(int i, int j) {
            double[] r = new double[j-i];
            for(int k = i; k < j; k++) {
                r[k-i] = k;
            }
            return r;
        }
                
        public static int len(Object[] a) { return a.length; }
        public static int len(char[] a) { return a.length; }

        public boolean similar(String authorName1, String authorName2) {
            RuleMatcher.probas_for a1 = RuleMatcher.get_probas_for(authorName1);
            RuleMatcher.probas_for a2 = RuleMatcher.get_probas_for(authorName2);

            Word[] w1 = makeWords(a1.words, a1.probas);
            Word[] w2 = makeWords(a2.words, a2.probas);

            return similar(w1, w2);
        }

        public static Word[] makeWords(String[] words, double[][] probas) {
            List<Word> _words = new ArrayList<>();

            //for p, word in enumerate(words):
            for(int p = 0; p < words.length; p++) {
                String word = words[p];
                int beg = 0;
                int l = word.length();
                double p_out = 1. - probas[p][0] - probas[p][1];
                boolean several = false;
                while(beg < l) {
                    int i = word.indexOf('-', beg);
                    if(i == -1) {
                        _words.add(new Word(word.subSequence(beg, l), probas[p], p_out));
                        beg = l;
                    } else {
                        if(!several) {
                            p_out = (p_out + .5) / 1.5;
                            several = true;
                        }
                        _words.add(new Word(word.subSequence(beg,i), probas[p], null));
                        beg = i + 1;
                    }
                }
            }
            return _words.toArray(new Word[_words.size()]);
        }


        public boolean similar(Word[] selfwords, Word[] authorwords) {
            if(selfwords.length > authorwords.length) {
                return similar(authorwords, selfwords);
            }
            this.max_nb = authorwords.length; // maximum number of words
            this.min_nb = selfwords.length; // minimum number of words
            this.words = authorwords;
            this.probas = range(-this.min_nb * this.max_nb, 0);
            //this.assoc = [False] * this.max_nb;
            this.assoc = new boolean[this.max_nb];
            Arrays.fill(this.assoc, false);
            while(this.t_l2r.size() < this.min_nb) {
                this.t_l2r.add(new L2R(selfwords, new int[] {}, authorwords, new int[] {}, Double.MAX_VALUE));
            }
            this.res = -1.0;
            this.mean = .0;

            _simple_assoc(selfwords);
            
            return this.res > min_similarity;
//            _try_assoc(selfwords, 0, 42., .0, 0);
//
//            int sht_len = min(selfwords.length, authorwords.length);
//            this.res /= sht_len;
//            if(this.res > .0) {
//                this.mean /= this.max_nb * sht_len;
//                if(this.min_nb > 2) {
//                    this.res = 1. - (1. - this.res) * 5. 
//                        / (this.min_nb + this.max_nb);
//                }
//
//                double sim = (this.res + this.mean) / 2.;
//
//                if(sim > min_similarity) {
//                    // link the two names
//                    //Link lk = new Link(sim, this.best_l2r);
//                    //links[author] = lk;
//                    //author.links[self] = lk;
//
//                    return true;
//                }
//            }
//
//            return false;
        }


        //#################
        //### internals


        void _simple_assoc(Word[] selfwords) {
            double[][] sim = new double[selfwords.length][this.words.length];
            for(int l = 0; l < selfwords.length; l++) {
                for(int r = 0; r < this.words.length; r++) {
                    sim[l][r] = _get_proba(selfwords, l, r);
                }
            }
            int[] assign = new int[selfwords.length];
            Arrays.fill(assign, -1);
            boolean[] rmark = new boolean[this.words.length];

            boolean found;
            do {
                int lmax = -1;
                int rmax = -1;
                double pmax = Double.NEGATIVE_INFINITY;
                
                for(int l = 0; l < selfwords.length; l++) {
                    for(int r = 0; r < this.words.length; r++) {
                        if(!rmark[r] && assign[l] < 0 && sim[l][r] > pmax && sim[l][r] > min_similarity) {
                            lmax = l;
                            rmax = r;
                            pmax = sim[l][r];
                        }
                    }
                }

                if(lmax >= 0) {
                    found = true;
                    assign[lmax] = rmax;
                    rmark[rmax] = true;
                } else {
                    found = false;
                }
            } while(found);

            this.res = 1.0;
            for(int l = 0; l < selfwords.length; l++) {
                if(assign[l] >= 0) {
                    this.res *= sim[l][assign[l]];
                } else {
                    this.res *= UNASSIGNED_COST;
                }
            }
            this.res = Math.pow(res, 1.0 / selfwords.length);
        }
        
        void _try_assoc(Word[] selfwords, int l, double res, double mean, int match_sz) {
            int r = 0;
            if(l == this.min_nb) {
                while(res > min_similarity && r < this.max_nb) {
                    if(!this.assoc[r]) {
                        int tmp = r;
                        while(tmp < this.max_nb && this.words[tmp].pout == null) {
                            tmp += 1;
                        }
                        if(tmp < this.max_nb) {
                            if(this.words[tmp].pout < res) {
                                res = this.words[tmp].pout;
                            }
                            mean += this.words[tmp].pout;
                        }
                    }
                    r += 1;
                }
                if(res > min_similarity) {
                    res *= match_sz;
                    if(res > this.res) {
                        this.res = res;
                        this.mean = mean * match_sz;
                        this.best_l2r = this.t_l2r.subList(0, this.min_nb);
                    }
                }
            } else {
                while(r < this.max_nb) {
                    if(!this.assoc[r]) {
                        double p = _get_proba(selfwords, l, r);
                        if(p > min_similarity) {
                            this.assoc[r] = true;
                            int match = min(len(selfwords[l].word), 
                                            len(this.words[r].word));
//                            self._try_assoc(l + 1, p if p < res else res, 
//                                                mean + p, match_sz + match);
                            _try_assoc(selfwords, l+1, p < res ? p : res, mean +p, match_sz + match);
                            this.assoc[r] = false;
                        }

                        if(selfwords[l].pout == null 
                                && this.words[r].pout != null) {
                            Word word = new Word(selfwords[l], null, selfwords[l + 1]);
                            p = _word_similarity(selfwords, new Object[] {l, word }, new Object[] {r});
                            if(p > min_similarity) {
                                int match=min(len(word.word),len(this.words[r].word));
                                this.assoc[r] = true;
                                _try_assoc(selfwords, l + 2, p < res ? p : res,
                                                   mean + p * 2, match_sz + match);
                                this.assoc[r] = false;
                            }
                        } else if(selfwords[l].pout != null 
                                && this.words[r].pout == null 
                                && !this.assoc[r + 1]) {
                            Word word = new Word(this.words[r], null, this.words[r + 1]);
                            p = _word_similarity(selfwords, new Object [] {l}, new Object[] {r, word});
                            if(p > min_similarity) {
                                int match = min(len(selfwords[l].word),len(word.word));
                                this.assoc[r] = true;
                                this.assoc[r + 1] = true;
                                _try_assoc(selfwords, l + 1, p < res ? p : res,
                                                   mean + p * 2, match_sz + match);
                                this.assoc[r] = false;
                                this.assoc[r + 1] = false;
                            }
                        }
                    }
                    r += 1;
                }
            }
        }

        double _get_proba(Word[] selfwords, int i, int j) {
            int ind = i * this.max_nb + j;
            double res = this.probas[ind];
            if(res < -.5) {
                res = _word_similarity(selfwords, new Object[] {i}, new Object[] {j});
                this.probas[ind] = res;
            }
            return res;
        }

        double _word_similarity(Word[] selfwords, Object[] lhs_index, Object[] rhs_index) {
            //lhs = selfwords[lhs_index[0]] if len(lhs_index) == 1 else lhs_index[1];
            //rhs = this.words[rhs_index[0]] if len(rhs_index)==1 else rhs_index[1];
            Word lhs = lhs_index.length == 1 ? selfwords[(Integer)lhs_index[0]] : (Word)lhs_index[1];
            Word rhs = rhs_index.length == 1 ? this.words[(Integer)rhs_index[0]] : (Word)rhs_index[1];

            double proba = 1. - (lhs.prob_firstname + rhs.prob_firstname - 2 * 
                          lhs.prob_firstname * rhs.prob_firstname) 
                * (lhs.prob_lastname + rhs.prob_lastname - 2 * lhs.prob_lastname * rhs.prob_lastname);
            double nick = -1.;

            int l1 = min(len(lhs.word), len(rhs.word));
            int l2 = max(len(lhs.word), len(rhs.word));
            if(lhs.initial() || rhs.initial()) {
                if(!char_equal(lhs.word[0], rhs.word[0]) 
                        || (l1 > 1 && !char_equal(lhs.word[1], rhs.word[1]))) {
                    proba = .0;
                }
            } else {
                // bonus for long words
                proba = 1.0 - (1.0 - proba) / (Math.pow(((l1 + l2 - 4.0) / 25.0), 2) + 1.);

                if(proba > min_similarity) {
                    int i = 0;
                    while(i < l1 && lhs.word[i] == rhs.word[i]
                            && lhs.word[i] == '_') {
                        i += 1;
                    }

                    if(i < l2) {
                        // if spellings don't match
                        //if(lhs.word in first_nick.get(rhs.word, [])) {
                        if(first_nick_get(rhs.word, Collections.EMPTY_LIST).contains(new String(lhs.word))) {
                            // if one is a common nickname for the other
                            nick = min(1. - lhs.prob_lastname, 1. - rhs.prob_lastname);
                        }

                        double mx = Math.sqrt(l1) - .2; // maximum distance allowed between words
                        double dist = l2 - l1;
                        if(i < l1) {
                            dist = mx - close_to(Arrays.copyOfRange(lhs.word, i, lhs.word.length), 
                                                 Arrays.copyOfRange(rhs.word, i, rhs.word.length), mx); // Edit distance
                        }
                        proba *= (1. - dist / mx);
                    }
                }
            }

            double ln = lhs.prob_lastname + rhs.prob_lastname;

            if(nick > proba) {
                proba = nick;
                ln = .0;
            }

            if(proba > min_similarity) {
                int i = (Integer)lhs_index[0];
                int j = (Integer)rhs_index[0];
                this.t_l2r.set(i, new L2R(selfwords, 
                    lhs_index.length == 1 ? new int[] {i} : new int[] {i, i+1},
                    this.words,
                    rhs_index.length == 1 ? new int[] {j} : new int[] {j, j+1},
                    ln));
            }

            return proba;
        }

        boolean char_equal(char c1, char c2) {
            return c1 == c2 || c1 == '_' || c2 == '_';
        }

        double close_to(char[] lhs_word, char[] rhs_word, double mx) {
            return _close_to(lhs_word, rhs_word, mx, 0, 0);
        }

        static final double INS_DEL_COST = 1.0;
        static final double TRANSPOS_COST = 1.0;
        static final double SUBSTIT_COST = 1.0;
        
        double _close_to(char[] lhs_word, char[] rhs_word, double max_dist, int lhs_off, int rhs_off) {
            double best_dist = -42.;

            while(lhs_off < len(lhs_word) && rhs_off < len(rhs_word) 
                    && lhs_word[lhs_off] == rhs_word[rhs_off] 
                    && lhs_word[lhs_off] != '_' && lhs_word[lhs_off] != '.') {
                lhs_off += 1;
                rhs_off += 1;
            }

            if(lhs_off < len(lhs_word) && rhs_off < len(rhs_word)) {
                if(lhs_word[lhs_off] == '.') {
                    return max_close_to(lhs_word, rhs_word, max_dist, lhs_off + 1, rhs_off, rhs_word);
                    //return max(_close_to(lhs_word, rhs_word, max_dist, lhs_off + 1, i) \
                    //               for i in xrange(rhs_off, len(rhs_word)));
                }
                if(rhs_word[rhs_off] == '.') {
                    return max_close_to(lhs_word, rhs_word, max_dist, rhs_off + 1, lhs_off, lhs_word);
                    //return max(_close_to(lhs_word, rhs_word, max_dist, i, rhs_off + 1) \
                    //               for i in xrange(lhs_off, len(lhs_word)));
                }

                double tmp_dist = max_dist;
                if(lhs_word[lhs_off] != '_') {
                    tmp_dist -= INS_DEL_COST;
                }
                if(tmp_dist >= .0) {
                    best_dist = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off + 1, rhs_off);
                }

                tmp_dist = max_dist;
                if(rhs_word[rhs_off] != '_') {
                    tmp_dist -= INS_DEL_COST;
                }
                if(tmp_dist >= .0) {
                    best_dist = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off, rhs_off + 1);
                }

                double chdist = char_equal(lhs_word[lhs_off], rhs_word[rhs_off]) ? 0.0 : 1.0;

                if(lhs_off < len(lhs_word) - 1 && rhs_off < len(rhs_word) - 1) {
                    tmp_dist = max_dist - chdist * TRANSPOS_COST;
                    if(tmp_dist >= .0 
                            && (char_equal(lhs_word[lhs_off], 
                                                rhs_word[rhs_off + 1]) 
                                     && char_equal(lhs_word[lhs_off + 1], 
                                                        rhs_word[rhs_off]))) {
                        res = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off + 2, rhs_off + 2);
                        if(res > best_dist) {
                            best_dist = res;
                        }
                    }
                }

                tmp_dist = max_dist - chdist * SUBSTIT_COST;
                if(tmp_dist >= .0) {
                    res = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off + 1, rhs_off + 1);
                    if(res > best_dist) {
                        best_dist = res;
                    }
                }
            } else {
                best_dist = max_dist 
                    - (len(lhs_word) + len(rhs_word) - lhs_off - rhs_off) 
                    * INS_DEL_COST;
            }

            return best_dist;
        }

        Set<String> first_nick_get(char[] word, List EMPTY_LIST) {
            String s = new String(word);
            Set<String> ss = NickNames.first_nicks.get(s);
            if(ss == null) {
                return Collections.EMPTY_SET;
            } else {
                 return ss;
            }
        }

        double max_close_to(char[] lhs_word, char[] rhs_word, double max_dist, int rhs_offp1, int lhs_off, char[] lhs_word0) {
                     //return max(_close_to(lhs_word, rhs_word, max_dist, i, rhs_off + 1) \
                    //               for i in xrange(lhs_off, len(lhs_word)));
            double max = Double.NEGATIVE_INFINITY;
            for(int i = lhs_off; i < len(lhs_word0); i++) {
               double d = _close_to(lhs_word, rhs_word, max_dist, i,  rhs_offp1);
               if(d > max) {
                   max = d;
               }
            }
            return max;
        }
            
        static class RuleMatcher {
            static Map<String, double[][]> _static_rules = new HashMap<>();
//# token types:
            static final String _i = "I"; /// initial
            static final String _n = "4"; /// number
            static final String _s = "s"; /// standard word
            static final String _u = "U"; /// upper case
            
            static {
                _static_rules.put("s", new double[][] {
                    {.0, .9},
                });
                _static_rules.put("U", new double[][] {
                    {.0, .9}
                });
                _static_rules.put("s s", new double[][] {
                    {.5, .5},
                    {.5, .5}
                });
                _static_rules.put("s,s", new double[][] {
                    {.0, .9},
                    {.9, .0}
                });
                _static_rules.put("s U", new double[][] {
                    {.9, 0.},
                    {0., .9}
                });
                _static_rules.put("s I s", new double[][] {
                    {.7, .3},
                    {.4, .1},
                    {.3, .7}
                });
                _static_rules.put("s I s s", new double[][] {
                    {.7, .3},
                    {.4, .1},
                    {.8, .1},
                    {.5, .5}
                });
                _static_rules.put("I s", new double[][] {
                    {.9, 0.},
                    {0., .9}
                });
                _static_rules.put("I s s", new double[][] {
                    {.9, 0.},
                    {.5, .5},
                    {0., .5}
                });
                _static_rules.put("s s I", new double[][] {
                    {.0, .5},
                    {.5, .5},
                    {.5, .0}
                });
                _static_rules.put("I I s", new double[][] {
                    {.9, 0.},
                    {.5, .2},
                    {.1, .9}
                });
                _static_rules.put("I I I s", new double[][] {
                    {.9, 0.},
                    {.5, .2},
                    {.2, .2},
                    {0., .9}
                });
                _static_rules.put("s I", new double[][] {
                    {.1, .9},
                    {.9, 0.}
                });
                _static_rules.put("s I I", new double[][] {
                    {.0, .9},
                    {.8, .1},
                    {.4, .1}
                });
                _static_rules.put("U s", new double[][] {
                    {.0, .9},
                    {.9, .0}
                });
                _static_rules.put("U,s", new double[][] {
                    {.0, .9},
                    {.9, .0}
                });
                _static_rules.put("U s s", new double[][] {
                    {.0, .9},
                    {.9, .0},
                    {.5, .0}
                });
                _static_rules.put("U,s s", new double[][] {
                    {.0, .9},
                    {.9, .0},
                    {.5, .0}
                });
                
            }

            private static boolean containsMostlyLowerCase(String c) {
                int lc = 0, nlc = 0;
                for(int i = 0; i < c.length(); i++) 
                    if(Character.isLowerCase(c.charAt(i)))
                        lc++;
                            else
                        nlc++;
                return lc > nlc;
            }

            static class probas_for {
                public final double[][] probas;
                public final String[] words;

                public probas_for(double[][] probas, String[] words) {
                    this.probas = probas;
                    this.words = words;
                }
            }
            
            /** Parse name into a rule. Return tuple with list of words, probability of 
             * (first name, second name) and numbers in name. probability will be None if name 
             * is invalid */
            public static probas_for get_probas_for(String name) {
               
                //words, rule, number = self._parse_name(name)
                Object[] wrn = _parse_name(name);
                if(wrn == null) {
                    System.err.println("Invalid name: " + name);
                    return null;
                }
                String[] words = (String[])wrn[0];
                String rule = (String)wrn[1];
                //Object number = wrn[2];
                double[][] probas;

                if(rule == null || !(rule.contains(_u) || rule.contains(_s))) {
                    System.err.println("Invalid name: " + name);
                    return null;
                }
                
                if(_static_rules.containsKey(rule)) {
                    probas = _static_rules.get(rule);
                } else {
                    List<double[]> _probas = new ArrayList<>();
                    
                    boolean relevant = (rule.contains(_s) || rule.contains(_i)) && rule.charAt(0) != rule.charAt(rule.length() - 1);
                    boolean comma = rule.contains(",");
                    //# Barry: This block I can't explain... might have been left there for 
                    //# testing and never removed 
                    for(int c = 0; c < words.length; c++) {
                        // Special rule for uppercase... c*2 is the token in the rule 
                        // corresponding to the word 
                        if(comma || (relevant && rule.charAt(c * 2) == _u.charAt(0))) {
                            _probas.add(new double[] {.1, .6});
                            if(rule.length() == c*2 && rule.charAt(c * 2 + 1) == ',') {
                                // Stop from entering again
                                comma = false;
                                relevant = false;
                            }
                        } else {
                            _probas.add(new double[] {.3, .3});
                        }
                    }
                    probas = _probas.toArray(new double[_probas.size()][]);
                }
                return new probas_for(probas, words);
            }

            static final Pattern RE_NAME = Pattern.compile("([^,\\s]+)([,\\s]+)?");
            /** Convert a name string to a sequence of tokens */
            static String[] _tokenize_name(String name) {
                Matcher m = RE_NAME.matcher(name);
                List<String> tokens = new ArrayList<>();
                while(m.find()) {
                    String w = m.group(1);
                    String delim = m.group(2);
                    tokens.add(w);
                    if(delim != null && delim.length() > 0) {
                        if(delim.contains(",")) {
                            tokens.add(",");
                        } else {
                            tokens.add(" ");
                        }
                    }
                }
                return tokens.toArray(new String[tokens.size()]);
            }
                
            /**
                Tokenize a name and convert into a rule. Returns a tuple containing:
                words: Token stream created from 'name'.
                rule: The rule that matches the tokenized name, with probabilities. Will be None
                      if name is invalid.
                number: A number that occurs in the name, if any.
                'rule' argument will be None if invalid name */
                         
            static Object[] _parse_name(String name) {
               List<String> words = new ArrayList<>();
               StringBuilder rule = new StringBuilder();
               // e.g. with name 'Barry Coughlan 1', 'num' will be 1, this occurs
               // because a PDF may contain footnote/reference numbers beside the name.
               Object number = null;
               for(String c : _tokenize_name(name)) {
                   final String t;
                   if(c.equals(" ") || c.equals(".")) {
                       t = c;
                   } else if(c.length() == 1 || c.contains(".") || c.charAt(1) == '-') {
                       t = _i;
                   } else if(containsMostlyLowerCase(c) || c.contains("_")) {
                       t = _s;
                       c = c.toLowerCase();
                   } else if(c.toUpperCase().endsWith(c)) {
                        t = _u;
                        c = c.toLowerCase();
                   } else if(isdigit(c)) {
                        t = _n;
                        if(number != null) {
                            //More than one number in name, name is invalid.
                            return null;
                        }
                        number = Integer.parseInt(c);
                   } else {
                       System.err.printf("Unexpected token in name \"%s\": %s\n", name, c);
                       return null;
                   }
                    
                   rule.append(t);
                   if(t.equals(_i) || t.equals(_u) || t.equals(_s)) {
                       words.add(c);
                   }
               }
                           
               return new Object[] { words.toArray(new String[words.size()]), rule.toString() };
            }

            static boolean isdigit(String c) {
                for(int i = 0; i < c.length(); i++) 
                    if(!Character.isDigit(c.charAt(i)))
                        return false;
                return true;
            }
        }
 
        
        static class L2R {
            Word[] selfwords;
            int[] i1;
            Word[] thiswords;
            int[] i2;
            double ln;

            public L2R(Word[] selfwords, int[] i1, Word[] thiswords, int[] i2, double ln) {
                this.selfwords = selfwords;
                this.i1 = i1;
                this.thiswords = thiswords;
                this.i2 = i2;
                this.ln = ln;
            }

        }

           
    }
    }
