package service;

import domain.*;
import domain.Sentence.WordTag;
import org.apache.commons.lang.StringUtils;
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
public abstract class AbstractWordTagger implements WordTagger{

    public static String[] RARE_CLASSES = new String[] {"Numeric","AllCaps","LastCaps","RARE"};

    private SentenceReader sentenceReader;

    private OutputWriter outputWriter;

    private NGramTagResults tagResults;

    private List<Sentence> sentences;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractWordTagger.class);

    @Override
    public void init(String location) throws IOException {
        sentences = sentenceReader.read(location);
        calculateNGramCounts(sentences);
    }

    @Override
    public Map<NGramTag,Integer> getNGramCounts(int length){
        switch (length) {
            case 1:
                return ((NGramTagResults)tagResults).getUnigramTagCountMap();
            case 2:
                return ((NGramTagResults)tagResults).getBigramTagCountMap();
            case 3:
                return ((NGramTagResults)tagResults).getTrigramTagCountMap();
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
    public void invalidate(){
        tagResults = new NGramTagResults();
        sentences = new ArrayList<Sentence>();
    }

    @Override
    public NGramTagResults getTagResults() {
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
    public List<String> replaceLessFrequentWordTags(String outputFileLocation, NGramTagResults tagResults, boolean rareSubClasses) throws Exception {

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
            String rareClass = "_RARE_";
            if(rareSubClasses){
                rareClass = deduceRareSubclass(toBeReplacedWord);
            }
            fixedWordsList.set(index,rareClass);
            newWordTagsList.set(index,rareClass + " " + fixedWordTagsList.get(index).split(" ")[1]);
        }

        outputWriter.write(outputFileLocation, false, newWordTagsList);
        return newWordTagsList;
    }

    protected void calculateNGramCounts(List<Sentence> sentences){

        LOG.info("Pre-calculating NGramCounts");

        tagResults = new NGramTagResults();
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
                    updateCountMap(((NGramTagResults)tagResults).getUnigramTagCountMap(), nGramTag);
                }

                //calculate 2-gram tag counts
                String tagBefore = wordTags[i].getTag();
                if(i < wordTags.length - 1){
                    String tagAfter = wordTags[i+1].getTag();
                    NGramTag nGramTag = new NGramTag(2,tagBefore,tagAfter);
                    updateCountMap(((NGramTagResults)tagResults).getBigramTagCountMap(),nGramTag);

                    //calculate 3-gram tag counts
                    if(i < wordTags.length - 2){
                        String tagAfterAfter = wordTags[i+2].getTag();
                        NGramTag threeGramTag = new NGramTag(3,tagBefore,tagAfter,tagAfterAfter);
                        updateCountMap(((NGramTagResults)tagResults).getTrigramTagCountMap(),threeGramTag);
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
    public List<String> getLowOccurenceWords(NGramTagResults tagResults){
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

    protected DynamicProgrammingResults calculatePiMap(String[] words, Map<String, Double> qFunction, Map<WordTag, Double> expectationMap, NGramTagResults tagResults, boolean useRareSubclasses){

        DynamicProgrammingResults dynamicProgrammingResults = new DynamicProgrammingResults();
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();
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
            }

            for(int u=0; u < uTags.length; u++){
                for(int v=0; v<tags.length; v++){
                    double currentMax = 0.0F;
                    for(int w=0; w<wTags.length; w++){
                        String key = "pi("+k+","+uTags[u]+","+tags[v]+")";
                        WordTag wordTag = new WordTag(words[k-1],tags[v]);
                        double expectation = 0.0F;
                        if(tagResults.getWordCountMap().containsKey(words[k-1]) && tagResults.getWordCountMap().get(words[k-1]) >= 5){
                            expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : 0.0F;
                        } else {
                            String rareClass = "_RARE_";
                            if(useRareSubclasses){
                                rareClass = deduceRareSubclass(words[k-1]);
                            }
                            expectation = expectationMap.get(new WordTag(rareClass,tags[v]));
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
        return dynamicProgrammingResults;
    }


    protected String deduceRareSubclass(String toBeReplacedWord){
        String rareClass = toBeReplacedWord.matches(".*[0-9].*") ? "_NUMERIC_" : null;
        if(rareClass == null){
            rareClass = StringUtils.isAllUpperCase(toBeReplacedWord) ? "_ALL_CAPITALS_" : null;
        }
        if(rareClass == null){
            rareClass = toBeReplacedWord.matches(".*[A-Z]$") ? "_LAST_CAPITAL_" : null;
        }
        if(rareClass == null){
            rareClass = "_RARE_";
        }
        return rareClass;
    }

    public SentenceReader getSentenceReader() {
        return sentenceReader;
    }

    public OutputWriter getOutputWriter() {
        return outputWriter;
    }
}
