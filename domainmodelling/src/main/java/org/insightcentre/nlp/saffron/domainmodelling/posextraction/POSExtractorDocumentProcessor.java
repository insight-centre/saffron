package org.insightcentre.nlp.saffron.domainmodelling.posextraction;

import java.util.Map;

public final class POSExtractorDocumentProcessor implements DocumentProcessor {
  public ExtractionResultsWrapper processDocument(String filePath,
      Integer lengthThresh, ExtractionResultsWrapper erw, StatProcessor sp) {
    ExtractionResultsWrapper erwTmp = sp.extractNLPInfo(filePath, lengthThresh);
    if (erwTmp == null) {
    	return erw;
    }
    
    erw.setNounPhraseMap(erw.mergeNPMap(erwTmp.getNounPhraseMap(), erw.getNounPhraseMap()));
    erw.setNounsMap(erw.mergeMap(erwTmp.getNounsMap(), erw.getNounsMap(), true));
    erw.setVerbsMap(erw.mergeMap(erwTmp.getVerbsMap(), erw.getVerbsMap(), true));
    erw.setAdjsMap(erw.mergeMap(erwTmp.getAdjsMap(), erw.getAdjsMap(), true));
    erw.setTokensNo(erw.getTokensNo() + erwTmp.getTokensNo());

    if (erw.getDocCount() == null) {
      erw.setDocCount(1);
    } else {
      erw.setDocCount(erw.getDocCount() + 1);
    }

    Map<String, Integer> docMap = erw.getDocsLengthMap();
    docMap.put(filePath.substring(filePath.lastIndexOf("/") + 1), erwTmp
        .getTokensNo());

    return erw;
  }
}
