package org.insightcentre.nlp.saffron.authors;

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
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.data.Author;

/**
 *
 * @author Hugues Lerebours Pigeonnière &lt;hugues@lereboursp.net&gt;
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class ConsolidateAuthors {

    public static final double min_similarity = 0.3;
    public static final double UNASSIGNED_COST = 0.1;

    public static Map<Author, Set<Author>> consolidate(Collection<Author> authors) {
        return consolidate(authors, new DefaultSaffronListener());
    }

    public static Map<Author, Set<Author>> consolidate(Collection<Author> authors, SaffronListener log) {
        Map<Author, Set<Author>> consolidated = new HashMap<>();
        Set<Author> marked = new HashSet<>();
        for (Author author : authors) {
            if (marked.contains(author)) {
                continue;
            }
            Set<Author> similar = new HashSet<>();
            similar.add(author);
            marked.add(author);
            for (Author author2 : authors) {
                if (!marked.contains(author2)) {
                    if (author.name != null && author2.name != null && isSimilar(author, author2)) {
                        similar.add(author2);
                        marked.add(author2);
                    }
                }
            }
            consolidated.put(_choose_author(similar), similar);
        }
        return consolidated;
    }

    public static boolean isSimilar(Author author, Author author2) {
        return new PyAuthorSim().similar(author.name, author2.name);
    }

    static Author _choose_author(Collection<Author> authors) {
        if (authors.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (authors.size() == 1) {
            return authors.iterator().next();
        }
        Author best = null;
        double bestScore = 0.0;
        for (Author author : authors) {
            PyAuthorSim.RuleMatcher.probas_for pa = PyAuthorSim.RuleMatcher.get_probas_for(author.name);
            double score = 0.0;
            if (pa != null) {
                for (int i = 0; i < pa.words.length; i++) {
                    double wordScore = (pa.words[i].length() - count(pa.words[i], '_'))
                            * (pa.probas[i][0] > pa.probas[i][1] ? pa.probas[i][0] : pa.probas[i][1]);
                    score += wordScore;
                }
                score = Math.pow(score, 1.0 / pa.words.length);
            }
            if (score > bestScore) {
                best = author;
                bestScore = score;
            }
        }
        Set<String> variants = new HashSet<>();
        for (Author author : authors) {
            if (!author.equals(best)) {
                variants.add(author.name);
            }
        }
        return new Author(best.id, best.name, variants);
    }

    static int count(String w, char c) {
        int count = 0;
        for (int i = 0; i < w.length(); i++) {
            if (c == w.charAt(i)) {
                count++;
            }
        }
        return count;
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
            for (int i = 0; i < s.length(); i++) {
                this.word[i] = s.charAt(i);
            }
            this.prob_firstname = prob_first_last[0];
            this.prob_lastname = prob_first_last[1];

        }

        public Word(Word word, double[] prob_first_last, Word opt2) {
            if (prob_first_last == null) {
                this.word = word.word;
                if (word.word.length == 1) {
                    this.word = append(this.word, '.');
                }
                this.word = append(this.word, opt2.word);
                if (opt2.word.length == 1) {
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
        static final double min_similarity = 0.3;

        public double[] range(int i, int j) {
            double[] r = new double[j - i];
            for (int k = i; k < j; k++) {
                r[k - i] = k;
            }
            return r;
        }

        public static int len(Object[] a) {
            return a.length;
        }

        public static int len(char[] a) {
            return a.length;
        }

        public boolean similar(String authorName1, String authorName2) {
            RuleMatcher.probas_for a1 = RuleMatcher.get_probas_for(authorName1);
            RuleMatcher.probas_for a2 = RuleMatcher.get_probas_for(authorName2);

            if (a1 == null || a2 == null) {
                return false;
            }
            Word[] w1 = makeWords(a1.words, a1.probas);
            Word[] w2 = makeWords(a2.words, a2.probas);

            return similar(w1, w2);
        }

        public static Word[] makeWords(String[] words, double[][] probas) {
            List<Word> _words = new ArrayList<>();

            //for p, word in enumerate(words):
            for (int p = 0; p < words.length; p++) {
                String word = words[p];
                int beg = 0;
                int l = word.length();
                double p_out = 1. - probas[p][0] - probas[p][1];
                boolean several = false;
                while (beg < l) {
                    int i = word.indexOf('-', beg);
                    if (i == -1) {
                        _words.add(new Word(word.subSequence(beg, l), probas[p], p_out));
                        beg = l;
                    } else {
                        if (!several) {
                            p_out = (p_out + .5) / 1.5;
                            several = true;
                        }
                        _words.add(new Word(word.subSequence(beg, i), probas[p], null));
                        beg = i + 1;
                    }
                }
            }
            return _words.toArray(new Word[_words.size()]);
        }

        public boolean similar(Word[] selfwords, Word[] authorwords) {
            if (selfwords.length > authorwords.length) {
                return similar(authorwords, selfwords);
            }
            this.max_nb = authorwords.length; // maximum number of words
            this.min_nb = selfwords.length; // minimum number of words
            this.words = authorwords;
            this.probas = range(-this.min_nb * this.max_nb, 0);

            double res = _simple_assoc(selfwords);

            return res > min_similarity;
        }

        double _simple_assoc(Word[] selfwords) {
            double[][] sim = new double[selfwords.length][this.words.length];
            for (int l = 0; l < selfwords.length; l++) {
                for (int r = 0; r < this.words.length; r++) {
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

                for (int l = 0; l < selfwords.length; l++) {
                    for (int r = 0; r < this.words.length; r++) {
                        if (!rmark[r] && assign[l] < 0 && sim[l][r] > pmax && sim[l][r] > min_similarity) {
                            lmax = l;
                            rmax = r;
                            pmax = sim[l][r];
                        }
                    }
                }

                if (lmax >= 0) {
                    found = true;
                    assign[lmax] = rmax;
                    rmark[rmax] = true;
                } else {
                    found = false;
                }
            } while (found);

            double res = 1.0;
            for (int l = 0; l < selfwords.length; l++) {
                if (assign[l] >= 0) {
                    res *= sim[l][assign[l]];
                } else {
                    res *= UNASSIGNED_COST;
                }
            }
            return Math.pow(res, 1.0 / selfwords.length);
        }

        double _get_proba(Word[] selfwords, int i, int j) {
            int ind = i * this.max_nb + j;
            double res = this.probas[ind];
            if (res < -.5) {
                res = _word_similarity(selfwords, new Object[]{i}, new Object[]{j});
                this.probas[ind] = res;
            }
            return res;
        }

        double _word_similarity(Word[] selfwords, Object[] lhs_index, Object[] rhs_index) {
            //lhs = selfwords[lhs_index[0]] if len(lhs_index) == 1 else lhs_index[1];
            //rhs = this.words[rhs_index[0]] if len(rhs_index)==1 else rhs_index[1];
            Word lhs = lhs_index.length == 1 ? selfwords[(Integer) lhs_index[0]] : (Word) lhs_index[1];
            Word rhs = rhs_index.length == 1 ? this.words[(Integer) rhs_index[0]] : (Word) rhs_index[1];

            double proba = 1. - (lhs.prob_firstname + rhs.prob_firstname - 2
                    * lhs.prob_firstname * rhs.prob_firstname)
                    * (lhs.prob_lastname + rhs.prob_lastname - 2 * lhs.prob_lastname * rhs.prob_lastname);
            double nick = -1.;

            int l1 = min(len(lhs.word), len(rhs.word));
            int l2 = max(len(lhs.word), len(rhs.word));
            if (lhs.initial() || rhs.initial()) {
                if (!char_equal(lhs.word[0], rhs.word[0])
                        || (l1 > 1 && !char_equal(lhs.word[1], rhs.word[1]))) {
                    proba = .0;
                }
            } else {
                // bonus for long words
                proba = 1.0 - (1.0 - proba) / (Math.pow(((l1 + l2 - 4.0) / 25.0), 2) + 1.);

                if (proba > min_similarity) {
                    int i = 0;
                    while (i < l1 && lhs.word[i] == rhs.word[i]
                            && lhs.word[i] == '_') {
                        i += 1;
                    }

                    if (i < l2) {
                        // if spellings don't match
                        //if(lhs.word in first_nick.get(rhs.word, [])) {
                        if (first_nick_get(rhs.word, Collections.EMPTY_LIST).contains(new String(lhs.word))) {
                            // if one is a common nickname for the other
                            nick = min(1. - lhs.prob_lastname, 1. - rhs.prob_lastname);
                        }

                        double mx = Math.sqrt(l1) - .2; // maximum distance allowed between words
                        double dist = l2 - l1;
                        if (i < l1) {
                            dist = mx - close_to(Arrays.copyOfRange(lhs.word, i, lhs.word.length),
                                    Arrays.copyOfRange(rhs.word, i, rhs.word.length), mx); // Edit distance
                        }
                        proba *= (1. - dist / mx);
                    }
                }
            }

            if (nick > proba) {
                proba = nick;
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

            while (lhs_off < len(lhs_word) && rhs_off < len(rhs_word)
                    && lhs_word[lhs_off] == rhs_word[rhs_off]
                    && lhs_word[lhs_off] != '_' && lhs_word[lhs_off] != '.') {
                lhs_off += 1;
                rhs_off += 1;
            }

            if (lhs_off < len(lhs_word) && rhs_off < len(rhs_word)) {
                if (lhs_word[lhs_off] == '.') {
                    return max_close_to(lhs_word, rhs_word, max_dist, lhs_off + 1, rhs_off, rhs_word);
                    //return max(_close_to(lhs_word, rhs_word, max_dist, lhs_off + 1, i) \
                    //               for i in xrange(rhs_off, len(rhs_word)));
                }
                if (rhs_word[rhs_off] == '.') {
                    return max_close_to(lhs_word, rhs_word, max_dist, rhs_off + 1, lhs_off, lhs_word);
                    //return max(_close_to(lhs_word, rhs_word, max_dist, i, rhs_off + 1) \
                    //               for i in xrange(lhs_off, len(lhs_word)));
                }

                double tmp_dist = max_dist;
                if (lhs_word[lhs_off] != '_') {
                    tmp_dist -= INS_DEL_COST;
                }
                if (tmp_dist >= .0) {
                    best_dist = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off + 1, rhs_off);
                }

                tmp_dist = max_dist;
                if (rhs_word[rhs_off] != '_') {
                    tmp_dist -= INS_DEL_COST;
                }
                if (tmp_dist >= .0) {
                    best_dist = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off, rhs_off + 1);
                }

                double chdist = char_equal(lhs_word[lhs_off], rhs_word[rhs_off]) ? 0.0 : 1.0;

                if (lhs_off < len(lhs_word) - 1 && rhs_off < len(rhs_word) - 1) {
                    tmp_dist = max_dist - chdist * TRANSPOS_COST;
                    if (tmp_dist >= .0
                            && (char_equal(lhs_word[lhs_off],
                                    rhs_word[rhs_off + 1])
                            && char_equal(lhs_word[lhs_off + 1],
                                    rhs_word[rhs_off]))) {
                        double res = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off + 2, rhs_off + 2);
                        if (res > best_dist) {
                            best_dist = res;
                        }
                    }
                }

                tmp_dist = max_dist - chdist * SUBSTIT_COST;
                if (tmp_dist >= .0) {
                    double res = _close_to(lhs_word, rhs_word, tmp_dist, lhs_off + 1, rhs_off + 1);
                    if (res > best_dist) {
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
            if (ss == null) {
                return Collections.EMPTY_SET;
            } else {
                return ss;
            }
        }

        double max_close_to(char[] lhs_word, char[] rhs_word, double max_dist, int rhs_offp1, int lhs_off, char[] lhs_word0) {
            //return max(_close_to(lhs_word, rhs_word, max_dist, i, rhs_off + 1) \
            //               for i in xrange(lhs_off, len(lhs_word)));
            double max = Double.NEGATIVE_INFINITY;
            for (int i = lhs_off; i < len(lhs_word0); i++) {
                double d = _close_to(lhs_word, rhs_word, max_dist, i, rhs_offp1);
                if (d > max) {
                    max = d;
                }
            }
            return max;
        }

        static class RuleMatcher {

            static final Map<String, double[][]> _static_rules = new HashMap<>();
//# token types:
            static final String _i = "I"; /// initial
            static final String _n = "4"; /// number
            static final String _s = "s"; /// standard word
            static final String _u = "U"; /// upper case

            static {
                _static_rules.put("s", new double[][]{
                    {.0, .9},});
                _static_rules.put("U", new double[][]{
                    {.0, .9}
                });
                _static_rules.put("s s", new double[][]{
                    {.5, .5},
                    {.5, .5}
                });
                _static_rules.put("s,s", new double[][]{
                    {.0, .9},
                    {.9, .0}
                });
                _static_rules.put("s U", new double[][]{
                    {.9, 0.},
                    {0., .9}
                });
                _static_rules.put("s I s", new double[][]{
                    {.7, .3},
                    {.4, .1},
                    {.3, .7}
                });
                _static_rules.put("s I s s", new double[][]{
                    {.7, .3},
                    {.4, .1},
                    {.8, .1},
                    {.5, .5}
                });
                _static_rules.put("I s", new double[][]{
                    {.9, 0.},
                    {0., .9}
                });
                _static_rules.put("I s s", new double[][]{
                    {.9, 0.},
                    {.5, .5},
                    {0., .5}
                });
                _static_rules.put("s s I", new double[][]{
                    {.0, .5},
                    {.5, .5},
                    {.5, .0}
                });
                _static_rules.put("I I s", new double[][]{
                    {.9, 0.},
                    {.5, .2},
                    {.1, .9}
                });
                _static_rules.put("I I I s", new double[][]{
                    {.9, 0.},
                    {.5, .2},
                    {.2, .2},
                    {0., .9}
                });
                _static_rules.put("s I", new double[][]{
                    {.1, .9},
                    {.9, 0.}
                });
                _static_rules.put("s I I", new double[][]{
                    {.0, .9},
                    {.8, .1},
                    {.4, .1}
                });
                _static_rules.put("U s", new double[][]{
                    {.0, .9},
                    {.9, .0}
                });
                _static_rules.put("U,s", new double[][]{
                    {.0, .9},
                    {.9, .0}
                });
                _static_rules.put("U s s", new double[][]{
                    {.0, .9},
                    {.9, .0},
                    {.5, .0}
                });
                _static_rules.put("U,s s", new double[][]{
                    {.0, .9},
                    {.9, .0},
                    {.5, .0}
                });

            }

            private static boolean containsMostlyLowerCase(String c) {
                int lc = 0, nlc = 0;
                for (int i = 0; i < c.length(); i++) {
                    if (Character.isLowerCase(c.charAt(i))) {
                        lc++;
                    } else {
                        nlc++;
                    }
                }
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

            /**
             * Parse name into a rule. Return tuple with list of words,
             * probability of (first name, second name) and numbers in name.
             * probability will be None if name is invalid
             */
            public static probas_for get_probas_for(String name) {

                //words, rule, number = self._parse_name(name)
                Object[] wrn = _parse_name(name);
                if (wrn == null) {
                    //System.err.println("Invalid name: " + name);
                    return null;
                }
                String[] words = (String[]) wrn[0];
                String rule = (String) wrn[1];
                //Object number = wrn[2];
                double[][] probas;

                if (rule == null || !(rule.contains(_u) || rule.contains(_s))) {
                    //System.err.println("Invalid name: " + name);
                    return null;
                }

                if (_static_rules.containsKey(rule)) {
                    probas = _static_rules.get(rule);
                } else {
                    List<double[]> _probas = new ArrayList<>();

                    boolean relevant = (rule.contains(_s) || rule.contains(_i)) && rule.charAt(0) != rule.charAt(rule.length() - 1);
                    boolean comma = rule.contains(",");
                    //# Barry: This block I can't explain... might have been left there for 
                    //# testing and never removed 
                    for (int c = 0; c < words.length; c++) {
                        // Special rule for uppercase... c*2 is the token in the rule 
                        // corresponding to the word 
                        if (comma || (relevant && c * 2 < rule.length() && rule.charAt(c * 2) == _u.charAt(0))) {
                            _probas.add(new double[]{.1, .6});
                            if (rule.length() == c * 2 && rule.charAt(c * 2 + 1) == ',') {
                                // Stop from entering again
                                comma = false;
                                relevant = false;
                            }
                        } else {
                            _probas.add(new double[]{.3, .3});
                        }
                    }
                    probas = _probas.toArray(new double[_probas.size()][]);
                }
                return new probas_for(probas, words);
            }

            static final Pattern RE_NAME = Pattern.compile("([^,\\s]+)([,\\s]+)?");

            /**
             * Convert a name string to a sequence of tokens
             */
            static String[] _tokenize_name(String name) {
                String[] name_elems = name.split(", ");
                if(name_elems.length == 2) {
                    name = name_elems[1] + " " + name_elems[0];
                }
                Matcher m = RE_NAME.matcher(name);
                List<String> tokens = new ArrayList<>();
                while (m.find()) {
                    String w = m.group(1);
                    String delim = m.group(2);
                    tokens.add(w);
                    if (delim != null && delim.length() > 0) {
                        if (delim.contains(",")) {
                            tokens.add(",");
                        } else {
                            tokens.add(" ");
                        }
                    }
                }
                return tokens.toArray(new String[tokens.size()]);
            }

            /**
             * Tokenize a name and convert into a rule. Returns a tuple
             * containing: words: Token stream created from 'name'. rule: The
             * rule that matches the tokenized name, with probabilities. Will be
             * None if name is invalid. number: A number that occurs in the
             * name, if any. 'rule' argument will be None if invalid name
             */
            static Object[] _parse_name(String name) {
                List<String> words = new ArrayList<>();
                StringBuilder rule = new StringBuilder();
                // e.g. with name 'Barry Coughlan 1', 'num' will be 1, this occurs
                // because a PDF may contain footnote/reference numbers beside the name.
                Object number = null;
                for (String c : _tokenize_name(name)) {
                    final String t;
                    if (c.equals(" ") || c.equals(".")) {
                        t = c;
                    } else if (c.length() == 1 || c.contains(".") || c.charAt(1) == '-') {
                        t = _i;
                    } else if (containsMostlyLowerCase(c) || c.contains("_")) {
                        t = _s;
                        c = c.toLowerCase();
                    } else if (c.toUpperCase().endsWith(c)) {
                        t = _u;
                        c = c.toLowerCase();
                    } else if (isdigit(c)) {
                        t = _n;
                        if (number != null) {
                            //More than one number in name, name is invalid.
                            return null;
                        }
                        number = Integer.parseInt(c);
                    } else {
                        //System.err.printf("Unexpected token in name \"%s\": %s\n", name, c);
                        return null;
                    }

                    rule.append(t);
                    if (t.equals(_i) || t.equals(_u) || t.equals(_s)) {
                        words.add(c);
                    }
                }

                return new Object[]{words.toArray(new String[words.size()]), rule.toString()};
            }

            static boolean isdigit(String c) {
                for (int i = 0; i < c.length(); i++) {
                    if (!Character.isDigit(c.charAt(i))) {
                        return false;
                    }
                }
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
