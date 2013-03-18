import domain.NGramTag;
import domain.WordTag;
import org.junit.BeforeClass;
import org.junit.Test;
import service.NGramGeneratorServiceImpl;

import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class WordsTest {

    private static NGramGeneratorServiceImpl nGramGenerator;

    @BeforeClass
    public static void setUp() throws Exception {
        nGramGenerator = new NGramGeneratorServiceImpl();
        nGramGenerator.init("src/test/resources/gene.train");
    }

    @Test
    public void assertOneGramCounts(){
        Map<NGramTag,Integer> oneGramCountMap = nGramGenerator.getNGramCounts(1);
        assertEquals(oneGramCountMap.get(new NGramTag(1,"I-GENE")),new Integer("41072"));
        assertEquals(oneGramCountMap.get(new NGramTag(1,"O")),new Integer("345128"));
    }

    @Test
    public void assertTwoGramResults(){
        Map<NGramTag,Integer> twoGramCountMap = nGramGenerator.getNGramCounts(2);
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
        Map<NGramTag,Integer> threeGramCountMap = nGramGenerator.getNGramCounts(3);
        assertEquals(threeGramCountMap.get(new NGramTag(3,"*","*","I-GENE")),new Integer("749"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"I-GENE","O","O")),new Integer("11320"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"I-GENE","I-GENE","O")),new Integer("9622"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"O","O","O")),new Integer("291686"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"O","I-GENE","STOP")),new Integer("1"));
    }

    @Test
    public void testWordTags(){
        assertEquals(nGramGenerator.getWordTagCountMap().get(new WordTag("consensus", "I-GENE")),new Integer("13"));
    }
}