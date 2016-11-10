package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A corpus of documents for processing
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Corpus {
    public final List<Document> documents;
    public final File index;


    /**
     * Create a corpus
     * @param documents The list of documents in the corpus
     * @param index The index where the documents are to be stored
     */
    @JsonCreator
    public Corpus(@JsonProperty("documents") List<Document> documents,
                  @JsonProperty("index") File index) {
        this.documents = documents;
        this.index = index;
    }

    /**
     * Create a corpus from a folder, each file will be considered a single document
     * @param folder The folder
     * @param index The index to write the index
     * @return A corpus object
     * @throws IOException If an IO error occurs
     */
    public static Corpus fromFolder(File folder, File index) throws IOException {
        if(!folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException(folder.getName() + " does not exist or is not a folder");
        }
        List<Document> docs = new ArrayList<>();
        for(File f : folder.listFiles()) {
            docs.add(new Document(f, f.getName(), f.getName(), 
                Files.probeContentType(f.toPath()), new ArrayList<Author>(), 
                new HashMap<String, String>()));
        }
        
        return new Corpus(docs, index);
    }

    /**
     * Create a corpus from a folder, each file will be considered a single document
     * @param zipFile The folder
     * @param index The index to write the index
     * @return A corpus object
     * @throws IOException If an IO error occurs
     */
    public static Corpus fromZIP(File zipFile, File index) throws IOException {
        if(!zipFile.exists() && zipFile.isDirectory()) {
            throw new IllegalArgumentException(zipFile.getName() + " does not exist or is a folder");
        }
        List<Document> docs = new ArrayList<>();
        ZipFile zip = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> zes = zip.entries();
        while(zes.hasMoreElements()) {
            ZipEntry ze = zes.nextElement();
            if(!ze.isDirectory()) {
                docs.add(new Document(new File(zipFile, ze.getName()), ze.getName(), 
                    ze.getName(), Files.probeContentType(new File(ze.getName()).toPath()), 
                    new ArrayList<Author>(), new HashMap<String, String>()));
                    
            }
        }
        
        return new Corpus(docs, index);
    }

    public File getIndex() {
        return index;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.documents);
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
        final Corpus other = (Corpus) obj;
        if (!Objects.equals(this.documents, other.documents)) {
            return false;
        }
        return true;
    }

    
}
