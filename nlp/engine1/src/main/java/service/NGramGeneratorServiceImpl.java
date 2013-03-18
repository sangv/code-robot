package service;

import domain.NGramTag;
import domain.WordTag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

    List<List<WordTag>> sentences = new ArrayList<List<WordTag>>();

    public void init(String location) throws IOException {
        List<String> input = getContents(location);
        List<WordTag> wordsInSentence = initWordsInSentence();
        for(String str: input){
            if(str == null || str.length() == 0) {
                sentences.add(completeWordsInSentence(wordsInSentence));

                wordsInSentence = initWordsInSentence();
            } else {
                String[] splits = str.split(" ");
                WordTag wordTag = new WordTag(splits[0],splits[1]);
                wordsInSentence.add(wordTag);
                Integer count = 0;
                if(wordTagCountMap.containsKey(wordTag)){
                    count =  wordTagCountMap.get(wordTag);
                }
                wordTagCountMap.put(wordTag,++count);
            }
        }
        sentences.add(completeWordsInSentence(wordsInSentence));//calling it once after for the last sentence
        calculateOneGramCounts(input);
        calculateNGramCounts();
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

    protected Map<NGramTag,Integer> calculateNGramCounts(){
        Map<NGramTag,Integer> nGramCountMap = new HashMap<NGramTag, Integer>();
        for(List<WordTag> sentence: sentences){
            WordTag[] wordTagsInSentence = sentence.toArray(new WordTag[]{});

            for(int i=0; i< wordTagsInSentence.length; ++i){
                String tagBefore = wordTagsInSentence[i].getTag();
                if(i < wordTagsInSentence.length - 1){
                    String tagAfter = wordTagsInSentence[i+1].getTag();
                    NGramTag nGramTag = new NGramTag(2,tagBefore,tagAfter);
                    int count = 0;
                    if(twoGramCountMap.containsKey(nGramTag)){
                        count =  twoGramCountMap.get(nGramTag);
                    }
                    twoGramCountMap.put(nGramTag,++count);

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

    protected void calculateOneGramCounts(List<String> input){
        for(String str: input){
            if(str != null && str.length() > 0){
                String[] splits = str.split(" ");
                NGramTag nGramTag = new NGramTag(1,splits[1]);
                Integer count = 0;
                if(oneGramCountMap.containsKey(nGramTag)){
                    count =  oneGramCountMap.get(nGramTag);
                }
                oneGramCountMap.put(nGramTag,++count);
            }
        }
    }


    public void invalidate(){
        wordTagCountMap = null;
    }

    public Map<WordTag, Integer> getWordTagCountMap() {
        return wordTagCountMap;
    }

    protected List<WordTag> initWordsInSentence(){
        List<WordTag> wordsInSentence = new ArrayList<WordTag>();
        wordsInSentence.add(new WordTag("None","*"));
        wordsInSentence.add(new WordTag("None","*"));
        return wordsInSentence;
    }

    protected List<WordTag> completeWordsInSentence(List<WordTag> wordsInSentence){
        wordsInSentence.add(new WordTag("None","STOP"));
        return wordsInSentence;
    }

    protected List<String> getContents(String fileLocation) throws IOException {
        FileInputStream fin =  new FileInputStream(fileLocation);
        BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
        List<String> strings = new ArrayList<String>();
        String thisLine;
        while ((thisLine = myInput.readLine()) != null) {
            strings.add(thisLine);
        }
        return strings;
    }
}
