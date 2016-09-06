package org.insightcentre.nlp.saffron.topicextraction.filter;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.topicextraction.util.WordUtils;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;

public class TokenCountRangeFilter implements ExtractedTopicsFilter {
    private Integer minTokens;
    private Integer maxTokens;

    public TokenCountRangeFilter(Integer minTokens, Integer maxTokens) {
        this.minTokens = minTokens;
        this.maxTokens = maxTokens;
    }

    @Override
    public List<ExtractedTopic> filter(List<ExtractedTopic> extracted, Set<String> stopWords)  {
        List<ExtractedTopic> filtered = new ArrayList<ExtractedTopic>();

        for (ExtractedTopic extractedTopic : extracted) {
            int tokens = WordUtils.computeTokensNo(extractedTopic.getPattern());
            if (tokens >= minTokens && tokens <= maxTokens) {
                filtered.add(extractedTopic);
            }
        }

        return filtered;
    }
}
