package org.insightcentre.nlp.saffron.authors.connect;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.AuthorTermConfiguration;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class ConnectAuthorTerm {
    private static class AT {
        public final String author;
        public final String term;

        public AT(String author, String term) {
            this.author = author;
            this.term = term;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.author);
            hash = 17 * hash + Objects.hashCode(this.term);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AT other = (AT) obj;
            if (!Objects.equals(this.author, other.author)) {
                return false;
            }
            if (!Objects.equals(this.term, other.term)) {
                return false;
            }
            return true;
        }
        
    }
    private final int top_n;

    public ConnectAuthorTerm(AuthorTermConfiguration config) {
        this.top_n = config.topN;
    }
    
    public Collection<AuthorTerm> connectResearchers(List<Term> terms, List<DocumentTerm> documentTerms,
        Iterable<Document> documents) {
        return connectResearchers(terms, documentTerms, documents, new DefaultSaffronListener());
    }
    
    public Collection<AuthorTerm> connectResearchers(List<Term> terms, List<DocumentTerm> documentTerms,
        Iterable<Document> documents, SaffronListener log) {

        Map<String, Document>     docById      = buildDocById(documents);
        Map<Author, List<String>> author2Term = buildAuthor2Term(documentTerms, docById, log);
        //Map<Author, List<String>> author2Doc   = buildAuthor2Doc(documentTerms, docById);
        Map<String, Term>        termById    = buildTermById(terms);

        Object2IntMap<AT>     occurrences = new Object2IntOpenHashMap<>();
        Object2IntMap<AT>     matches     = new Object2IntOpenHashMap<>();
        Object2IntMap<AT>     paper_count = new Object2IntOpenHashMap<>();
        Object2DoubleMap<AT>  tfirf       = new Object2DoubleOpenHashMap<>();
        countOccurrence(author2Term, termById, occurrences, matches, log);
        countTfirf(documentTerms, docById, paper_count, tfirf);

        List<AuthorTerm> ats = new ArrayList<>();
        for(Map.Entry<Author, List<String>> e : author2Term.entrySet()) {
            TreeSet<AuthorTerm> topN = new TreeSet<>(new Comparator<AuthorTerm>() {

                @Override
                public int compare(AuthorTerm arg0, AuthorTerm arg1) {
                    int i1 = Double.compare(arg0.getScore(), arg1.getScore());
                    if(i1 == 0) {
                        int i2 = arg0.getAuthorId().compareTo(arg1.getAuthorId());
                        if(i2 == 0) {
                            int i3 = arg0.getTermId().compareTo(arg1.getTermId());
                            if(i3 == 0) {
                                return arg0.hashCode() - arg1.hashCode();
                            }
                            return i3;
                        }
                        return i2;
                    }
                    return i1;
                }
            });
        
            for(String termString : e.getValue()) {
                AT atKey = new AT(e.getKey().id, termString);
                AuthorTerm at = new AuthorTerm();
                at.setAuthorId(e.getKey().id);
                at.setTermId(termString);
                at.setTfIrf(tfirf.getDouble(atKey));
                at.setMatches(matches.getInt(atKey));
                at.setOccurrences(occurrences.getInt(atKey));
                at.setPaperCount(paper_count.getInt(atKey));
                at.setScore(at.getTfIrf() * at.getPaperCount());
                at.setResearcherScore((double)at.getPaperCount() * Math.log(1 + at.getMatches()));
                if(topN.size() < top_n) {
                    topN.add(at);
                } else if(topN.size() >= top_n && at.getScore() > topN.first().getScore()) {
                    topN.pollFirst();
                    topN.add(at);
                }
            }
            ats.addAll(topN);
        }

        return ats;
        
    }

    private Map<Author, List<String>> buildAuthor2Term(List<DocumentTerm> documentTerms, Map<String, Document> docById,
            SaffronListener log) {
        Map<Author, List<String>> author2Term = new HashMap<>();
        for(DocumentTerm dt : documentTerms) {
            Document doc = docById.get(dt.getDocumentId());
            if(doc == null) {
                continue;
            }
            for(Author a : doc.authors) {
                if(!author2Term.containsKey(a)) 
                    author2Term.put(a, new ArrayList<String>());
                author2Term.get(a).add(dt.getTermString());
            }
        }
        return author2Term;
    }

    private Map<String, Term> buildTermById(List<Term> terms) {
        Map<String, Term> termById = new HashMap<>();
        for(Term term : terms)
            termById.put(term.getString(), term);
        return termById;
    }

    private Map<String, Document> buildDocById(Iterable<Document> documents) {
        Map<String, Document> docById = new HashMap<>();
        for(Document document : documents)
            docById.put(document.id, document);
        return docById;
    }

    private void countOccurrence(Map<Author, List<String>> author2Term, Map<String, Term> terms, Object2IntMap<AT> occurrences, 
            Object2IntMap<AT> matches, SaffronListener log) {
        for(Map.Entry<Author, List<String>> e : author2Term.entrySet()) {
            Author a = e.getKey();
            for(String termString : e.getValue()) {
                Term t = terms.get(termString);
                if(t == null) {
                    continue;
                }
                AT at = new AT(a.id, termString);
                occurrences.put(at, occurrences.getInt(at) + t.getOccurrences());
                matches.put(at, matches.getInt(at) + t.getMatches());
            }
        }
    }

    private void countTfirf(List<DocumentTerm> docTerms, Map<String, Document> docById, Object2IntMap<AT> paper_count, Object2DoubleMap<AT> tfirf) {
        for(DocumentTerm dt : docTerms) {
            Document doc = docById.get(dt.getDocumentId());
            for(Author a : doc.authors) {
                AT at = new AT(a.id, dt.getTermString());
                paper_count.put(at, paper_count.getInt(at) + 1);
                if(dt.getTfIdf() != null)
                    tfirf.put(at, tfirf.getDouble(at) + dt.getTfIdf());
            }
        }
    }


}
