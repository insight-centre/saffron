package org.insightcentre.nlp.saffron.topicextraction.data;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@XmlRootElement(name = "domainModel")
public class DomainModel {
	private List<String> domainWords;

	public List<String> getDomainWords() {
		return domainWords;
	}
	
	public void setDomainWords(List<String> domainWords) {
		this.domainWords = domainWords;
	}

	public DomainModel(List<String> domainWords) {
		this.domainWords = domainWords;
	}

	public DomainModel() {

	}

	public static DomainModel fromFile(File f) throws IOException {
		List<String> model = FileUtils.readLines(f);
		return new DomainModel(model);
	}
	
	public static DomainModel fromStream(InputStream is) throws IOException {
		List<String> model = IOUtils.readLines(is);
		return new DomainModel(model);
	}

	/**
	 * Creates a temporary folder with a lists.def and domainModel.lst file for
	 * use as a GATE Gazetteer.
	 * 
	 * Reasoning: Unfortunately GATE gazetteers are designed to take file paths
	 * as parameters, and no implementation is designed to work without being
	 * given a "listsURL" parameter. It would be possible to extend
	 * HashmapGazetteer for this purporse, but this would likely cause issues
	 * when upgrading GATE.
	 * 
	 * @return
	 * @throws IOException
	 */
	public TemporaryGazetteerDirectory asGateGazetteer() throws IOException {
		return new TemporaryGazetteerDirectory(this);
	}
	
	public class TemporaryGazetteerDirectory implements Closeable {
		private File listsFile;
		private Path tempPath;

		public TemporaryGazetteerDirectory(DomainModel domainModel) throws IOException {
			tempPath = Files.createTempDirectory(UUID.randomUUID().toString());

			listsFile = tempPath.resolve("lists.def").toFile();
			FileUtils.writeStringToFile(listsFile, "domainModel.lst:domainModel\n");

			File domainModelPath = tempPath.resolve("domainModel.lst").toFile();
			String domainWordList = StringUtils.join(domainWords, "\n");
			FileUtils.writeStringToFile(domainModelPath, domainWordList);
		}

		public URL getListsURL() throws MalformedURLException {
			return listsFile.toURI().toURL();
		}
		
		@Override
		public void close() throws IOException {
			FileUtils.deleteDirectory(tempPath.toFile());
		}
	}

	@Override
	public String toString() {
		return "DomainModel [model=" + domainWords + "] len="+domainWords.size();
	}
}
