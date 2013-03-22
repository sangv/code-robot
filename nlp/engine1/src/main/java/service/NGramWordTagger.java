package service;

import domain.NGramTag;
import domain.Sentence;
import domain.Sentence.WordTag;
import domain.TagResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.SentenceReader;
import writer.OutputWriter;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Sang Venkatraman
 */
public class NGramWordTagger implements WordTagger{

    private SentenceReader sentenceReader;

    private OutputWriter outputWriter;

    private TagResults tagResults;

    private List<Sentence> sentences;

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
            return tagResults.getUnigramTagCountMap();
            case 2:
            return tagResults.getBigramTagCountMap();
            case 3:
            return tagResults.getTrigramTagCountMap();
        }
        return null;
    }

    @Override
    public Map<WordTag,Float> calculateExpectations(Map<String,Integer> tagMap,Map<WordTag,Integer> taggedWords) throws IOException {

        Map<WordTag,Float> expectationMap = new LinkedHashMap<WordTag,Float>();
        for(Entry<WordTag,Integer> entry :taggedWords.entrySet()){
                Float expectationOfXgivenY = ((float) entry.getValue())/((float)tagMap.get(entry.getKey().getTag()));
                expectationMap.put(entry.getKey(),expectationOfXgivenY);
        }

        return expectationMap;
    }

    @Override
    public List<String> estimate(String testFileLocation, String outputFileLocation, Map<WordTag,Integer> taggedWords, Map<WordTag,Float> expectationMap) throws IOException {

        Set<String> setOfWords = new LinkedHashSet<String>();
        Iterator iter = taggedWords.entrySet().iterator();
        while(iter.hasNext()){
            Entry<WordTag,Integer> entry = (Entry) iter.next();
            setOfWords.add(entry.getKey().getWord());
        }

        Map<WordTag,Float> resultWordTags = new LinkedHashMap<WordTag,Float>();

        Map<String,String> expectedTags = new LinkedHashMap<String,String>();

        for(String word: setOfWords){

            Map<Float,String> expToTagMap = new LinkedHashMap<Float,String>();
            float expOfIGeneAndWord =  expectationMap.containsKey(new WordTag(word,"I-GENE")) ? expectationMap.get(new WordTag(word,"I-GENE")) : 0;
            float expOfOAndWord =  expectationMap.containsKey(new WordTag(word,"O")) ? expectationMap.get(new WordTag(word,"O")) : 0;

            expToTagMap.put(expOfIGeneAndWord,"I-GENE");
            expToTagMap.put(expOfOAndWord,"O");
            float maxExpectation = Math.max(expOfIGeneAndWord,expOfOAndWord);
            resultWordTags.put(new WordTag(word, expToTagMap.get(maxExpectation)), Math.max(expOfOAndWord, expOfIGeneAndWord));
            expectedTags.put(word,expToTagMap.get(maxExpectation));
        }

        List<String> newWords = sentenceReader.getContents(testFileLocation);
        List<String> estimatedWords = new ArrayList<String>();
        float expOfIGeneAndRare =  expectationMap.containsKey(new WordTag("_RARE_","I-GENE")) ? expectationMap.get(new WordTag("_RARE_","I-GENE")) : 0;
        float expOfOAndRare =  expectationMap.containsKey(new WordTag("_RARE_","O")) ? expectationMap.get(new WordTag("_RARE_","O")) : 0;
        String tagForRareWords = expOfOAndRare >= expOfIGeneAndRare? "O" : "I-GENE";
        LOG.info("Tag for rare words is: " + tagForRareWords);
        for(String newWord: newWords){
            if(newWord != null && newWord.length() > 0){
                String tag = expectedTags.containsKey(newWord) ? expectedTags.get(newWord) : tagForRareWords;
                estimatedWords.add(newWord + " " + tag);
            } else {
                estimatedWords.add("");
            }

        }

        outputWriter.write(outputFileLocation,false,estimatedWords);
        return estimatedWords;
    }

    @Override
    public void invalidate(){
        tagResults = new TagResults();
        sentences = new ArrayList<Sentence>();
    }

    @Override
    public TagResults getTagResults() {
        return tagResults;
    }

    @Override
    public void setSentenceReader(SentenceReader sentenceReader) {
        this.sentenceReader = sentenceReader;
    }

    @Override
    public void setOutputWriter(OutputWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    protected void calculateNGramCounts(List<Sentence> sentences){

        LOG.info("Pre-calculating NGramCounts");

        tagResults = new TagResults();
        for(Sentence sentence: sentences){

            WordTag[] wordTags =  sentence.getWordTags().toArray(new WordTag[]{});
            for(int i=0; i< wordTags.length; i++){

                //calculate count of word-tag combinations
                if(!("*".equals(wordTags[i].getTag()) || "STOP".equals(wordTags[i].getTag()) || "_RARE_".equals(wordTags[i].getTag()))){

                    updateCountMap(tagResults.getWordTagCountMap(),wordTags[i]);
                    updateCountMap(tagResults.getTagCountMap(),wordTags[i].getTag());
                    updateCountMap(tagResults.getWordCountMap(),wordTags[i].getWord());
                    tagResults.getWords().add(wordTags[i].getWord());
                    tagResults.getTags().add(wordTags[i].getTag());
                    tagResults.getWordTags().add(wordTags[i].getWord() + " " + wordTags[i].getTag());

                    NGramTag nGramTag = new NGramTag(1,wordTags[i].getTag());
                    updateCountMap(tagResults.getUnigramTagCountMap(), nGramTag);
                }

                //calculate 2-gram tag counts
                String tagBefore = wordTags[i].getTag();
                if(i < wordTags.length - 1){
                    String tagAfter = wordTags[i+1].getTag();
                    NGramTag nGramTag = new NGramTag(2,tagBefore,tagAfter);
                    updateCountMap(tagResults.getBigramTagCountMap(),nGramTag);

                    //calculate 3-gram tag counts
                    if(i < wordTags.length - 2){
                        String tagAfterAfter = wordTags[i+2].getTag();
                        NGramTag threeGramTag = new NGramTag(3,tagBefore,tagAfter,tagAfterAfter);
                        updateCountMap(tagResults.getTrigramTagCountMap(),threeGramTag);
                    }
                }
            }
            tagResults.getWords().add("");
            tagResults.getTags().add("");
            tagResults.getWordTags().add("");
        }
    }

    protected synchronized void updateCountMap(Map map, Object key){
        int count = (Integer) (map.containsKey(key) ? map.get(key) : 0);
        map.put(key, ++count);
    }

}
