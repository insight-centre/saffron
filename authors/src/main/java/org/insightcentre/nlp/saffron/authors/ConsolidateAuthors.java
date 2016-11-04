package org.insightcentre.nlp.saffron.authors;

import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Author;

/**
 *
 * @author Hugues Lerebours Pigeonnière <hugues@lereboursp.net>
 * @author John McCrae <john@mccr.ae>
 */
public class ConsolidateAuthors {
    public static final double min_similarity = 0.3;
    
    public static Map<Author, Set<Author>> consolidate(List<Author> authors) {
        
        
        for(Author author : authors) {
            Set<Author> similar = new HashSet<>();
            for(Author author2 : authors) {
                if(isSimilar(author, author2)) {
                    similar.add(author2);
                }

            }
        }
        return null;
    }

    private static boolean isSimilar(Author author, Author author2) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static class Word {
        Double pout;
        char[] word;
        private double prob_firstname;
        private double prob_lastname;

        private char[] append(char[] s, char t) {
            char[] s2 = new char[s.length + 1];
            System.arraycopy(s, 0, s2, 0, s.length);
            s2[s.length] = t;
            return s2;
        }
        private char[] append(char[] s, char[] t) {
            char[] s2 = new char[s.length + t.length];
            System.arraycopy(s, 0, s2, 0, s.length);
            System.arraycopy(t, 0, s2, s.length, t.length);
            return s2;
        }
         
        public Word(Word word, Double[] prob_first_last, Word opt2) {
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
        
        private boolean initial() {
            return (this.word.length == 1 
                 || (this.word.length == 3 && this.word[2] == '.'));
        }

    }

    private static class PyAuthorSim {
        int max_nb;
        int min_nb;
        Word[] words;
        double[] probas;
        boolean[] assoc;
        List<L2R> t_l2r = new ArrayList<>();
        List<L2R> best_l2r = new ArrayList<>();
        double res;
        double mean;
        public int sht_len;
        static final double min_similarity = 0.3;


        public double[] range(int i, int j) {
            double[] r = new double[j-i];
            for(int k = i; k < i; k++) {
                r[k-i] = k;
            }
            return r;
        }
                
        public static int len(Object[] a) { return a.length; }
        public static int len(char[] a) { return a.length; }

        public boolean similar(Word[] selfwords, Word[] authorwords) {
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

            _try_assoc(selfwords, 0, 42., .0, 0);

            this.res /= this.sht_len;
            if(this.res > .0) {
                this.mean /= this.max_nb * this.sht_len;
                if(this.min_nb > 2) {
                    this.res = 1. - (1. - this.res) * 5. 
                        / (this.min_nb + this.max_nb);
                }

                double sim = (this.res + this.mean) / 2.;

                if(sim > min_similarity) {
                    // link the two names
                    //Link lk = new Link(sim, this.best_l2r);
                    //links[author] = lk;
                    //author.links[self] = lk;

                    return true;
                }
            }

            return false;
        }


        //#################
        //### internals

        void _try_assoc(Word[] selfwords, int l, double res, double mean, int match_sz) {
            int r = 0;
            if(l == this.min_nb) {
                while(res > min_similarity && r < this.max_nb) {
                    if(!this.assoc[r]) {
                        int tmp = r;
                        while(this.words[tmp].pout == null) {
                            tmp += 1;
                        }
                        if(this.words[tmp].pout < res) {
                            res = this.words[tmp].pout;
                        }
                        mean += this.words[tmp].pout;
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
            res = this.probas[ind];
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
            Word rhs = rhs_index.length == 1 ? selfwords[(Integer)rhs_index[0]] : (Word)rhs_index[1];

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

        private boolean char_equal(char c1, char c2) {
            return c1 == c2 || c1 == '_' || c2 == '_';
        }

        private double close_to(char[] lhs_word, char[] rhs_word, double mx) {
            return _close_to(lhs_word, rhs_word, mx, 0, 0);
        }

        private static final double INS_DEL_COST = 1.0;
        private static final double TRANSPOS_COST = 1.0;
        private static final double SUBSTIT_COST = 1.0;
        
        private double _close_to(char[] lhs_word, char[] rhs_word, double max_dist, int lhs_off, int rhs_off) {
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

        private Set<String> first_nick_get(char[] word, List EMPTY_LIST) {
            String s = new String(word);
            Set<String> ss = NickNames.first_nicks.get(s);
            if(ss == null) {
                return Collections.EMPTY_SET;
            } else {
                 return ss;
            }
        }

        private double max_close_to(char[] lhs_word, char[] rhs_word, double max_dist, int rhs_offp1, int lhs_off, char[] lhs_word0) {
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
        
        private static class L2R {
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
