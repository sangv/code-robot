package service;

import domain.NGramTag;
import domain.Sentence;
import domain.Sentence.WordTag;
import domain.TagResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.FileBasedSentenceReader;
import reader.SentenceReader;
import writer.FileOutputWriter;
import writer.OutputWriter;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

public class WordTaggerTest {

    private WordTagger wordTagger = new NGramWordTagger();

    private OutputWriter outputWriter = new FileOutputWriter();

    private SentenceReader sentenceReader = new FileBasedSentenceReader();

    private static final Logger LOG = LoggerFactory.getLogger(WordTaggerTest.class);

    @Before
    public void setUp() throws Exception {
        wordTagger.setSentenceReader(sentenceReader);
        wordTagger.setOutputWriter(outputWriter);
    }

    @After
    public void tearDown() throws Exception {
        wordTagger.invalidate();
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
        assertEquals(threeGramCountMap.get(new NGramTag(3, "O", "I-GENE", "STOP")),new Integer("1"));
    }

    @Test
    public void testWordTags() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        assertEquals(wordTagger.getTagResults().getWordTagCountMap().get(new WordTag("consensus", "I-GENE")),new Integer("13"));
    }

    @Test
    public void generateUnigramCounts() throws IOException {
        wordTagger.init("src/test/resources/gene.train");
        TagResults originalTagResults = wordTagger.getTagResults();
        Map<WordTag,Double> originalExpectationsMap = wordTagger.calculateExpectations(originalTagResults.getTagCountMap(), originalTagResults.getWordTagCountMap());
        wordTagger.invalidate();

        wordTagger.init("src/test/resources/reduced_count.out");
        TagResults tagResults = wordTagger.getTagResults();
        assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(new WordTag("_RARE_","O")));
        Map<Sentence.WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();

        Map<WordTag,Double> expectationsMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(), taggedWords);
        //List<String> estimatedWordTags = wordTagger.estimate("src/test/resources/gene.dev","src/test/resources/gene_dev.p1.out",taggedWords,expectationsMap);
        //assertTrue(estimatedWordTags.contains("BACKGROUND O"));
        List<String> estimatedWordTags = wordTagger.estimate("src/test/resources/gene.dev","src/test/resources/gene_dev.p1.out",expectationsMap,tagResults,false);
        List<String> estimatedTestWordTags = wordTagger.estimate("src/test/resources/gene.test","src/test/resources/gene_test.p1.out",expectationsMap,tagResults,false);
    }

    @Test
    public void calculateQFunctions() throws IOException {

        wordTagger.init("src/test/resources/reduced_count.out");
        TagResults tagResults = wordTagger.getTagResults();
        Map<WordTag,Double> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
        assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(new WordTag("_RARE_","O")));

        Map<String,Double> qFunction = wordTagger.calculateQFunction(tagResults);

        assertNotNull(qFunction);
        assertEquals(21,qFunction.size());
        String[] existingTags = new String[]{"O","*","*"};


        WordTag rareWithTagO = new WordTag("_RARE_","O");

        double expectationOfRAREGivenO =  (double)tagResults.getWordTagCountMap().get(rareWithTagO)/(double)tagResults.getTagCountMap().get("O");
        assertEquals(0.9456403565992607, qFunction.get(existingTags[0] + "Given" + existingTags[1] + "And" + existingTags[2]));

        assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(rareWithTagO));
        assertEquals(new Integer(345128),tagResults.getTagCountMap().get("O"));

        assertEquals(0.08339224867295612,expectationMap.get(rareWithTagO));

        Map<String, Double> piMap = new LinkedHashMap<String,Double>();
        piMap.put("pi(0,*,*)", 1.0);
        assertEquals(0.07885907577270845,piMap.get("pi(0,*,*)")*qFunction.get(existingTags[0] + "Given" + existingTags[1] + "And" + existingTags[2])*expectationOfRAREGivenO);
    }

    @Test
    @Ignore
    public void testReplaceLessFrequentWordTags() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        TagResults tagResults = wordTagger.getTagResults();
        Map<WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();
        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","O")));
        assertEquals(399996,tagResults.getWords().size());

        List<String> reducedCountWords = wordTagger.replaceLessFrequentWordTags("src/test/resources/reduced_count.out",tagResults,false);

        String reducedCountFileLocation = "src/test/resources/reduced_count.out";

        List<String> replacedWordTagsList = sentenceReader.getContents(reducedCountFileLocation);
        assertTrue(replacedWordTagsList.contains("M O"));
        assertTrue(replacedWordTagsList.contains(". O"));
        assertTrue(replacedWordTagsList.contains("_RARE_ O"));
        outputWriter.write("src/test/resources/reduced_count.out", false, replacedWordTagsList);
    }

    public void printMap(Map map){
        Iterator iter = map.entrySet().iterator();
        while(iter.hasNext()){
           Map.Entry entry = (Map.Entry) iter.next();
           LOG.info("Entry: Key: " + entry.getKey() + " Value: " + entry.getValue());
        }
    }

}