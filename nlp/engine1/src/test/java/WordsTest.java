
import domain.NGramTag;
import domain.WordTag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class WordsTest {

    static Map<WordTag,Integer> wordTagCountMap = new HashMap<WordTag, Integer>();

    static List<String> wordTagResults = new ArrayList<String>();

    static Map<NGramTag,Integer> oneGramCountMap = new HashMap<NGramTag, Integer>();

    static List<String> oneGramResults = new ArrayList<String>();

    static Map<NGramTag,Integer> twoGramCountMap = new HashMap<NGramTag, Integer>();

    static List<String> twoGramResults = new ArrayList<String>();

    static Map<NGramTag,Integer> threeGramCountMap = new HashMap<NGramTag, Integer>();

    static List<String> threeGramResults = new ArrayList<String>();


    public static void main(String[] args) throws Exception {

        List<String> input = getContents("/home/sang/Source/code-robot/nlp/engine1/src/test/resources/gene.train");

        calculateWordTagCounts(input);

        calculateOneGramCounts(input);

        printResults();

        assertResults();

    }

    public static void calculateWordTagCounts(List<String> input){
        List<WordTag> wordsInSentence = initWordsInSentence();
        for(String str: input){
            if(str == null || str.length() == 0) {
                completeWordsInSentence(wordsInSentence);

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
        completeWordsInSentence(wordsInSentence);//calling it once after for the last sentence
        assertCondition(wordTagCountMap.get(new WordTag("consensus","I-GENE")),new Integer("13"));

        Set<Map.Entry<WordTag,Integer>> entrySet = wordTagCountMap.entrySet();
        Iterator<Map.Entry<WordTag,Integer>> iter = entrySet.iterator();
        while(iter.hasNext()){
            Map.Entry<WordTag,Integer> entry = iter.next();
            wordTagResults.add(entry.getValue() + " WORDTAG " + entry.getKey().getTag() + " " + entry.getKey().getWord());
        }

        for(String wordTagResult: wordTagResults){
            System.out.println(wordTagResult);
        }

    }

    public static void calculateOneGramCounts(List<String> input){
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

        assertCondition(oneGramCountMap.get(new NGramTag(1,"I-GENE")),new Integer("41072"));
        assertCondition(oneGramCountMap.get(new NGramTag(1,"O")),new Integer("345128"));

        Set<Map.Entry<NGramTag,Integer>> entrySet = oneGramCountMap.entrySet();
        Iterator<Map.Entry<NGramTag,Integer>> iter = entrySet.iterator();
        while(iter.hasNext()){
            Map.Entry<NGramTag,Integer> entry = iter.next();
            oneGramResults.add(entry.getValue() + " 1-GRAM " + entry.getKey().getTag());
        }

        for(String oneGramResult: oneGramResults){
            System.out.println(oneGramResult);
        }

    }

    public static void calculateTwoGramCounts(List<WordTag> sentence){
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

    public static void printResults(){

        Set<Map.Entry<NGramTag,Integer>> entrySet = twoGramCountMap.entrySet();
        Iterator<Map.Entry<NGramTag,Integer>> iter = entrySet.iterator();
        while(iter.hasNext()){
            Map.Entry<NGramTag,Integer> entry = iter.next();
            twoGramResults.add(entry.getValue() + " 2-GRAM " + entry.getKey().getTag() + " " + entry.getKey().getOthers()[0]);
        }

        for(String twoGramResult: twoGramResults){
            System.out.println(twoGramResult);
        }

        entrySet = threeGramCountMap.entrySet();
        iter = entrySet.iterator();
        while(iter.hasNext()){
            Map.Entry<NGramTag,Integer> entry = iter.next();
            threeGramResults.add(entry.getValue() + " 3-GRAM " + entry.getKey().getTag() + " " + entry.getKey().getOthers()[0] + " " + entry.getKey().getOthers()[1]);
        }

        for(String threeGramResult: threeGramResults){
            System.out.println(threeGramResult);
        }

    }

    public static void assertResults(){
        assertCondition(twoGramCountMap.get(new NGramTag(2,"I-GENE","I-GENE")),new Integer("24435"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"O","I-GENE")),new Integer("15888"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"I-GENE","O")),new Integer("16624"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"O","STOP")),new Integer("13783"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"I-GENE","STOP")),new Integer("13"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"*","O")),new Integer("13047"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"O","O")),new Integer("315457"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"*","I-GENE")),new Integer("749"));
        assertCondition(twoGramCountMap.get(new NGramTag(2,"*","*")),new Integer("13796"));


        assertCondition(threeGramCountMap.get(new NGramTag(3,"*","*","I-GENE")),new Integer("749"));
        assertCondition(threeGramCountMap.get(new NGramTag(3,"I-GENE","O","O")),new Integer("11320"));
        assertCondition(threeGramCountMap.get(new NGramTag(3,"I-GENE","I-GENE","O")),new Integer("9622"));
        assertCondition(threeGramCountMap.get(new NGramTag(3,"O","O","O")),new Integer("291686"));
        assertCondition(threeGramCountMap.get(new NGramTag(3,"O","I-GENE","STOP")),new Integer("1"));


    }

    private static List<String> getContents(String fileLocation) throws IOException {
        FileInputStream fin =  new FileInputStream(fileLocation);
        BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
        List<String> strings = new ArrayList<String>();
        String thisLine;
        while ((thisLine = myInput.readLine()) != null) {
            strings.add(thisLine);
        }
        return strings;
    }


    public static void assertCondition(Object actual, Object expected){
        if(actual == null || expected == null || !expected.equals(actual)){
            throw new RuntimeException("Assertion failed. Got: " + actual + " Expected: " + expected);
        }
    }

    protected static List<WordTag> initWordsInSentence(){
        List<WordTag> wordsInSentence = new ArrayList<WordTag>();
        wordsInSentence.add(new WordTag("None","*"));
        wordsInSentence.add(new WordTag("None","*"));
        return wordsInSentence;
    }

    protected static void completeWordsInSentence(List<WordTag> wordsInSentence){
        wordsInSentence.add(new WordTag("None","STOP"));//FIXME
        calculateTwoGramCounts(wordsInSentence);
    }

}