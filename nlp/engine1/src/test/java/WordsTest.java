import domain.NGramTag;
import domain.TaggedSentence;
import domain.TaggedSentence.WordTag;
import evaluator.FMeasureEvaluationMetric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.FileBasedSentenceReader;
import reader.SentenceReader;
import service.NGramWordTagger;
import service.WordTagger;
import writer.FileOutputWriter;
import writer.OutputWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class WordsTest {

    private WordTagger wordTagger;

    private OutputWriter outputWriter = new FileOutputWriter();

    private SentenceReader sentenceReader = new FileBasedSentenceReader();

    private static final Logger LOG = LoggerFactory.getLogger(WordsTest.class);

    @Before
    public void setUp() throws Exception {
        wordTagger = new NGramWordTagger();
        wordTagger.setSentenceReader(sentenceReader);
        wordTagger.setOutputWriter(outputWriter);
    }

    @Test
    public void assertOneGramCounts() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        Map<NGramTag,Integer> oneGramCountMap = wordTagger.getNGramCounts(1);
        assertEquals(oneGramCountMap.get(new NGramTag(1,"I-GENE")),new Integer("41072"));
        assertEquals(oneGramCountMap.get(new NGramTag(1,"O")),new Integer("345128"));
    }

    @Test
    public void assertTwoGramResults() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
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
    public void assertThreeGramResults() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        Map<NGramTag,Integer> threeGramCountMap = wordTagger.getNGramCounts(3);
        assertEquals(threeGramCountMap.get(new NGramTag(3,"*","*","I-GENE")),new Integer("749"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"I-GENE","O","O")),new Integer("11320"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"I-GENE","I-GENE","O")),new Integer("9622"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"O","O","O")),new Integer("291686"));
        assertEquals(threeGramCountMap.get(new NGramTag(3,"O","I-GENE","STOP")),new Integer("1"));
    }

    @Test
    public void testWordTags() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        assertEquals(wordTagger.getWordTagCounts().getWordTagCountMap().get(new WordTag("consensus", "I-GENE")),new Integer("13"));
    }

    @Test
    @Ignore
    public void testReplaceLessFrequentWordTags() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        Map<TaggedSentence.WordTag,Integer> taggedWords = wordTagger.getWordTagCounts().getWordTagCountMap();
        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","O")));
        assertEquals(399996,wordTagger.getWordTagCounts().getWords().size());

        List<String> replacedWordTagsList = wordTagger.replaceLessFrequentWordTags("src/test/resources/reduced_count.out",taggedWords);

        assertTrue(replacedWordTagsList.contains("M O"));
        assertTrue(replacedWordTagsList.contains(". O"));
        assertTrue(replacedWordTagsList.contains("_RARE_ O"));
        outputWriter.write("src/test/resources/reduced_count.out", false, replacedWordTagsList);
    }

    @Test
    public void generateUnigramCounts() throws IOException {
        wordTagger.init("src/test/resources/reduced_count.out");
        Map<TaggedSentence.WordTag,Integer> taggedWords = wordTagger.getWordTagCounts().getWordTagCountMap();
        Integer iGeneTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("I-GENE");
        Integer oTagCount = wordTagger.getWordTagCounts().getTagCountMap().get("O");
        Map<WordTag,Float> expectationsMap = wordTagger.calculateExpectations(wordTagger.getWordTagCounts().getTagCountMap(), taggedWords);
        wordTagger.estimate("src/test/resources/gene.dev","src/test/resources/gene_dev.p1.out",taggedWords,expectationsMap);
    }

    @Test
    public void evaluate() throws IOException {
        FMeasureEvaluationMetric evaluator = new FMeasureEvaluationMetric();
        assertEquals(1.0D,evaluator.calculateMetric("src/test/resources/gene.key.copy","src/test/resources/gene.key","I-GENE"));
        assertEquals(0.5092661230541141D,evaluator.calculateMetric("src/test/resources/gene_dev.p1.out","src/test/resources/gene.key","I-GENE"));
    }

    public void printMap(Map map){
        Iterator iter = map.entrySet().iterator();
        while(iter.hasNext()){
           Map.Entry entry = (Map.Entry) iter.next();
           LOG.info("Entry: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }
    
}