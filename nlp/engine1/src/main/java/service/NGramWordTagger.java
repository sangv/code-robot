package service;

import domain.DynamicProgrammingResults;
import domain.NGramTag;
import domain.Sentence.WordTag;
import domain.TagResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Sang Venkatraman
 */
public class NGramWordTagger extends AbstractWordTagger{

    private static final Logger LOG = LoggerFactory.getLogger(NGramWordTagger.class);


    @Override
    public List<String> estimate(String testFileLocation, String outputFileLocation, Map<WordTag, Double> expectationMap, TagResults tagResults, boolean useRareSubclasses) throws IOException {

        Map<WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();
        Map<String,String> expectedTags = calculateExpectedTagMap(taggedWords,expectationMap);

        List<String> newWords = getSentenceReader().getContents(testFileLocation);
        List<String> estimatedWords = new ArrayList<String>();
        Double expOfIGeneAndRare =  expectationMap.containsKey(new WordTag("_RARE_","I-GENE")) ? expectationMap.get(new WordTag("_RARE_","I-GENE")) : 0;
        Double expOfOAndRare =  expectationMap.containsKey(new WordTag("_RARE_","O")) ? expectationMap.get(new WordTag("_RARE_","O")) : 0;
        String tagForRareWords = expOfOAndRare >= expOfIGeneAndRare? "O" : "I-GENE";
        LOG.info("Tag for rare words is: " + tagForRareWords);
        for(String newWord: newWords){
            if(newWord != null && newWord.length() > 0){
                String tag = expectedTags.containsKey(newWord) ? expectedTags.get(newWord) :tagForRareWords;
                estimatedWords.add(newWord + " " + tag);
            } else {
                estimatedWords.add("");
            }

        }

        getOutputWriter().write(outputFileLocation, false, estimatedWords);
        return estimatedWords;
    }

}
