package service;

import domain.NGramTag;
import domain.WordTag;
import domain.WordTagCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.SentenceReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Sang Venkatraman
 */
public class NGramWordTagger implements WordTagger{

    SentenceReader sentenceReader;

    private WordTagCounts wordTagCounts;

    private List<List<WordTag>> sentences;

    private static final Logger LOG = LoggerFactory.getLogger(NGramWordTagger.class);

    @Override
    public void init(String location) throws IOException {
        sentences = sentenceReader.read(location);
        calculateNGramCounts(sentences);
    }

    @Override
    public Map<NGramTag,Integer> getNGramCounts(int length){
        switch (length) {
            case 1:
            return wordTagCounts.getOneGramCountMap();
            case 2:
            return wordTagCounts.getTwoGramCountMap();
            case 3:
            return wordTagCounts.getThreeGramCountMap();
        }
        return null;
    }

    protected void calculateNGramCounts(List<List<WordTag>> sentences){

        LOG.info("Pre-calculating NGramCounts");

        wordTagCounts = new WordTagCounts();
        for(List<WordTag> sentence: sentences){
            WordTag[] wordTagsInSentence = sentence.toArray(new WordTag[]{});

            for(int i=0; i< wordTagsInSentence.length; ++i){

                //calculate count of word-tag combinations
                if(!("*".equals(wordTagsInSentence[i].getTag()) || "*".equals(wordTagsInSentence[i].getTag()))){
                    int count = 0;
                    if(wordTagCounts.getWordTagCountMap().containsKey(wordTagsInSentence[i])){
                        count =  wordTagCounts.getWordTagCountMap().get(wordTagsInSentence[i]);
                    }
                    wordTagCounts.getWordTagCountMap().put(wordTagsInSentence[i], ++count);

                    //calculate 1-gram tag counts
                    count = 0;
                    NGramTag nGramTag = new NGramTag(1,wordTagsInSentence[i].getTag());
                    if(wordTagCounts.getOneGramCountMap().containsKey(nGramTag)){
                        count =  wordTagCounts.getOneGramCountMap().get(nGramTag);
                    }
                    wordTagCounts.getOneGramCountMap().put(nGramTag, ++count);
                }

                //calculate 2-gram tag counts
                String tagBefore = wordTagsInSentence[i].getTag();
                if(i < wordTagsInSentence.length - 1){
                    String tagAfter = wordTagsInSentence[i+1].getTag();
                    NGramTag nGramTag = new NGramTag(2,tagBefore,tagAfter);
                    int count = 0;
                    if(wordTagCounts.getTwoGramCountMap().containsKey(nGramTag)){
                        count =  wordTagCounts.getTwoGramCountMap().get(nGramTag);
                    }
                    wordTagCounts.getTwoGramCountMap().put(nGramTag, ++count);

                    //calculate 3-gram tag counts
                    if(i < wordTagsInSentence.length - 2){
                        String tagAfterAfter = wordTagsInSentence[i+2].getTag();
                        NGramTag threeGramTag = new NGramTag(3,tagBefore,tagAfter,tagAfterAfter);

                        count = 0;
                        if(wordTagCounts.getThreeGramCountMap().containsKey(threeGramTag)){
                            count =  wordTagCounts.getThreeGramCountMap().get(threeGramTag);
                        }
                        wordTagCounts.getThreeGramCountMap().put(threeGramTag, ++count);
                    }
                }
            }
        }
    }

    @Override
    public void invalidate(){
        wordTagCounts = new WordTagCounts();
        sentences = null;
    }

    @Override
    public WordTagCounts getWordTagCounts() {
        return wordTagCounts;
    }

    @Override
    public void setSentenceReader(SentenceReader sentenceReader) {
        this.sentenceReader = sentenceReader;
    }

}
