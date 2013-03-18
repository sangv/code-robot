package service;

import domain.NGramTag;
import domain.WordTag;
import reader.FileBasedSentenceReader;
import reader.SentenceReader;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: sang
 * Date: 3/18/13
 * Time: 3:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class NGramGeneratorServiceImpl {

    Map<WordTag,Integer> wordTagCountMap = new HashMap<WordTag, Integer>();

    Map<NGramTag,Integer> oneGramCountMap =  new HashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> twoGramCountMap =  new HashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> threeGramCountMap =  new HashMap<NGramTag, Integer>();

    SentenceReader sentenceReader = new FileBasedSentenceReader();


    public void init(String location) throws IOException {
        calculateNGramCounts(sentenceReader.read(location));
    }

    public Map<NGramTag,Integer> getNGramCounts(int length){
        switch (length) {
            case 1:
            return oneGramCountMap;
            case 2:
            return twoGramCountMap;
            case 3:
            return threeGramCountMap;
        }
        return null;
    }

    protected Map<NGramTag,Integer> calculateNGramCounts(List<List<WordTag>> sentences){
        Map<NGramTag,Integer> nGramCountMap = new HashMap<NGramTag, Integer>();
        for(List<WordTag> sentence: sentences){
            WordTag[] wordTagsInSentence = sentence.toArray(new WordTag[]{});

            for(int i=0; i< wordTagsInSentence.length; ++i){

                //calculate count of word-tag combinations
                int count = 0;
                if(wordTagCountMap.containsKey(wordTagsInSentence[i])){
                    count =  wordTagCountMap.get(wordTagsInSentence[i]);
                }
                wordTagCountMap.put(wordTagsInSentence[i],++count);

                //calculate 1-gram tag counts
                count = 0;
                NGramTag nGramTag = new NGramTag(1,wordTagsInSentence[i].getTag());
                if(oneGramCountMap.containsKey(nGramTag)){
                    count =  oneGramCountMap.get(nGramTag);
                }
                oneGramCountMap.put(nGramTag,++count);

                //calculate 2-gram tag counts
                String tagBefore = wordTagsInSentence[i].getTag();
                if(i < wordTagsInSentence.length - 1){
                    String tagAfter = wordTagsInSentence[i+1].getTag();
                    nGramTag = new NGramTag(2,tagBefore,tagAfter);
                    count = 0;
                    if(twoGramCountMap.containsKey(nGramTag)){
                        count =  twoGramCountMap.get(nGramTag);
                    }
                    twoGramCountMap.put(nGramTag,++count);

                    //calculate 3-gram tag counts
                    if(i < wordTagsInSentence.length - 2){
                        String tagAfterAfter = wordTagsInSentence[i+2].getTag();
                        NGramTag threeGramTag = new NGramTag(3,tagBefore,tagAfter,tagAfterAfter);

                        count = 0;
                        if(threeGramCountMap.containsKey(threeGramTag)){
                            count =  threeGramCountMap.get(threeGramTag);
                        }
                        threeGramCountMap.put(threeGramTag,++count);
                    }
                }
            }
        }

        return nGramCountMap;
    }

    public void invalidate(){
        wordTagCountMap = null;
    }

    public Map<WordTag, Integer> getWordTagCountMap() {
        return wordTagCountMap;
    }

}
