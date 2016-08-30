package org.insightcentre.nlp.saffron.domainmodelling;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CorpusWordList {
    private List<CorpusWord> words;

    public List<CorpusWord> getWords() {
        return words;
    }

    public void setWords(List<CorpusWord> words) {
        this.words = words;
    }
}
