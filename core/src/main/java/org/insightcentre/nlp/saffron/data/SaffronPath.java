package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A path that can be interpolated with saffron.home
 *
 * @author John McCrae
 */
public class SaffronPath {

    @JsonProperty("path")
    private String path;

    // for deserialisation
    public SaffronPath() {
    }

    @JsonCreator
    public SaffronPath(String path) {
        this.path = path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public String getResolvedPath() {
        return resolve(this.path);
    }

    public static SaffronPath fromFile(File file) {
        return new SaffronPath(file.getAbsolutePath());
    }

    public File toFile() {
        return new File(resolve(path));
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static String resolve(String path) {
        String saffronPath = System.getenv("SAFFRON_HOME");
        if (saffronPath == null) {
            saffronPath = System.getProperty("saffron.home");
        }
        if (saffronPath == null) {
            saffronPath = ".";
        }
        if (path.startsWith("${saffron.models}")) {
            final String fileName = path.replaceAll("\\$\\{saffron.models\\}/?", "");
            // Code based on https://www.baeldung.com/java-compress-and-uncompress
            // Equivalent to 
            //     wget https://server1.nlp/insight-centre/saffron-datasets/models/$file.zip
            //     unzip $file.zip
            final File destDir = new File(new File(saffronPath), "models");
            final File file = new File(destDir, fileName);
            byte[] buffer = new byte[1024];
            if (!file.exists()) {
                final String url = "https://server1.nlp.insight-centre.org/saffron-datasets/models/"
                        + fileName + ".zip";
                try (ZipInputStream zis = new ZipInputStream(new URL(url).openStream())) {

                    ZipEntry zipEntry = zis.getNextEntry();
                    while (zipEntry != null) {
                        File newFile = newFile(destDir, zipEntry);
                        if (zipEntry.isDirectory()) {
                            if (!newFile.isDirectory() && !newFile.mkdirs()) {
                                throw new IOException("Failed to create directory " + newFile);
                            }
                        } else {
                            // fix for Windows-created archives
                            File parent = newFile.getParentFile();
                            if (!parent.isDirectory() && !parent.mkdirs()) {
                                throw new IOException("Failed to create directory " + parent);
                            }

                            // write file content
                            FileOutputStream fos = new FileOutputStream(newFile);
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                        }
                        zipEntry = zis.getNextEntry();
                    }
                    zis.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(!file.exists()) {
                System.err.println("Could not resolve: " + saffronPath);
            }
            return file.getAbsolutePath();
        }
        return path.replaceAll("\\$\\{saffron.home\\}", saffronPath);
    }

    @Override
    @JsonValue
    public String toString() {
        return path;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.path);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SaffronPath other = (SaffronPath) obj;
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        return true;
    }

}
