package service;

import domain.NGramTag;
import domain.TaggedSentence;
import domain.TaggedSentence.WordTag;
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

    private List<TaggedSentence> sentences;

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

    protected void calculateNGramCounts(List<TaggedSentence> sentences){

        LOG.info("Pre-calculating NGramCounts");

        wordTagCounts = new WordTagCounts();
        for(TaggedSentence sentence: sentences){

            WordTag[] wordTags =  sentence.getWordTags().toArray(new WordTag[]{});
            for(int i=0; i< wordTags.length; i++){

                //calculate count of word-tag combinations
                if(!("*".equals(wordTags[i].getTag()) || "STOP".equals(wordTags[i].getTag()))){

                    updateCountMap(wordTagCounts.getWordTagCountMap(),wordTags[i]);
                    updateCountMap(wordTagCounts.getTagCountMap(),wordTags[i].getTag());

                    NGramTag nGramTag = new NGramTag(1,wordTags[i].getTag());
                    updateCountMap(wordTagCounts.getOneGramCountMap(),nGramTag);
                }

                //calculate 2-gram tag counts
                String tagBefore = wordTags[i].getTag();
                if(i < wordTags.length - 1){
                    String tagAfter = wordTags[i+1].getTag();
                    NGramTag nGramTag = new NGramTag(2,tagBefore,tagAfter);
                    updateCountMap(wordTagCounts.getTwoGramCountMap(),nGramTag);

                    //calculate 3-gram tag counts
                    if(i < wordTags.length - 2){
                        String tagAfterAfter = wordTags[i+2].getTag();
                        NGramTag threeGramTag = new NGramTag(3,tagBefore,tagAfter,tagAfterAfter);
                        updateCountMap(wordTagCounts.getThreeGramCountMap(),threeGramTag);
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

    protected synchronized void updateCountMap(Map map, Object key){
        int count = (Integer) (map.containsKey(key) ? map.get(key) : 0);
        map.put(key, ++count);
    }

}
