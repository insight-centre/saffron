package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;

/**
 * All the data generated during the run of Saffron that is exposed by the Web
 * interface
 *
 * @author John McCrae <john@mccr.ae>
 */
public class SaffronData {

    private Taxonomy taxonomy;
    private List<AuthorAuthor> authorSim;
    private List<TermTerm> termSim;
    private List<AuthorTerm> authorTerms;
    private List<DocumentTerm> docTerms;
    private HashMap<String, Term> terms;
    private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
    private HashMap<String, List<TermTerm>> termByTerm1, termByTerm2;
    private HashMap<String, List<DocumentTerm>> docByTerm, termByDoc;
    private HashMap<String, List<AuthorTerm>> authorByTerm, termByAuthor;
    private List<String> termsSorted;
    private HashMap<String, Document> corpus;
    private HashMap<String, List<Document>> corpusByAuthor;
    private HashMap<String, Author> authors;
    private HashMap<String, IntList> taxoMap;
    private DocumentSearcher searcher;

    public Taxonomy getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
        this.taxoMap = getTaxoLocations(taxonomy);
    }

    public List<AuthorAuthor> getAuthorSim() {
        return authorSim;
    }

    public void setAuthorSim(List<AuthorAuthor> authorSim) {
        authorByAuthor1 = new HashMap<>();
        authorByAuthor2 = new HashMap<>();
        for (AuthorAuthor aa : authorSim) {
            if (!authorByAuthor1.containsKey(aa.author1_id)) {
                authorByAuthor1.put(aa.author1_id, new ArrayList<AuthorAuthor>());
            }
            authorByAuthor1.get(aa.author1_id).add(aa);
            if (!authorByAuthor2.containsKey(aa.author2_id)) {
                authorByAuthor2.put(aa.author2_id, new ArrayList<AuthorAuthor>());
            }
            authorByAuthor2.get(aa.author2_id).add(aa);
        }

        this.authorSim = authorSim;
    }

    public List<AuthorAuthor> getAuthorSimByAuthor1(String author1) {
        List<AuthorAuthor> aas = authorByAuthor1.get(author1);
        return aas == null ? Collections.EMPTY_LIST : aas;
    }

    public List<AuthorAuthor> getAuthorSimByAuthor2(String author2) {
        List<AuthorAuthor> aas = authorByAuthor2.get(author2);
        return aas == null ? Collections.EMPTY_LIST : aas;
    }

    public List<Author> authorAuthorToAuthor1(List<AuthorAuthor> aas) {
        List<Author> as = new ArrayList<>();
        for(AuthorAuthor aa : aas) {
            Author a = getAuthor(aa.author1_id);
            if(a != null) {
                as.add(a);
            }
        }
        return as;
    }


    public List<Author> authorAuthorToAuthor2(List<AuthorAuthor> aas) {
        List<Author> as = new ArrayList<>();
        for(AuthorAuthor aa : aas) {
            Author a = getAuthor(aa.author2_id);
            if(a != null) {
                as.add(a);
            }
        }
        return as;
    }

    public List<AuthorTerm> getAuthorTerms() {
        return authorTerms;
    }

    public void setAuthorTerms(Collection<AuthorTerm> authorTerms) {
        authorByTerm = new HashMap<>();
        termByAuthor = new HashMap<>();
        for (AuthorTerm at : authorTerms) {
            if (!authorByTerm.containsKey(at.getTermId())) {
                authorByTerm.put(at.getTermId(), new ArrayList<AuthorTerm>());
            }
            authorByTerm.get(at.getTermId()).add(at);
            if (!termByAuthor.containsKey(at.getAuthorId())) {
                termByAuthor.put(at.getAuthorId(), new ArrayList<AuthorTerm>());
            }
            termByAuthor.get(at.getAuthorId()).add(at);
        }
        this.authorTerms = new ArrayList<>(authorTerms);
    }

    public List<AuthorTerm> getAuthorByTerm(String term) {
        List<AuthorTerm> ats = authorByTerm.get(term);
        return ats == null ? Collections.EMPTY_LIST : ats;
    }

    public List<Author> authorTermsToAuthors(List<AuthorTerm> ats) {
        List<Author> authors = new ArrayList<>();
        for (AuthorTerm at : ats) {
            Author a = getAuthor(at.getAuthorId());
            if (a != null) {
                authors.add(a);
            }
        }
        return authors;
    }

    public List<AuthorTerm> getTermByAuthor(String author) {
        List<AuthorTerm> ats = termByAuthor.get(author);
        return ats == null ? Collections.EMPTY_LIST : ats;
    }

    public List<DocumentTerm> getDocTerms() {
        return docTerms;
    }

    public void setDocTerms(List<DocumentTerm> docTerms) {
        docByTerm = new HashMap<>();
        termByDoc = new HashMap<>();
        for (DocumentTerm dt : docTerms) {
            if (!docByTerm.containsKey(dt.getTermString())) {
                docByTerm.put(dt.getTermString(), new ArrayList<DocumentTerm>());
            }
            docByTerm.get(dt.getTermString()).add(dt);
            if (!termByDoc.containsKey(dt.getDocumentId())) {
                termByDoc.put(dt.getDocumentId(), new ArrayList<DocumentTerm>());
            }
            termByDoc.get(dt.getDocumentId()).add(dt);
        }
        this.docTerms = docTerms;
    }

    public List<Document> getDocByTerm(String term) {
        final List<DocumentTerm> dts = docByTerm.get(term);
        if (dts == null) {
            return Collections.EMPTY_LIST;
        } else {
            final List<Document> docs = new ArrayList<>();
            for (DocumentTerm dt : dts) {
                Document d = corpus.get(dt.getDocumentId());
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
    }

    public List<DocumentTerm> getTermByDoc(String doc) {
        List<DocumentTerm> dts = termByDoc.get(doc);
        if (dts == null) {
            return Collections.EMPTY_LIST;
        } else {
            return dts;
        }
    }

    public Collection<String> getTopTerms(int from, int to) {
        if(from < termsSorted.size() && to <= termsSorted.size()) {
            return termsSorted.subList(from, to);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public Term getTerm(String term) {
        return terms.get(term);
    }

    public void setTerms(Collection<Term> _terms) {
        this.terms = new HashMap<>();
        this.termsSorted = new ArrayList<>();
        for (Term t : _terms) {
            this.terms.put(t.getString(), t);
            this.termsSorted.add(t.getString());
        }
        this.termsSorted.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (terms.containsKey(o1) && terms.containsKey(o2)) {
                    double wt1 = terms.get(o1).getScore();
                    double wt2 = terms.get(o2).getScore();
                    if (wt1 > wt2) {
                        return -1;
                    } else if (wt2 > wt1) {
                        return +1;
                    }
                }
                return o1.compareTo(o2);
            }
        });
    }

    public List<TermTerm> getTermSim() {
        return termSim;
    }

    public void setTermSim(List<TermTerm> termSim) {
        termByTerm1 = new HashMap<>();
        termByTerm2 = new HashMap<>();
        for (TermTerm tt : termSim) {
            if (!termByTerm1.containsKey(tt.getTerm1())) {
                termByTerm1.put(tt.getTerm1(), new ArrayList<TermTerm>());
            }
            termByTerm1.get(tt.getTerm1()).add(tt);
            if (!termByTerm2.containsKey(tt.getTerm2())) {
                termByTerm2.put(tt.getTerm2(), new ArrayList<TermTerm>());
            }
            termByTerm2.get(tt.getTerm2()).add(tt);
        }
        this.termSim = termSim;
    }

    public List<TermTerm> getTermByTerm1(String term1, List<String> _ignore) {
        Set<String> ignore = _ignore == null ? new HashSet<>() : new HashSet<>(_ignore);
        List<TermTerm> tt = termByTerm1.get(term1);
        if(tt != null) {
            Iterator<TermTerm> itt = tt.iterator();
            while(itt.hasNext()) {
                if(ignore.contains(itt.next().getTerm2()))
                    itt.remove();
            }
            return tt;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public List<TermTerm> getTermByTerm2(String term2) {
        List<TermTerm> tt = termByTerm2.get(term2);
        return tt == null ? Collections.EMPTY_LIST : tt;
    }

    /**
     * Is the Saffron data available. If this is false the getters of this class
     * may return null;
     *
     * @return true if the code is loaded
     */
    public boolean isLoaded() {
        return taxonomy != null && authorSim != null && termSim != null
                && authorTerms != null && docTerms != null && terms != null
                && corpus != null;
    }

    /**
     * Load the Saffron data from disk
     *
     * @param directory The directory containing the JSON files
     * @return An initializes object
     * @throws IOException
     */
    public static SaffronData fromDirectory(File directory) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();

        File taxonomyFile = new File(directory, "taxonomy.json");
        if (!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find taxonomy.json");
        }

        final SaffronData saffron = new SaffronData();

        saffron.setTaxonomy(mapper.readValue(taxonomyFile, Taxonomy.class));

        File authorSimFile = new File(directory, "author-sim.json");
        if (!authorSimFile.exists()) {
            throw new FileNotFoundException("Could not find author-sim.json");
        }

        saffron.setAuthorSim((List<AuthorAuthor>) mapper.readValue(authorSimFile,
                tf.constructCollectionType(List.class, AuthorAuthor.class)));

        File termSimFile = new File(directory, "term-sim.json");
        if (!termSimFile.exists()) {
            throw new FileNotFoundException("Could not find term-sim.json");
        }

        saffron.setTermSim((List<TermTerm>) mapper.readValue(termSimFile,
                tf.constructCollectionType(List.class, TermTerm.class)));

        File authorTermFile = new File(directory, "author-terms.json");
        if (!authorTermFile.exists()) {
            throw new FileNotFoundException("Could not find author-terms.json");
        }

        saffron.setAuthorTerms((List<AuthorTerm>) mapper.readValue(authorTermFile,
                tf.constructCollectionType(List.class, AuthorTerm.class)));

        File docTermsFile = new File(directory, "doc-terms.json");
        if (!docTermsFile.exists()) {
            throw new FileNotFoundException("Could not find doc-terms.json");
        }

        saffron.setDocTerms((List<DocumentTerm>) mapper.readValue(docTermsFile,
                tf.constructCollectionType(List.class, DocumentTerm.class)));

        File termsFile = new File(directory, "terms.json");
        if (!termsFile.exists()) {
            throw new FileNotFoundException("Could not find terms.json");
        }

        saffron.setTerms((List<Term>) mapper.readValue(termsFile,
                tf.constructCollectionType(List.class, Term.class)));

        File indexFile = new File(directory, "index");
        if(!indexFile.exists()) {
            throw new FileNotFoundException("Could not find index");
        }

        saffron.setCorpus(CorpusTools.readFile(indexFile));

        return saffron;
    }

    public void setCorpus(Corpus corpus) {
        this.corpus = new HashMap<>();
        this.corpusByAuthor = new HashMap<>();
        this.authors = new HashMap<>();
        for (Document d : corpus.getDocuments()) {
            this.corpus.put(d.id, d);
            for (Author a : d.getAuthors()) {
                if (!corpusByAuthor.containsKey(a.id)) {
                    corpusByAuthor.put(a.id, new ArrayList<Document>());
                }
                corpusByAuthor.get(a.id).add(d);
                if (!authors.containsKey(a.id)) {
                    authors.put(a.id, a);
                }
            }
        }
        this.searcher = (DocumentSearcher)corpus;
    }

    public DocumentSearcher getSearcher() {
        return searcher;
    }



    public List<Document> getDocsByAuthor(String authorId) {
        List<Document> docs = corpusByAuthor.get(authorId);
        return docs == null ? Collections.EMPTY_LIST : docs;
    }

    public Author getAuthor(String authorId) {
        return authors.get(authorId);
    }

    public Document getDoc(String docId) {
        return corpus.get(docId);
    }

    private HashMap<String, IntList> getTaxoLocations(Taxonomy t) {
        IntList il = new IntArrayList();
        HashMap<String, IntList> map = new HashMap<>();
        _getTaxoLocations(t, il, map);
        return map;
    }

    private void _getTaxoLocations(Taxonomy t, IntList il, HashMap<String, IntList> map) {
        map.put(t.root, il);
        for (int i = 0; i < t.children.size(); i++) {
            IntList il2 = new IntArrayList(il);
            il2.add(i);
            _getTaxoLocations(t.children.get(i), il2, map);
        }
    }

    private Taxonomy taxoNavigate(Taxonomy t, IntList il) {
        for (int i : il) {
            t = t.children.get(i);
        }
        return t;
    }

    public List<String> getTaxoParents(String termString) {
        IntList il = taxoMap.get(termString);
        if (il != null) {
            Taxonomy t = taxonomy;
            List<String> route = new ArrayList<>();
            for (int i : il) {
                route.add(t.root);
                t = t.children.get(i);
            }
            return route;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public List<String> getTaxoChildren(String termString) {
        IntList il = taxoMap.get(termString);
        if (il != null) {
            Taxonomy t = taxoNavigate(taxonomy, il);
            List<String> children = new ArrayList<>();
            for (Taxonomy t2 : t.children) {
                children.add(t2.root);
            }
            return children;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public List<TermAndScore> getTaxoChildrenScored(String termString) {
        IntList il = taxoMap.get(termString);
        if (il != null) {
            Taxonomy t = taxoNavigate(taxonomy, il);
            List<TermAndScore> children = new ArrayList<>();
            for (Taxonomy t2 : t.children) {
                children.add(new TermAndScore(t2.root, t2.linkScore));
            }
            return children;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public static class TermAndScore {
        public final String term;
        public final double score;

        public TermAndScore(String term, double score) {
            this.term = term;
            this.score = score;
        }


    }
}
