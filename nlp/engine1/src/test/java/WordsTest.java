import domain.NGramTag;
import domain.WordTag;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.FileBasedSentenceReader;
import service.NGramWordTagger;
import service.WordTagger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static junit.framework.Assert.assertEquals;

public class WordsTest {

    private static WordTagger wordTagger;

    private static final Logger LOG = LoggerFactory.getLogger(WordsTest.class);

    @BeforeClass
    public static void setUp() throws Exception {
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
    public void testReplaceLessFrequentWordTags(){
        Map<WordTag,Integer> taggedWords = wordTagger.getWordTagCounts().getWordTagCountMap();
        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","O")));

        Map<WordTag,Integer> toBeReplacedWords = new HashMap<WordTag,Integer>();
        Iterator<Entry<WordTag,Integer>> iter = taggedWords.entrySet().iterator();
        while(iter.hasNext()){
            Entry<WordTag,Integer> entry = iter.next();
            if(entry.getValue() < 5){
               toBeReplacedWords.put(new WordTag(entry.getKey().getWord(),entry.getKey().getTag()),entry.getValue());
            }
        }

        for(Entry<WordTag,Integer> entry :toBeReplacedWords.entrySet()){
            taggedWords.remove(new WordTag(entry.getKey().getWord(),entry.getKey().getTag()));
            taggedWords.put(new WordTag(entry.getKey().getWord(), "_RARE_"), entry.getValue());

        }
        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","_RARE_")));
        printMap(wordTagger.getWordTagCounts().getTagCountMap());
        Integer iGeneTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("I-GENE");
        Integer oTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("O");
        calculateExpectation(iGeneTagCount,oTagCount,taggedWords);
    }

    private Map<WordTag,Float> calculateExpectation(Integer iGeneTagCount, Integer oTagCount,Map<WordTag,Integer> taggedWords) {
        Map<WordTag,Float> expectationMap = new HashMap<WordTag,Float>();
        for(Entry<WordTag,Integer> entry :taggedWords.entrySet()){
            Float expectationOfXgivenY = ((float) entry.getValue())/((float)iGeneTagCount);
            expectationMap.put(entry.getKey(),expectationOfXgivenY);
        }
        printMap(expectationMap);
        return expectationMap;
    }

    public void printMap(Map map){
        Iterator iter = map.entrySet().iterator();
        while(iter.hasNext()){
           Map.Entry entry = (Map.Entry) iter.next();
           LOG.info("Entry: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }
    
    
}