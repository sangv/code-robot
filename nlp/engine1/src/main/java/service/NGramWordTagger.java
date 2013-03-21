package service;

import domain.NGramTag;
import domain.TaggedSentence;
import domain.TaggedSentence.WordTag;
import domain.WordTagCounts;
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
                if(!("*".equals(wordTags[i].getTag()) || "STOP".equals(wordTags[i].getTag()) || "_RARE_".equals(wordTags[i].getTag()))){

                    updateCountMap(wordTagCounts.getWordTagCountMap(),wordTags[i]);
                    updateCountMap(wordTagCounts.getTagCountMap(),wordTags[i].getTag());
                    updateCountMap(wordTagCounts.getWordCountMap(),wordTags[i].getWord());
                    wordTagCounts.getWords().add(wordTags[i].getWord());
                    wordTagCounts.getTags().add(wordTags[i].getTag());
                    wordTagCounts.getWordTags().add(wordTags[i].getWord() + " " + wordTags[i].getTag());

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
            wordTagCounts.getWords().add("");
            wordTagCounts.getTags().add("");
            wordTagCounts.getWordTags().add("");
        }
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
    public List<String> replaceLessFrequentWordTags(String outputFileLocation, Map<TaggedSentence.WordTag,Integer> taggedWords) throws Exception {

        Map<String,Integer> wordCountMap = getWordTagCounts().getWordCountMap();

        List<String> toBeReplacedWords = new ArrayList<String>();
        Iterator<Entry<String,Integer>> iter = wordCountMap.entrySet().iterator();
        while(iter.hasNext()){
            Entry<String,Integer> entry = iter.next();
            if(entry.getValue() < 5){
                for(int j=0; j< entry.getValue(); j++){
                    toBeReplacedWords.add(entry.getKey());
                }
            }
        }

        List<String> fixedWordsList = getWordTagCounts().getWords();
        List<String> fixedWordTagsList = getWordTagCounts().getWordTags();
        for(String toBeReplacedWord: toBeReplacedWords){
            //don't have to worry about empty lines because they wont make it here
            int index = fixedWordsList.indexOf(toBeReplacedWord);
            String existingWordTag = fixedWordTagsList.get(index);
            String[] existingWordAndTag = existingWordTag.split(" ");
            String newWordTag = "_RARE_" + " " + existingWordAndTag[1];
            fixedWordTagsList.remove(index);
            fixedWordTagsList.add(index,newWordTag);
        }

        outputWriter.write(outputFileLocation, false, fixedWordTagsList);
        return fixedWordsList;
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

    @Override
    public void setOutputWriter(OutputWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    protected synchronized void updateCountMap(Map map, Object key){
        int count = (Integer) (map.containsKey(key) ? map.get(key) : 0);
        map.put(key, ++count);
    }

}
