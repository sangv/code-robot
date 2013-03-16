
import domain.NGramTag;
import domain.WordTag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class WordsTest {

    static Map<WordTag,Integer> wordTagCountMap = new HashMap<WordTag, Integer>();

    static Map<NGramTag,Integer> oneGramCountMap = new HashMap<NGramTag, Integer>();

    static List<String> oneGramResults = new ArrayList<String>();

    static List<String> wordTagResults = new ArrayList<String>();


    public static void main(String[] args) throws Exception {

        List<String> input = getContents("/home/sang/Source/code-robot/nlp/engine1/gene.train");

        calculateWordTagCounts(input);

        calculateOneGramCounts(input);

    }

    public static void calculateWordTagCounts(List<String> input){
        for(String str: input){
            if(str != null && str.length() > 0){
                String[] splits = str.split(" ");
                WordTag wordTag = new WordTag(splits[0],splits[1]);
                Integer count = 0;
                if(wordTagCountMap.containsKey(wordTag)){
                    count =  wordTagCountMap.get(wordTag);
                }
                wordTagCountMap.put(wordTag,++count);
            }
        }

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
        if(actual != null && expected != null && !expected.equals(actual)){
            throw new RuntimeException("Assertion failed. Got: " + actual + " Expected: " + expected);
        }
    }

}