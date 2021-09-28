package org.insightcentre.saffron.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;
import org.insightcentre.saffron.web.exception.ConceptNotFoundException;
import org.insightcentre.saffron.web.exception.TermNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.json.JSONObject;

/**
 * All the data generated during the run of Saffron that is exposed by the Web
 * interface
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class SaffronInMemoryDataSource implements SaffronDataSource {

    private final Map<String, SaffronDataImpl> data = new HashMap<>();

    public final String type = "file";

    private static class SaffronDataImpl {

        private Taxonomy taxonomy;
        private Partonomy partonomy;
        private KnowledgeGraph knowledgeGraph;

        private List<AuthorAuthor> authorSim;
        private List<TermTerm> termSim;
        private List<AuthorTerm> authorTerms;
        private List<DocumentTerm> docTerms;
        private HashMap<String, Term> terms;
        private HashMap<String, Concept> concepts;
        private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
        private HashMap<String, List<TermTerm>> termByTerm1, termByTerm2;
        private HashMap<String, List<DocumentTerm>> docByTerm, termByDoc;
        private HashMap<String, List<AuthorTerm>> authorByTerm, termByAuthor;
        private List<String> termsSorted;
        private HashMap<String, Document> corpus;
        private HashMap<String, List<Document>> corpusByAuthor;
        private HashMap<String, Author> authors;
        private HashMap<String, IntList> taxoMap;
        private Corpus searcher;
        private final String id;

        public SaffronDataImpl(String id) {
            this.id = id;
        }

        public Taxonomy getTaxonomy() {
            return taxonomy;
        }

        public Partonomy getPartonomy() {
            return partonomy;
        }

        public KnowledgeGraph getKnowledgeGraph() {
            return knowledgeGraph;
        }

        public void setTaxonomy(Taxonomy taxonomy) {
            this.taxonomy = taxonomy;
            this.taxoMap = getTaxoLocations(taxonomy);
        }

        public void setKnowledgeGraph(KnowledgeGraph knowledgeGraph) {
            this.knowledgeGraph = knowledgeGraph;
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
            for (AuthorAuthor aa : aas) {
                Author a = getAuthor(aa.author1_id);
                if (a != null) {
                    as.add(a);
                }
            }
            return as;
        }

        public List<Author> authorAuthorToAuthor2(List<AuthorAuthor> aas) {
            List<Author> as = new ArrayList<>();
            for (AuthorAuthor aa : aas) {
                Author a = getAuthor(aa.author2_id);
                if (a != null) {
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
            if (from < termsSorted.size() && to <= termsSorted.size()) {
                return termsSorted.subList(from, to);
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public Term getTerm(String term) {
            return terms.get(term);
        }

        public Collection<Term> getTerms() {
            return terms == null ? Collections.EMPTY_LIST : terms.values();
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

        public Concept getConcept(String id) {
            return concepts.get(id);
        }

        public List<Concept> getConcepts() {
            return concepts == null ? Collections.EMPTY_LIST : (List) concepts.values();
        }

        public void addConcept(Concept concept) {
            if (this.concepts == null || this.concepts.size() == 0)
                this.concepts = new HashMap<>();
            this.concepts.put(concept.getId(), concept);
        }

        public void setConcepts(Collection<Concept> concepts) {
            this.concepts = new HashMap<>();
            for (Concept c: concepts) {
                this.concepts.put(c.getId(), c);
            }
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
            if (tt != null) {
                Iterator<TermTerm> itt = tt.iterator();
                while (itt.hasNext()) {
                    if (ignore.contains(itt.next().getTerm2())) {
                        itt.remove();
                    }
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
         * Is the Saffron data available. If this is false the getters of this
         * class may return null;
         *
         * @return true if the code is loaded
         */
        public boolean isLoaded() {
            return taxonomy != null && authorSim != null && termSim != null
                    && authorTerms != null && docTerms != null && terms != null
                    && corpus != null;
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
            //this.searcher = corpus;
        }

        public Corpus getSearcher() {
            return searcher;
        }

        public List<Document> getDocsByAuthor(String authorId) {
            List<Document> docs = corpusByAuthor.get(authorId);
            return docs == null ? Collections.EMPTY_LIST : docs;
        }

        public Author getAuthor(String authorId) {
            return authors.get(authorId);
        }

        public Collection<Author> getAuthors() {
            return authors.values();
        }

        public Document getDoc(String docId) {
            return corpus.get(docId);
        }

        public Collection<Document> getDocuments() {
            return corpus.values();
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

        public Taxonomy getTaxoDescendent(String termString) {

            Taxonomy t = taxonomy;
            return taxonomy.descendent(termString);
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

        private void updateTermName(String term, String newTerm, Status status) {
            Term t = terms.get(term);
            if (t != null) {
                for (AuthorTerm at : authorTerms) {
                    if (at.getTermId().equals(term)) {
                        at.setTermId(newTerm);
                    }
                }
                for (DocumentTerm dt : docTerms) {
                    if (dt.getTermString().equals(term)) {
                        dt.setTermString(newTerm);
                    }
                }
                for (TermTerm tt : termSim) {
                    if (tt.getTerm1().equals(term)) {
                        tt.setTerm1(newTerm);
                    }
                    if (tt.getTerm2().equals(term)) {
                        tt.setTerm2(newTerm);
                    }
                }
                updateTermNameInTaxonomy(taxonomy, term, newTerm);
                t.setString(newTerm);
                t.setStatus(status);
            }
        }

        private void updateTermNameInTaxonomy(Taxonomy taxo, String term, String newTerm) {
            if(taxo.root.equals(term)) {
                taxo.root = newTerm;
            } else {
                for(Taxonomy child : taxo.getChildren()) {
                    updateTermNameInTaxonomy(child, term, newTerm);
                }
            }
        }

        public void setSearcher(Corpus searcher) {
            this.searcher = searcher;
        }
    }


    /**
     * Load the Saffron data from disk
     *
     * @param directory The directory containing the JSON files
     * @return An initializes object
     * @throws IOException
     */
    public void fromDirectory(File directory, String name) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();
        String workingDir = System.getProperty("user.dir");
        workingDir = workingDir.substring(0, workingDir.length() - 4);
        System.setProperty("user.dir", workingDir);
        String saffonPath;
        if (directory.getAbsolutePath().equals(directory.getPath())) {
            saffonPath = directory.getAbsolutePath();
        } else {
            saffonPath = workingDir + "/" + directory;
        }
        File taxonomyFile = new File(saffonPath, "kg.json");
        if (!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find kg.json");
        }

        final SaffronDataImpl saffron = new SaffronDataImpl(name);

        saffron.setKnowledgeGraph(mapper.readValue(taxonomyFile, KnowledgeGraph.class));

        File authorSimFile = new File(saffonPath, "author-sim.json");
        if (!authorSimFile.exists()) {
            throw new FileNotFoundException("Could not find author-sim.json");
        }

        saffron.setAuthorSim((List<AuthorAuthor>) mapper.readValue(authorSimFile,
                tf.constructCollectionType(List.class, AuthorAuthor.class)));

        File termSimFile = new File(saffonPath, "term-sim.json");
        if (!termSimFile.exists()) {
            throw new FileNotFoundException("Could not find term-sim.json");
        }

        saffron.setTermSim((List<TermTerm>) mapper.readValue(termSimFile,
                tf.constructCollectionType(List.class, TermTerm.class)));

        File authorTermFile = new File(saffonPath, "author-terms.json");
        if (!authorTermFile.exists()) {
            throw new FileNotFoundException("Could not find author-terms.json");
        }

        saffron.setAuthorTerms((List<AuthorTerm>) mapper.readValue(authorTermFile,
                tf.constructCollectionType(List.class, AuthorTerm.class)));

        File docTermsFile = new File(saffonPath, "doc-terms.json");
        if (!docTermsFile.exists()) {
            throw new FileNotFoundException("Could not find doc-terms.json");
        }

        saffron.setDocTerms((List<DocumentTerm>) mapper.readValue(docTermsFile,
                tf.constructCollectionType(List.class, DocumentTerm.class)));

        File termsFile = new File(saffonPath, "terms.json");
        if (!termsFile.exists()) {
            throw new FileNotFoundException("Could not find terms.json");
        }

        saffron.setTerms((List<Term>) mapper.readValue(termsFile,
                tf.constructCollectionType(List.class, Term.class)));

        File indexFile = new File(saffonPath, "corpus.json");
        if (!indexFile.exists()) {
            throw new FileNotFoundException("Could not find index");
        }

        saffron.setCorpus(CorpusTools.readFile(indexFile));

        this.data.put(name, saffron);
    }


    @Override
    public boolean addAuthorSimilarity(String id, Date date, List<AuthorAuthor> authorSim) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        authorSim.addAll(saffron.authorSim);
        saffron.setAuthorSim(authorSim);
        return true;
    }

    @Override
    public boolean addDocumentTermCorrespondence(String id, Date date, List<DocumentTerm> terms) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        terms.addAll(saffron.docTerms);
        saffron.setDocTerms(terms);
        return true;
    }

    @Override
    public boolean addRun(String id, Date date, Configuration config) {
        if(!data.containsKey(id)) {
            data.put(id, new SaffronDataImpl(id));
        }
        return true;
    }

    @Override
    public boolean addTaxonomy(String id, Date date, Taxonomy graph) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        saffron.setTaxonomy(graph);
        return true;
    }

    @Override
    public boolean addPartonomy(String id, Date date, Partonomy graph) {
        throw new NotImplementedException();
    }

    @Override
    public boolean addTermExtraction(String id, Date date, Set<Term> res) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        res.addAll(saffron.terms.values());
        saffron.setTerms(res);
        return true;
    }

    @Override
    public boolean addTerms(String id, Date date, List<Term> terms) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        terms.addAll(saffron.terms.values());
        saffron.setTerms(terms);
        return true;
    }

    @Override
    public boolean addTermsSimilarity(String id, Date date, List<TermTerm> termSimilarity) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        termSimilarity.addAll(saffron.termSim);
        saffron.setTermSim(termSimilarity);
        return true;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void deleteRun(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return;
        }
        data.remove(runId);
    }

    @Override
    public void deleteTerm(String runId, String term) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return;
        }
        List<Term> terms = saffron.getTerms().stream().filter((Term t) -> !t.getString().equals(term)).collect(Collectors.toList());
        saffron.setTerms(terms);
    }

    @Override
    public String getRun(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return "";
        }
        return saffron.id;
    }

    @Override
    public void updateRun(String runId, String originalRun, JSONObject json, String status) {
        throw new NotImplementedException();
    }

    @Override
    public boolean updateTaxonomy(String runId, Taxonomy graph) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return false;
        }
        saffron.setTaxonomy(graph);
        return true;
    }

    @Override
    public boolean updateTerm(String id, String term, String status) {
        return false;
    }

    @Override
    public boolean updateTermName(String id, String term, String newTerm, String status) {
        return false;
    }


    @Override
    public Taxonomy getTaxonomy(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxonomy();
    }

    @Override
    public Partonomy getPartonomy(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getPartonomy();
    }

    @Override
    public KnowledgeGraph getKnowledgeGraph(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getKnowledgeGraph();
    }

    @Override
    public List<DocumentTerm> getDocTerms(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTerms();
    }


    @Override
    public List<String> getTaxoParents(String runId, String termString) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoParents(termString);
    }

    @Override
    public List<SaffronRun> getAllRuns() {
        throw new NotImplementedException();
    }

    @Override
    public List<TermAndScore> getTaxoChildrenScored(String runId, String termString) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildrenScored(termString);
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor1(String runId, String author1) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorSimByAuthor1(author1);
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor2(String runId, String author1) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorSimByAuthor2(author1);
    }

    @Override
    public List<Author> authorAuthorToAuthor1(String runId, List<AuthorAuthor> aas) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorAuthorToAuthor1(aas);
    }

    @Override
    public List<Author> authorAuthorToAuthor2(String runId, List<AuthorAuthor> aas) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorAuthorToAuthor2(aas);
    }

    @Override
    public List<String> getTaxoChildren(String runId, String termString) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildren(termString);
    }

    @Override
    public List<TermTerm> getTermByTerm1(String runId, String term1, List<String> _ignore) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByTerm1(term1, _ignore);
    }

    @Override
    public List<TermTerm> getTermByTerm2(String runId, String term2) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByTerm2(term2);
    }

    @Override
    public List<AuthorTerm> getTermByAuthor(String runId, String author) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByAuthor(author);
    }

    @Override
    public List<AuthorTerm> getAuthorByTerm(String runId, String term) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorByTerm(term);
    }

    @Override
    public List<Author> authorTermsToAuthors(String runId, List<AuthorTerm> ats) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorTermsToAuthors(ats);
    }

    @Override
    public List<DocumentTerm> getTermByDoc(String runId, String doc) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermByDoc(doc);
    }

    @Override
    public List<Document> getDocsByTerm(String runId, String term) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocByTerm(term);
    }

    @Override
    public Term getTerm(String runId, String term) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTerm(term);
    }

    @Override
    public List<Document> getDocsByAuthor(String runId, String authorId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocsByAuthor(authorId);
    }

    @Override
    public Collection<String> getTopTerms(String runId, int from, int to) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopTerms(from, to);
    }

    @Override
    public Author getAuthor(String runId, String authorId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthor(authorId);
    }

    @Override
    public Document getDoc(String runId, String docId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDoc(docId);
    }

    @Override
    public Corpus getSearcher(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getSearcher();
    }

    @Override
    public void setDocTerms(String runId, List<DocumentTerm> docTerms) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setDocTerms(docTerms);
    }

    @Override
    public void setIndex(String runId, Corpus index) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setSearcher(index);
    }

    @Override
    public void setCorpus(String runId, Corpus corpus) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setCorpus(corpus);
    }

    @Override
    public void setTerms(String runId, List<Term> terms) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTerms(terms);
    }

    @Override
    public void setAuthorTerms(String runId, Collection<AuthorTerm> authorTerms) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorTerms(authorTerms);
    }

    @Override
    public void setTermSim(String runId, List<TermTerm> termSim) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTermSim(termSim);
    }

    @Override
    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorSim(authorSim);
    }

    @Override
    public void setTaxonomy(String runId, Taxonomy taxonomy) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTaxonomy(taxonomy);
    }

    @Override
    public void setKnowledgeGraph(String runId, KnowledgeGraph knowledgeGraph) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setKnowledgeGraph(knowledgeGraph);
    }

    @Override
    public void remove(String runId) {
        data.remove(runId);
    }

    @Override
    public boolean containsKey(String id) {
        return data.containsKey(id);
    }

    @Override
    public boolean isLoaded(String id) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.isLoaded();
    }

    @Override
    public Iterable<String> runs() {
        return data.keySet();
    }

    @Override
    public Taxonomy getTaxoDescendent(String runId, String termString) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoDescendent(termString);
    }

    @Override
    public Iterable<Document> getAllDocuments(String datasetName) {
        SaffronDataImpl saffron = data.get(datasetName);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocuments();
    }

    @Override
    public Iterable<Author> getAllAuthors(String datasetName) {
        SaffronDataImpl saffron = data.get(datasetName);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthors();
    }

    @Override
    public Iterable<Term> getAllTerms(String datasetName) {
        SaffronDataImpl saffron = data.get(datasetName);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTerms();
    }

    @Override
    public Date getDate(String doc) {
        return new Date();
    }

    @Override
    public List<AuthorTerm> getAllAuthorTerms(String name) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorTerms();
    }

    @Override
    public Iterable<DocumentTerm> getDocTermByTerm(String name, String termId) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTerms().stream().filter(dt -> dt.getTermString().equals(termId)).collect(Collectors.toList());
    }

    @Override
    public Iterable<TermTerm> getAllTermSimilarities(String name) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermSim();
    }

    @Override
    public Iterable<TermTerm> getTermByTerms(String name, String term1, String term2) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTermSim().stream().filter(tt -> tt.getTerm1().equals(term1) && tt.getTerm2().equals(term2)).collect(Collectors.toList());
    }


    @Override
    public List<Concept> getAllConcepts(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getConcepts();
    }

    @Override
    public Concept getConcept(String runId, String conceptId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getConcept(conceptId);
    }

    //FIXME Suboptimal
    @Override
    public List<Concept> getConceptsByPreferredTermString(String runId, String preferredTermString) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        List<Concept> result = new ArrayList<Concept>();
        for(Concept concept: saffron.getConcepts()) {
            if (concept.getPreferredTermString().equals(preferredTermString))
                result.add(concept);
        }

        return result;
    }

    @Override
    public void addConcepts(String runId, List<Concept> concepts) {
        if (concepts != null) {
            for(Concept concept: concepts) {
                try {
                    this.addConcept(runId, concept);
                } catch (TermNotFoundException e) {
                    //TODO Include logging!!!!!!
                    // The term X could not be found in the database, Skipping concept Y
                    continue;
                }
            }
        }
    }

    @Override
    public void addConcept(String runId, Concept conceptToBeAdded) throws TermNotFoundException {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        if (conceptToBeAdded.getId() == null || conceptToBeAdded.getId().equals(""))
            throw new InvalidValueException("The concept id cannot be null or empty");

        if (saffron.getConcept(conceptToBeAdded.getId()) != null)
            throw new RuntimeException("A concept with same id already exists in the database. id: " + conceptToBeAdded.getId());
        if (saffron.getTerm(conceptToBeAdded.getPreferredTermString()) == null)
            throw new TermNotFoundException(conceptToBeAdded.getPreferredTerm());
        for (Term synonym: conceptToBeAdded.getSynonyms()) {
            if (saffron.getTerm(synonym.getString()) == null)
                throw new TermNotFoundException(synonym);
        }

        saffron.addConcept(conceptToBeAdded);
    }

    @Override
    public void updateConcept(String runId, Concept conceptToBeUpdated)
            throws ConceptNotFoundException, TermNotFoundException {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        if (conceptToBeUpdated.getId() == null || conceptToBeUpdated.getId().equals(""))
            throw new InvalidValueException("The concept id cannot be null or empty");

        Concept c = saffron.getConcept(conceptToBeUpdated.getId());
        if (c == null)
            throw new ConceptNotFoundException(conceptToBeUpdated);
        if (saffron.getTerm(conceptToBeUpdated.getPreferredTermString()) == null)
            throw new TermNotFoundException(conceptToBeUpdated.getPreferredTerm());
        for (Term synonym: conceptToBeUpdated.getSynonyms()) {
            if (saffron.getTerm(synonym.getString()) == null)
                throw new TermNotFoundException(synonym);
        }

        this.removeConcept(runId, c.getId());
        this.addConcept(runId, conceptToBeUpdated);
    }

    @Override
    public void removeConcept(String runId, String conceptId) throws ConceptNotFoundException {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        if (saffron.getConcept(conceptId) == null)
            throw new ConceptNotFoundException(new Concept.Builder(conceptId, "").build());

        List<Concept> concepts = saffron.getConcepts().stream().filter((Concept c) -> !c.getId().equals(conceptId)).collect(Collectors.toList());
        saffron.setConcepts(concepts);
    }

    //FIXME Suboptimal
    public void removeTermFromConcepts(String runId, String term) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }

        for(Concept concept: getAllConcepts(runId)) {
            try {
                if (concept.getPreferredTerm().equals(term)) {
                    // If term is a preferred term, then choose a random synonym to become
                    // a preferred term, or remove the concept if no synonym is available
                    if (concept.getSynonyms() == null || concept.getSynonyms().size() == 0)
                        this.removeConcept(runId, concept.getId());
                    else {
                        concept.setPreferredTerm(concept.getSynonyms().iterator().next());
                        this.updateConcept(runId, concept);
                    }
                } else if (concept.getSynonymsStrings().contains(term)) {
                    // If term is not a preferred term, just remove it from the list of synonyms
                    Set<Term> synonyms = concept.getSynonyms();
                    Term toBeRemoved = null;
                    for(Term toRemove: synonyms){
                        if (toRemove.getString().equals(term)) {
                            toBeRemoved = toRemove;
                            break;
                        }
                    }
                    synonyms.remove(toBeRemoved);
                    concept.setSynonyms(synonyms);
                    this.updateConcept(runId, concept);
                }
            } catch (ConceptNotFoundException | TermNotFoundException e) {
                //Include logging here
                throw new RuntimeException("An error has occurred while removing term-concept relationships",e);
            }
        }
    }


	@Override
	public void addAuthors(String runId, List<Author> authors) {
		throw new NotImplementedException();
	}


	@Override
	public void addAuthor(String runId, Author authorToBeAdded) throws Exception {
		throw new NotImplementedException();
	}


	@Override
	public List<AuthorTerm> getAuthorTermRelationsPerTerm(String runId, String termId) {
		throw new NotImplementedException();
	}

        @Override
	public List<AuthorTerm> getAuthorTermRelationsPerAuthor(String runId, String authorId) {
		throw new NotImplementedException();
	}

    @Override
    public List<AuthorAuthor> getAuthorSimilarity(String runId, String authorId) {
        throw new NotImplementedException();
    }


}
