package org.insightcentre.nlp.saffron.documentindex;

import org.insightcentre.nlp.saffron.data.CollectionCorpus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.google.common.collect.Iterators;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.documentindex.tika.DocumentAnalyzer;

/**
 * General tools for working with corpora
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class CorpusTools {
    
    /**
     * Read a file from disk, the format should be one of
     * <ul>
     * <li> <code>.json</code> For Json file</li>
     * <li> <code>.json.gz</code> For Gzipped Json file</li>
     * <li> <code>.tar.gz</code> or <code>.tgz</code> For Tarballs</li>
     * <li> <code>.zip</code> For ZIP files</li>
     * <li> A directory containing files </li>
     * </ul>
     * @param file The file to read
     * @return The corpus object
     * @throws IOException If the file could not be read
     * @throws IllegalArgumentException If the file type was not recognized
     */
    public static Corpus readFile(File file) throws IOException {
        if (file.getName().endsWith(".json")) {
            return fromJson(file);
        } else if (file.getName().endsWith(".json.gz")) {
            return new ObjectMapper().readValue(
                    new GZIPInputStream(new FileInputStream(file)),
                    IndexedCorpus.class);
        } else if (file.getName().endsWith(".tar.gz") || file.getName().endsWith(".tgz")) {
            return fromTarball(file);
        } else if (file.getName().endsWith(".zip")) {
            return fromZIP(file);
        } else if (file.isDirectory()) {
            File indexFile = new File(file, "segments.gen");
            if(indexFile.exists()) {
                return DocumentSearcherFactory.load(file);
            } else {
                return fromFolder(file);
            }
        } else {
            throw new IllegalArgumentException("Could not deduce corpus type for: " + file.getName());
        }
    }

    /**
     * Read a corpus from a JSON file
     *
     * @param jsonFile The JSON file to read
     * @return The corpus object
     * @throws IOException If the file could not be read
     */
    public static Corpus fromJson(File jsonFile) throws IOException {
        return new ObjectMapper().readValue(jsonFile, IndexedCorpus.class);
    }





    /**
     * Create a corpus from a folder, each file will be considered a single
     * document
     *
     * @param folder The folder
     * @return A corpus object
     */
    public static Corpus fromFolder(File folder) {
        if (!folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException(folder.getName() + " does not exist or is not a folder");
        }
        return new FolderCorpus(folder);
    }

    public static class FolderIterator implements Iterator<File> {

        final File[] files;
        int i = -1;
        Iterator<File> currIter = null;
        File next = null;

        public FolderIterator(File[] files) {
            this.files = files;
            advance();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public File next() {
            File f = next;
            advance();
            return f;
        }

        public void advance() {
            next = null;
            if (currIter != null && currIter.hasNext()) {
                next = currIter.next();
                while (currIter != null && !currIter.hasNext() && i < files.length - 1) {
                    i++;
                    if (files[i].isDirectory()) {
                        currIter = new FolderIterator(files[i].listFiles());
                    } else {
                        currIter = null;
                    }
                }
            } else {
                while (next == null && i < files.length) {
                    i++;
                    if (i >= files.length) {
                        // end of list
                    } else if (files[i].isDirectory()) {
                        currIter = new FolderIterator(files[i].listFiles());
                        if (currIter.hasNext()) {
                            next = currIter.next();
                        }
                    } else {
                        next = files[i];
                    }
                }
            }
        }

    }
    private static class FolderCorpus implements Corpus {

        private final File folder;

        public FolderCorpus(File folder) {
            this.folder = folder;
        }

        @Override
        public Iterable<Document> getDocuments() {
            return new Iterable<Document>() {
                @Override
                public Iterator<Document> iterator() {
                    final FolderIterator iter = new FolderIterator(folder.listFiles());
                    return new Iterator<Document>() {
                        @Override
                        public boolean hasNext() {
                            return iter.hasNext();
                        }

                        @Override
                        public Document next() {
                            File f = iter.next();
                            try {
                                return DocumentAnalyzer.analyze(f, f.getName().replaceAll("/|\\\\", "_"));
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    };
                }
            };
        }
        private int _size = -1;

        @Override
        public int size() {
            if(_size < 0) 
                _size = countFiles(folder);
            return _size;
        }
    }
    
    
        private static int countFiles(File folder) {
            if(!folder.isDirectory())
                return 1;
            int n = 0;
            for(File f : folder.listFiles()) {
                if(f.isDirectory()) {
                    n += countFiles(f);
                } else {
                    n++;
                }
            }
            return n;
        }


    /**
     * Create a corpus from a json file which contains a reference to a file location
     *
     * @param jsonFile The json file
     * @return A corpus object
     */
    public static Corpus fromJsonFiles(File jsonFile) {
        return new JSONCorpus(jsonFile);
    }

    private static class JSONCorpus implements Corpus {

        private final List<File> jsonFile;
        List<Author> authors;

        public JSONCorpus(File jsonFile) {
            List<File> jsonFileList = new ArrayList<>();
            Corpus corpus = null;
            try {
                corpus = new ObjectMapper().readValue(jsonFile, IndexedCorpus.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            corpus.getDocuments().forEach(doc -> {
                SaffronPath path = doc.getFile();
                String docFileName = path.resolve(doc.getFile().getPath());
                jsonFileList.add(new File(docFileName));
                authors = doc.authors;
            });
            this.jsonFile = jsonFileList;
        }

        @Override
        public Iterable<Document> getDocuments() {

            return () -> {
                try {
                    final Enumeration<? extends File> zes = Collections.enumeration(jsonFile);
                    return new FilterIterator<>(
                            new Iterator<Document>() {
                                @Override
                                public boolean hasNext() {
                                    return zes.hasMoreElements();
                                }

                                @Override
                                public Document next() {
                                    try {
                                        File ze = zes.nextElement();
                                        while (ze.isDirectory() && zes.hasMoreElements()) {
                                            ze = zes.nextElement();
                                        }
                                        return DocumentAnalyzer.analyze(ze, ze.getName().replaceAll("/|\\\\", "_"), authors);
                                    } catch (IOException x) {
                                        throw new RuntimeException(x);
                                    }
                                }
                            }, o -> o != null);

                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            };
        }

        private int _size = -1;

        @Override
        public int size() {
            if(_size < 0) {
                try {
                    _size = Iterators.size(jsonFile.iterator());
                } catch(Exception x) {
                    throw new RuntimeException(x);
                }
            }
            return _size;
        }


    }



    /**
     * Create a corpus from a zip file, each file will be considered a single
     * document
     *
     * @param zipFile The zip file
     * @return A corpus object
     */
    public static Corpus fromZIP(File zipFile) {
        if (!zipFile.exists() && zipFile.isDirectory()) {
            throw new IllegalArgumentException(zipFile.getName() + " does not exist or is a folder");
        }
        return new ZIPCorpus(zipFile);
    }

    private static class ZIPCorpus implements Corpus {

        private final File zipFile;

        public ZIPCorpus(File zipFile) {
            this.zipFile = zipFile;
        }

        @Override
        public Iterable<Document> getDocuments() {

            return new Iterable<Document>() {
                @Override
                public Iterator<Document> iterator() {
                    try {
                        final ZipFile zip = new ZipFile(zipFile);
                        final Enumeration<? extends ZipEntry> zes = zip.entries();
                        return new FilterIterator<>(
                                new Iterator<Document>() {
                            File file = null;

                            @Override
                            public boolean hasNext() {
                                return zes.hasMoreElements();
                            }

                            @Override
                            public Document next() {
                                try {
                                    ZipEntry ze = zes.nextElement();
                                    while (ze.isDirectory() && zes.hasMoreElements()) {
                                        ze = zes.nextElement();
                                    }
                                    return DocumentAnalyzer.analyze(zip.getInputStream(ze), ze.getName().replaceAll("/|\\\\", "_"));
                                } catch (IOException x) {
                                    throw new RuntimeException(x);
                                }
                            }
                        }, new Predicate<Document>() {
                            @Override
                            public boolean evaluate(Document o) {
                                return o != null;
                            }
                        });

                    } catch (ZipException x) {
                        throw new RuntimeException(x);
                    } catch (IOException x) {
                        throw new RuntimeException(x);
                    }
                }
            };
        }

        private int _size = -1;
        
        @Override
        public int size() {
            if(_size < 0) {
                try {
                    final ZipFile zip = new ZipFile(zipFile);
                    _size = zip.size();
                } catch(IOException x) {
                    throw new RuntimeException(x);
                }
            }
            return _size;
        }
        
        
    }

    /**
     * Create a corpus from a tarball (.tar.gz) file, each file will be
     * considered a single document
     *
     * @param zipFile The zip file
     * @return A corpus object
     */
    public static Corpus fromTarball(File zipFile) {
        return fromTarball(zipFile, null);
    }

    /**
     * Create a corpus from a tarball (.tar.gz) file, each file will be
     * considered a single document
     *
     * @param zipFile The zip file
     * @param targetDir The directory to unzip files to
     * @return A corpus object
     */
    public static Corpus fromTarball(File zipFile, File targetDir) {
        if (!zipFile.exists() && zipFile.isDirectory()) {
            throw new IllegalArgumentException(zipFile.getName() + " does not exist or is a folder");
        }
        if (targetDir != null && !targetDir.mkdirs() && !targetDir.isDirectory()) {
            throw new IllegalArgumentException(targetDir.getName() + " could not be created as a file");
        }
        return new TarballCorpus(zipFile, targetDir);
    }

    private static class TarballCorpus implements Corpus {

        private final File zipFile;
        private final File targetDir;

        public TarballCorpus(File zipFile, File targetDir) {
            this.zipFile = zipFile;
            this.targetDir = targetDir;
        }

        @Override
        public Iterable<Document> getDocuments() {

            return new Iterable<Document>() {
                @Override
                public Iterator<Document> iterator() {
                    try {
                        final TarArchiveInputStream tais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(zipFile)));

                        return new Iterator<Document>() {
                            File file = null;
                            TarArchiveEntry tae;

                            private void advance() {
                                try {
                                    if (tae == null) {
                                        tae = tais.getNextTarEntry();
                                    }
                                    while (tae != null && !tae.isFile()) {
                                        tae = tais.getNextTarEntry();
                                    }
                                } catch (IOException x) {
                                    throw new RuntimeException(x);
                                }
                            }

                            @Override
                            public boolean hasNext() {
                                advance();
                                return tae != null;
                            }

                            @Override
                            public Document next() {
                                try {
                                    advance();
                                    if (tae == null) {
                                        throw new NoSuchElementException();
                                    }
                                    return DocumentAnalyzer.analyze(tais, tae.getName().replace(File.separator, "_"));
                                } catch (IOException x) {
                                    throw new RuntimeException(x);
                                } finally {
                                    try {
                                        tae = tais.getNextTarEntry();
                                    } catch (IOException x) {
                                    }
                                }
                            }
                        };

                    } catch (ZipException x) {
                        throw new RuntimeException(x);
                    } catch (IOException x) {
                        throw new RuntimeException(x);
                    }
                }
            };

        }

        private int _size = -1;
        
        @Override
        public int size() {
            if(_size < 0) {
                int n = 0;
                try(final TarArchiveInputStream tais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(zipFile)))) {
                    TarArchiveEntry tae = (TarArchiveEntry)tais.getNextEntry();
                    while(tae != null) {
                        if(tae.isFile()) n++;
                        tae = tais.getNextTarEntry();
                    }
                    _size = n;
                } catch(IOException x) {
                    throw new RuntimeException(x);
                }
            }
            return _size;
        }
        
        
    }

    /**
     * Create a corpus from a collection
     * @param documents The document collection
     * @return A corpus object over the documents
     */
    public static Corpus fromCollection(Collection<Document> documents) {
        return new CollectionCorpus(documents);
    }
}
