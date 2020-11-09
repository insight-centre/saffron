package org.insightcentre.nlp.saffron.taxonomy.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class KGExtractionUtils {

    private KnowledgeGraph knowledgeGraph;
    private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
    private HashMap<String, List<TermTerm>> termByTerm1, termByTerm2;
    private HashMap<String, List<DocumentTerm>> docByTerm, termByDoc;
    private HashMap<String, List<AuthorTerm>> authorByTerm, termByAuthor;
    private List<AuthorAuthor> authorSim;
    private List<TermTerm> termSim;
    private List<AuthorTerm> authorTerms;
    private List<DocumentTerm> docTerms;
    private HashMap<String, Term> terms;
    private List<String> termsSorted;
    private HashMap<String, Document> corpus;
    private HashMap<String, List<Document>> corpusByAuthor;
    private HashMap<String, Author> authors;

    public KnowledgeGraph getKnowledgeGraph() {
        return this.knowledgeGraph;
    }

    private KGExtractionUtils() {
    }
    
    

    /**
     * Load the Saffron data from disk
     *
     * @param directory The directory containing the JSON files
     * @return An initializes object
     * @throws IOException
     */
    public static KGExtractionUtils fromDirectory(File directory) throws IOException {
        KGExtractionUtils utils = new KGExtractionUtils();
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();
        String workingDir = System.getProperty("user.dir");
        workingDir = workingDir.substring(0, workingDir.length() - 8);
        System.setProperty("user.dir", workingDir);
        File taxonomyFile = new File(directory, "kg.json");
        if (!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find kg.json");
        }
        utils.setKnowledgeGraph(mapper.readValue(taxonomyFile, KnowledgeGraph.class));

        File authorSimFile = new File(directory, "author-sim.json");
        if (!authorSimFile.exists()) {
            throw new FileNotFoundException("Could not find author-sim.json");
        }

        utils.setAuthorSim((List<AuthorAuthor>) mapper.readValue(authorSimFile,
                tf.constructCollectionType(List.class, AuthorAuthor.class)));

        File termSimFile = new File(directory, "term-sim.json");
        if (!termSimFile.exists()) {
            throw new FileNotFoundException("Could not find term-sim.json");
        }

        utils.setTermSim((List<TermTerm>) mapper.readValue(termSimFile,
                tf.constructCollectionType(List.class, TermTerm.class)));

        File authorTermFile = new File(directory, "author-terms.json");
        if (!authorTermFile.exists()) {
            throw new FileNotFoundException("Could not find author-terms.json");
        }

        utils.setAuthorTerms((List<AuthorTerm>) mapper.readValue(authorTermFile,
                tf.constructCollectionType(List.class, AuthorTerm.class)));

        File docTermsFile = new File(directory, "doc-terms.json");
        if (!docTermsFile.exists()) {
            throw new FileNotFoundException("Could not find doc-terms.json");
        }

        utils.setDocTerms((List<DocumentTerm>) mapper.readValue(docTermsFile,
                tf.constructCollectionType(List.class, DocumentTerm.class)));

        File termsFile = new File(directory, "terms.json");
        if (!termsFile.exists()) {
            throw new FileNotFoundException("Could not find terms.json");
        }

        utils.setTerms((List<Term>) mapper.readValue(termsFile,
                tf.constructCollectionType(List.class, Term.class)));

        File indexFile = new File(directory, "corpus.json");
        if (!indexFile.exists()) {
            System.err.println("Corpus does not exist... skipping");
        } else {

            // Temporary fix until more changes from other branch can be merged
            //utils.setCorpus(CorpusTools.readFile(indexFile));
            try{
                utils.setCorpus((Corpus)Class.forName("org.insightcentre.nlp.saffron.documentindex.CorpusTools").getMethod("readFile", File.class).invoke(null, indexFile));
            } catch(Exception x) {
                throw new RuntimeException(x);
            }
        }
        return utils;
    }


    private void setKnowledgeGraph(KnowledgeGraph knowledgeGraph) {
        this.knowledgeGraph = knowledgeGraph;
    }

    public Collection<Term> getTerms() {
        return terms == null ? Collections.EMPTY_LIST : terms.values();
    }

    public List<TermTerm> getTermByTerm1(String term1, List<String> _ignore) {
        Set<String> ignore = _ignore == null ? new HashSet<>() : new HashSet<>(_ignore);
        List<TermTerm> tt = this.termByTerm1.get(term1);
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

    private void setAuthorSim(List<AuthorAuthor> authorSim) {
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

    private void setAuthorTerms(Collection<AuthorTerm> authorTerms) {
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

    private void setDocTerms(List<DocumentTerm> docTerms) {
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

    private void setTerms(Collection<Term> _terms) {
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

    private void setCorpus(Corpus corpus) {
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
    }

    private void setTermSim(List<TermTerm> termSim) {
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


}
