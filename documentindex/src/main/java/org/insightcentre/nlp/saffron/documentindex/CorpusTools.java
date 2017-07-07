package org.insightcentre.nlp.saffron.documentindex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;

/**
 * General tools for working with corpora
 *
 * @author John McCrae <john@mccr.ae>
 */
public class CorpusTools {
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
            if(currIter != null && currIter.hasNext()) {
                next = currIter.next();
                while(currIter != null && !currIter.hasNext() && i < files.length) {
                    i++;
                    if(files[i].isDirectory()) {
                        currIter = new FolderIterator(files[i].listFiles());
                    } else {
                        currIter = null;
                    }                    
                }
            } else {
                while(next == null && i < files.length) {
                    i++;
                    if(i >= files.length) {
                        // end of list
                    } else if(files[i].isDirectory()) {
                        currIter  = new FolderIterator(files[i].listFiles());
                        if(currIter.hasNext())
                            next = currIter.next();
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
                                return new Document(f, f.getName(), f.getName(),
                                        Files.probeContentType(f.toPath()), new ArrayList<Author>(),
                                        new HashMap<String, String>());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    };
                }
            };
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
                                    if (ze.isDirectory()) {
                                        return null;
                                    }
                                    if (file != null) {
                                        file.delete();
                                    }
                                    file = File.createTempFile(ze.getName(), "");
                                    file.deleteOnExit();
                                    FileOutputStream fos2 = new FileOutputStream(file);
                                    InputStream is = zip.getInputStream(ze);

                                    fos2.getChannel().transferFrom(Channels.newChannel(is), 0, Long.MAX_VALUE);

                                    return new Document(file, ze.getName(),
                                            ze.getName(), Files.probeContentType(new File(ze.getName()).toPath()),
                                            new ArrayList<Author>(), new HashMap<String, String>());
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
    }

     /**
     * Create a corpus from a tarball (.tar.gz) file, each file will be considered a single
     * document
     *
     * @param zipFile The zip file
     * @return A corpus object
     */
    public static Corpus fromTarball(File zipFile) {
        if (!zipFile.exists() && zipFile.isDirectory()) {
            throw new IllegalArgumentException(zipFile.getName() + " does not exist or is a folder");
        }
        return new TarballCorpus(zipFile);
    }

    private static class TarballCorpus implements Corpus {

        private final File zipFile;

        public TarballCorpus(File zipFile) {
            this.zipFile = zipFile;
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
                                    if(tae == null)
                                        tae = tais.getNextTarEntry();
                                    while(tae != null && !tae.isFile())
                                        tae = tais.getNextTarEntry();
                                } catch(IOException x) {
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
                                    if(tae == null) throw new NoSuchElementException();
                                    if (file != null) {
                                        file.delete();
                                    }
                                    file = File.createTempFile(tae.getName(), "");
                                    file.deleteOnExit();
                                    FileOutputStream fos2 = new FileOutputStream(file);

                                    fos2.getChannel().transferFrom(Channels.newChannel(tais), 0, Long.MAX_VALUE);

                                    return new Document(file, tae.getName(),
                                            tae.getName(), Files.probeContentType(new File(tae.getName()).toPath()),
                                            new ArrayList<Author>(), new HashMap<String, String>());                                    
                                } catch (IOException x) {
                                    throw new RuntimeException(x);
                                } finally {
                                    try {
                                        tae = tais.getNextTarEntry();
                                    } catch(IOException x) { }
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
    }
}
