import domain.NGramTag;
import domain.TaggedSentence;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.FileBasedSentenceReader;
import service.NGramWordTagger;
import service.WordTagger;

import domain.TaggedSentence.WordTag;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static junit.framework.Assert.assertEquals;

public class WordsTest {

    private WordTagger wordTagger;

    private static final Logger LOG = LoggerFactory.getLogger(WordsTest.class);

    @Before
    public void setUp() throws Exception {
        wordTagger = new NGramWordTagger();
        wordTagger.setSentenceReader(new FileBasedSentenceReader());
        wordTagger.init("src/test/resources/gene.train");
    }

    @Test
    public void assertOneGramCounts(){
        Map<NGramTag,Integer> oneGramCountMap = wordTagger.getNGramCounts(1);
        assertEquals(oneGramCountMap.get(new NGramTag(1,"I-GENE")),new Integer("41072"));
        assertEquals(oneGramCountMap.get(new NGramTag(1,"O")),new Integer("345128"));
    }

    @Test
    public void assertTwoGramResults(){
        Map<NGramTag,Integer> twoGramCountMap = wordTagger.getNGramCounts(2);
        assertEquals(twoGramCountMap.get(new NGramTag(2,"I-GENE","I-GENE")),new Integer("24435"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"O","I-GENE")),new Integer("15888"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"I-GENE","O")),new Integer("16624"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"O","STOP")),new Integer("13783"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"I-GENE","STOP")),new Integer("13"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"*","O")),new Integer("13047"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"O","O")),new Integer("315457"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"*","I-GENE")),new Integer("749"));
        assertEquals(twoGramCountMap.get(new NGramTag(2,"*","*")),new Integer("13796"));
   }

    @Test
    public void assertThreeGramResults(){
        Map<NGramTag,Integer> threeGramCountMap = wordTagger.getNGramCounts(3);
        assertEquals(threeGramCountMap.get(new NGramTag(3,"*","*","I-GENE")),new Integer("749"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"I-GENE","O","O")),new Integer("11320"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"I-GENE","I-GENE","O")),new Integer("9622"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"O","O","O")),new Integer("291686"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"O","I-GENE","STOP")),new Integer("1"));
    }

    @Test
    public void testWordTags(){
        assertEquals(wordTagger.getWordTagCounts().getWordTagCountMap().get(new WordTag("consensus", "I-GENE")),new Integer("13"));
    }

    @Test
    public void testReplaceLessFrequentWordTags() throws IOException {
        Map<TaggedSentence.WordTag,Integer> taggedWords = wordTagger.getWordTagCounts().getWordTagCountMap();
        Map<String,Integer> wordCountMap = wordTagger.getWordTagCounts().getWordCountMap();

        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","O")));

        Map<String,Integer> toBeReplacedWords = new LinkedHashMap<String,Integer>();
        Iterator<Entry<WordTag,Integer>> iter = taggedWords.entrySet().iterator();
        while(iter.hasNext()){
            Entry<WordTag,Integer> entry = iter.next();
            if(entry.getValue() < 5){
               toBeReplacedWords.put(entry.getKey().getWord(),entry.getValue());
            }
        }

        for(Entry<String,Integer> entry :toBeReplacedWords.entrySet()){
            taggedWords.remove(new WordTag(entry.getKey(),"I-GENE")); //removing all possible tags
            taggedWords.remove(new WordTag(entry.getKey(),"O"));
            taggedWords.put(new WordTag(entry.getKey(), "_RARE_"), entry.getValue());
        }
        writeToFile(new File("src/test/resources/reduced_count.out"),false,wordTagger.getWordTagCounts().getWords());
        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","_RARE_")));
        //printMap(wordTagger.getWordTagCounts().getTagCountMap());
        Integer iGeneTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("I-GENE");
        Integer oTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("O");
        Integer rareTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("_RARE_");
        calculateExpectation(wordTagger.getWordTagCounts().getTagCountMap(),taggedWords);
    }

    private Map<WordTag,Float> calculateExpectation(Map<String,Integer> tagMap,Map<WordTag,Integer> taggedWords) throws IOException {
        Set<String> setOfWords = new LinkedHashSet<String>();
        Iterator iter = taggedWords.entrySet().iterator();
        while(iter.hasNext()){
            Entry<WordTag,Integer> entry = (Entry) iter.next();
            setOfWords.add(entry.getKey().getWord());
        }

        Map<WordTag,Float> expectationMap = new LinkedHashMap<WordTag,Float>();
        for(Entry<WordTag,Integer> entry :taggedWords.entrySet()){
            if(!"_RARE_".equals(entry.getKey().getTag())) {
                Float expectationOfXgivenY = ((float) entry.getValue())/((float)tagMap.get(entry.getKey().getTag()));
                expectationMap.put(entry.getKey(),expectationOfXgivenY);
            }
        }
        //printMap(expectationMap);

        Map<WordTag,Float> resultWordTags = new LinkedHashMap<WordTag,Float>();

        Map<String,String> expectedTags = new LinkedHashMap<String,String>();

        for(String word: setOfWords){

            Map<Float,String> expToTagMap = new LinkedHashMap<Float,String>();
            //float expOfRareAndWord =  expectationMap.containsKey(new WordTag(word,"_RARE_")) ? expectationMap.get(new WordTag(word,"_RARE_")) : 0;
            float expOfIGeneAndWord =  expectationMap.containsKey(new WordTag(word,"I-GENE")) ? expectationMap.get(new WordTag(word,"I-GENE")) : 0;
            float expOfOAndWord =  expectationMap.containsKey(new WordTag(word,"O")) ? expectationMap.get(new WordTag(word,"O")) : 0;

            //expToTagMap.put(expOfRareAndWord,"_RARE_");
            expToTagMap.put(expOfIGeneAndWord,"I-GENE");
            expToTagMap.put(expOfOAndWord,"O");
            float maxExpectation = Math.max(expOfIGeneAndWord,expOfOAndWord);
            resultWordTags.put(new WordTag(word, expToTagMap.get(maxExpectation)), Math.max(expOfOAndWord, expOfIGeneAndWord));
            expectedTags.put(word,expToTagMap.get(maxExpectation));
        }

        //printMap(resultWordTags);

        List<String> newWords = getContents("src/test/resources/gene.dev");
        List<String> estimatedWords = new ArrayList<String>();
        for(String newWord: newWords){
            if(newWord != null && newWord.length() > 0){
                String tag = expectedTags.containsKey(newWord) ? expectedTags.get(newWord) : "_RARE_";
                estimatedWords.add(newWord + " " + tag);
            } else {
                estimatedWords.add("");
            }

        }
        //printMap(estimatedWords);
        assertEquals(newWords.size(),estimatedWords.size());
        writeToFile(new File("src/test/resources/gene_dev.p1.out"),false,estimatedWords);
        return expectationMap;
    }

    public void printMap(Map map){
        Iterator iter = map.entrySet().iterator();
        while(iter.hasNext()){
           Map.Entry entry = (Map.Entry) iter.next();
           LOG.info("Entry: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
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

    protected void writeToFile(File file, boolean append,List<String> list) throws IOException {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new java.io.FileWriter(file, append));
            for(String estimatedWord: list){
                bufferedWriter.write(estimatedWord);
                bufferedWriter.newLine();
            }
        } finally {
            bufferedWriter.flush();
            bufferedWriter.close();
        }
    }
    
}