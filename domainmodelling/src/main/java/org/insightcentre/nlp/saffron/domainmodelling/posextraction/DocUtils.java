/**
 * 
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction;


import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Keyphrase;


/**
 * @author Georgeta Bordea
 * 
 */
public class DocUtils {


  private ExtractionResultsWrapper erw =
      new ExtractionResultsWrapper(new HashMap<String, Keyphrase>(),
          new HashMap<String, Long>(), new HashMap<String, Long>(),
          new HashMap<String, Long>(), 0, 0);

  public ExtractionResultsWrapper getErw() {
    return erw;
  }

  public void processDocs(Collection<File> files, DocumentProcessor dp, Integer lengthThreshold, StatProcessor sp) {
      for (File file : files) {
          String filePath = file.getAbsolutePath();
          erw = dp.processDocument(filePath, lengthThreshold, erw, sp);
      }
  }

  public void processDocs(File file, DocumentProcessor dp,
      Integer lengthThreshold, StatProcessor sp) {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            processDocs(new File(file, files[i]), dp, lengthThreshold,
                sp);
          }
        }
      } else {
          String filePath = file.getAbsolutePath();
          // TODO process with gate, index, count nouns
          erw = dp.processDocument(filePath, lengthThreshold, erw, sp);
      }
    }
  }
}
