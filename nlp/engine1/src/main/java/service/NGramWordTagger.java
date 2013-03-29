package service;

import domain.BackPointer;
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
    public Map<WordTag,Double> calculateExpectations(Map<String,Integer> tagMap,Map<WordTag,Integer> taggedWords) throws IOException {

        Map<WordTag,Double> expectationMap = new LinkedHashMap<WordTag,Double>();
        for(Entry<WordTag,Integer> entry :taggedWords.entrySet()){
                double expectationOfXgivenY = ((double) entry.getValue())/((double)tagMap.get(entry.getKey().getTag()));
                expectationMap.put(entry.getKey(),expectationOfXgivenY);
        }

        return expectationMap;
    }

    @Override
    public List<String> estimate(String testFileLocation, String outputFileLocation, Map<WordTag, Integer> taggedWords, Map<WordTag, Double> expectationMap) throws IOException {

        Map<String,String> expectedTags = calculateExpectedTagMap(taggedWords,expectationMap);

        List<String> newWords = sentenceReader.getContents(testFileLocation);
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

        outputWriter.write(outputFileLocation,false,estimatedWords);
        return estimatedWords;
    }

    @Override
    public List<String> estimateWithViterbi(String testFileLocation, String outputFileLocation, Map<String, Double> qFunction, Map<WordTag, Double> expectationMap, TagResults tagResults) throws IOException {

        List<String> estimatedWords = new ArrayList<String>();

        List<List<String>> sentences = sentenceReader.readSentences(testFileLocation);
        for (List<String> sentence : sentences){
              String[] words = sentence.toArray(new String[]{});
              estimatedWords.addAll(calculateViterbiEstimates(words,qFunction,expectationMap,tagResults));
              estimatedWords.add("");
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

    @Override
    public Map<String,Double> calculateQFunction(TagResults tagResults){
        Map<NGramTag,Integer> trigramCounts = tagResults.getTrigramTagCountMap();
        Map<NGramTag,Integer> bigramCounts = tagResults.getBigramTagCountMap();
        Map<String,Double> qFunctionResults = new LinkedHashMap<String,Double>();
        calculateQFunction(new String[]{"I-GENE","O","*"},bigramCounts,trigramCounts,qFunctionResults);
        calculateQFunction(new String[]{"O","I-GENE","*"},bigramCounts,trigramCounts,qFunctionResults);
        calculateQFunction(new String[]{"STOP","I-GENE","O","*"},bigramCounts,trigramCounts,qFunctionResults);

        return qFunctionResults;
    }

    @Override
    public List<String> replaceLessFrequentWordTags(String outputFileLocation, TagResults tagResults) throws Exception {

        List<String> toBeReplacedWords = getLowOccurenceWords(tagResults);
        List<String> fixedWordsList = tagResults.getWords();
        List<String> fixedWordTagsList = tagResults.getWordTags();

        List<String> newWordTagsList = new ArrayList<String>();
        for(String wordTag: fixedWordTagsList){
            newWordTagsList.add(wordTag);
        }

        for(String toBeReplacedWord: toBeReplacedWords){
            //don't have to worry about empty lines because they wont make it here
            int index = fixedWordsList.indexOf(toBeReplacedWord);
            fixedWordsList.set(index,"_RARE_");
            newWordTagsList.set(index,"_RARE_" + " " + fixedWordTagsList.get(index).split(" ")[1]);
        }

        outputWriter.write(outputFileLocation, false, newWordTagsList);
        return newWordTagsList;
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

    protected synchronized Map<String,String> calculateExpectedTagMap(Map<WordTag,Integer> taggedWords, Map<WordTag,Double> expectationMap){
        Set<String> setOfWords = new LinkedHashSet<String>();
        Iterator iter = taggedWords.entrySet().iterator();
        while(iter.hasNext()){
            Entry<WordTag,Integer> entry = (Entry) iter.next();
            setOfWords.add(entry.getKey().getWord());
        }

        Map<WordTag,Double> resultWordTags = new LinkedHashMap<WordTag,Double>();

        Map<String,String> expectedTags = new LinkedHashMap<String,String>();

        for(String word: setOfWords){

            Map<Double,String> expToTagMap = new LinkedHashMap<Double,String>();
            Double expOfIGeneAndWord =  expectationMap.containsKey(new WordTag(word,"I-GENE")) ? expectationMap.get(new WordTag(word,"I-GENE")) : 0;
            Double expOfOAndWord =  expectationMap.containsKey(new WordTag(word,"O")) ? expectationMap.get(new WordTag(word,"O")) : 0;

            expToTagMap.put(expOfIGeneAndWord,"I-GENE");
            expToTagMap.put(expOfOAndWord,"O");
            Double maxExpectation = Math.max(expOfIGeneAndWord,expOfOAndWord);
            resultWordTags.put(new WordTag(word, expToTagMap.get(maxExpectation)), Math.max(expOfOAndWord, expOfIGeneAndWord));
            expectedTags.put(word,expToTagMap.get(maxExpectation));
        }
        return expectedTags;
    }

    protected void calculateQFunction(String[] keyTags,Map<NGramTag,Integer> bigramCounts, Map<NGramTag,Integer> trigramCounts, Map<String,Double> qFunctionMap){

            String tag =  keyTags[0];
            for(int i=0; i< keyTags.length; i++){
                for(int j=0; j< keyTags.length; j++){
                    NGramTag bigramTag = new NGramTag(2,keyTags[i],keyTags[j]);
                    NGramTag trigramTag = new NGramTag(3,keyTags[i],new String[]{keyTags[j],tag});
                    int numerator = trigramCounts.containsKey(trigramTag) ? trigramCounts.get(trigramTag) : 0;
                    int denominator = bigramCounts.containsKey(bigramTag) ? bigramCounts.get(bigramTag) : 0;
                    double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
                    if(qFunction > 0) {
                        qFunctionMap.put(tag + "Given" + keyTags[i] + "And" + keyTags[j],qFunction);
                    }

                }
            }
    }

    @Override
    public List<String> getLowOccurenceWords(TagResults tagResults){
        Map<WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();
        Map<String,Integer> wordCountMap = tagResults.getWordCountMap();
        Integer countOfRareAndIGene = 0;
        Integer countOfRareAndO = 0;
        List<String> toBeReplacedWords = new ArrayList<String>();
        Iterator<Entry<String,Integer>> iter = wordCountMap.entrySet().iterator();
        while(iter.hasNext()){
            Entry<String,Integer> entry = iter.next();
            if(entry.getValue() < 5){
                for(int j=0; j< entry.getValue(); j++){
                    toBeReplacedWords.add(entry.getKey());
                }
                int incrementIGeneBy = taggedWords.containsKey(new WordTag(entry.getKey(),"I-GENE")) ? taggedWords.get(new WordTag(entry.getKey(),"I-GENE")):0;
                countOfRareAndIGene = countOfRareAndIGene+incrementIGeneBy;
                int incrementOBy = taggedWords.containsKey(new WordTag(entry.getKey(),"O")) ? taggedWords.get(new WordTag(entry.getKey(),"O")):0;
                countOfRareAndO = countOfRareAndO + incrementOBy;
            }
        }
        return toBeReplacedWords;
    }

    List<String> calculateViterbiEstimates(String[] words, Map<String, Double> qFunction, Map<WordTag, Double> expectationMap, TagResults tagResults){

        BackPointer backPointer = calculatePiMap(words,qFunction,expectationMap,tagResults);
        Map<String, Double> piMap =  backPointer.getPiMap();

        Map<String, String> maxBackPointerMap =  backPointer.getMaxBackPointerMap();


        Map<String,Double> endMap = new LinkedHashMap<String,Double>();
        String[] tags = {"O","I-GENE"};
        for(int i=0; i< tags.length; i++){
            for(int j=0; j<tags.length; j++){
                String lookupKey = "pi(3,"+tags[i]+","+tags[j]+")";
                Double piValue = piMap.containsKey(lookupKey)?piMap.get(lookupKey):0.0F;
                String qKey = "STOP" + "Given" + tags[i] + "And" + tags[j];
                Double qValue = qFunction.containsKey(qKey)? qFunction.get(qKey) : 0.0F;
                endMap.put(new String(tags[i] + "," + tags[j]),piValue*qValue);
            }
        }

        String[] calculatedTags = new String[words.length+1];

        calculatedTags[0] = "*";
        double maxValue = 0.0F;
        for(String key:endMap.keySet()){
            if(endMap.get(key) >= maxValue){
               maxValue = endMap.get(key);
               String[] split = key.split(",");
               calculatedTags[words.length]=split[1];
               calculatedTags[words.length-1]=split[0];
               // maxBackPointerMap.put(words.length+"_"+calculatedTags[words.length-1]+"_"+calculatedTags[words.length],null);
            }
        }


        //printMap(maxBackPointer);
        for(int k = words.length-2; k > 0;k--){
            calculatedTags[k] = maxBackPointerMap.get(Integer.toString(k+2)+"_"+calculatedTags[k+1]+"_"+calculatedTags[k+2]);
        }


        List<String> estimatedWords = new ArrayList<String>();
        for(int blah=1; blah<=words.length; blah++){
            //LOG.info(words[blah-1] + " " + calculatedTags[blah]);
            estimatedWords.add(words[blah-1] + " " + calculatedTags[blah]);
        }
        return estimatedWords;
    }

    protected BackPointer calculatePiMap(String[] words, Map<String, Double> qFunction, Map<WordTag, Double> expectationMap, TagResults tagResults){

        BackPointer backPointer = new BackPointer();
        Map<String, Double> piMap = backPointer.getPiMap();
        Map<String, String> maxBackPointerMap = backPointer.getMaxBackPointerMap();
        piMap.put("pi(0,*,*)",1.0);
        String[] wTags;
        String[] uTags;
        String[] tags = {"O","I-GENE"};
        for(int k=1; k<=words.length; k++){

            if(k == 1){
                wTags = new String[]{"*"};
                uTags = new String[]{"*"};
            } else if (k ==2){
                wTags = new String[]{"*"};
                uTags = new String[]{"O","I-GENE"};
            } else {
                wTags = new String[]{"O","I-GENE"};
                uTags = new String[]{"O","I-GENE"};
            } /*else {
                wTags = new String[]{"O","I-GENE"};
                uTags = new String[]{"O","I-GENE"};
                tags = new String[]{"STOP"};
            }*/

            for(int u=0; u < uTags.length; u++){
                    for(int v=0; v<tags.length; v++){
                        double currentMax = 0.0F;
                        for(int w=0; w<wTags.length; w++){
                            String key = "pi("+k+","+uTags[u]+","+tags[v]+")";
                            WordTag wordTag = new WordTag(words[k-1],tags[v]);
                            double expectation = 0.0F;
                            if(tagResults.getWordCountMap().containsKey(words[k-1]))
                                if (tagResults.getWordCountMap().get(words[k-1]) >= 5) {
                                    expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : 0.0F;
                                } else {
                                    expectation = expectationMap.get(new WordTag("_RARE_",tags[v]));//written seperate because this could be optimized
                            } else {
                                expectation = expectationMap.get(new WordTag("_RARE_",tags[v]));
                            }

                            Double pivalue =  piMap.containsKey("pi("+ new Integer(k-1) +","+wTags[w]+"," + uTags[u]+ ")") ? piMap.get("pi("+ new Integer(k-1) +","+wTags[w]+"," + uTags[u]+ ")") : 0.0F;
                            Double result = pivalue*qFunction.get(tags[v] + "Given" + wTags[w] + "And" + uTags[u])*expectation;
                            if(result > currentMax){ //TODO revisit
                                currentMax = result;
                                piMap.put(key,result);
                                maxBackPointerMap.put(k+"_"+uTags[u]+"_"+tags[v],wTags[w]);
                            }
                        }
                    }
                }
        }
        return backPointer;
    }

    public void printMap(Map map){
        Iterator iter = map.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            LOG.info("Entry: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }

}
