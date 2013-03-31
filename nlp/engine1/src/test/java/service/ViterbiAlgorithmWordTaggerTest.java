package service;

import domain.Sentence;
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class ViterbiAlgorithmWordTaggerTest {

        private WordTagger wordTagger = new ViterbiAlgorithmWordTagger();

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
        public void calculateQFunctions() throws IOException {

            wordTagger.init("src/test/resources/reduced_count.out");
            TagResults tagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Double> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
            assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(new Sentence.WordTag("_RARE_","O")));

            Map<String,Double> qFunction = wordTagger.calculateQFunction(tagResults);

            assertNotNull(qFunction);
            assertEquals(21,qFunction.size());
            String[] existingTags = new String[]{"O","*","*"};


            Sentence.WordTag rareWithTagO = new Sentence.WordTag("_RARE_","O");

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
        public void testViterbiAlgorithm() throws Exception {

            wordTagger.init("src/test/resources/reduced_count.out");
            TagResults tagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Double> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
            Map<String,Double> qFunction = wordTagger.calculateQFunction(tagResults);

            List<List<String>> wordsList = sentenceReader.readSentences("src/test/resources/gene.dev_one");
            String[] words = wordsList.get(0).toArray(new String[]{});
            Map<String, Double> piMap = ((ViterbiAlgorithmWordTagger)wordTagger).calculatePiMap(words, qFunction, expectationMap,tagResults,false).getPiMap();

            //printMap(piMap);
            assertEquals(1.0,piMap.get("pi(0,*,*)"));
            assertEquals(0.07885907577270845,piMap.get("pi(1,*,O)"));
            assertEquals(3.27081753905703E-5,piMap.get("pi(2,O,O)"));

            assertEquals(7.048060248543543E-7,piMap.get("pi(3,O,O)"));

        }

        @Test
        public void testViterbiAlgorithmOnTest() throws Exception {

            wordTagger.init("src/test/resources/gene.train");
            TagResults originalTagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Double> originalExpectationsMap = wordTagger.calculateExpectations(originalTagResults.getTagCountMap(), originalTagResults.getWordTagCountMap());
            wordTagger.invalidate();

            wordTagger.init("src/test/resources/reduced_count.out");
            TagResults tagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Double> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
            Map<String,Double> qFunction = wordTagger.calculateQFunction(tagResults);

            wordTagger.estimate("src/test/resources/gene.dev", "src/test/resources/gene_dev.p2.out", expectationMap, tagResults, false);
            wordTagger.estimate("src/test/resources/gene.test", "src/test/resources/gene_test.p2.out", expectationMap, tagResults, false);
            List<String> results = wordTagger.estimate("src/test/resources/gene.dev_one", "src/test/resources/gene_dev_one.out", expectationMap, originalTagResults, false);
            int index = 0;
            assertEquals("STAT5A O",results.get(index));
            assertEquals("mutations O",results.get(++index));
            assertEquals("in O",results.get(++index));
            assertEquals("the O",results.get(++index));
            assertEquals("Src I-GENE",results.get(++index));
            assertEquals("homology I-GENE",results.get(++index));
            assertEquals("2 I-GENE",results.get(++index));
            assertEquals("( I-GENE",results.get(++index));
            assertEquals("SH2 I-GENE",results.get(++index));
            assertEquals(") I-GENE",results.get(++index));
            assertEquals("and O",results.get(++index));
            assertEquals("SH3 I-GENE",results.get(++index));
            assertEquals("domains I-GENE",results.get(++index));
            assertEquals("did O",results.get(++index));
            assertEquals("not O",results.get(++index));
            assertEquals("alter O",results.get(++index));
            assertEquals("the O",results.get(++index));
            assertEquals("BTK O",results.get(++index));
            assertEquals("- O",results.get(++index));
            assertEquals("mediated O",results.get(++index));
            assertEquals("tyrosine O",results.get(++index));
            assertEquals("phosphorylation O",results.get(++index));
            assertEquals(". O",results.get(++index));
        }

        @Test
        public void testViterbiAlgorithmWithMultipleRareClassesOnTest() throws Exception {

            wordTagger.init("src/test/resources/gene.train");
            TagResults originalTagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Double> originalExpectationsMap = wordTagger.calculateExpectations(originalTagResults.getTagCountMap(), originalTagResults.getWordTagCountMap());
            wordTagger.invalidate();

            wordTagger.init("src/test/resources/reduced_count_multiple_rare.out");
            TagResults tagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Double> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
            Map<String,Double> qFunction = wordTagger.calculateQFunction(tagResults);

            wordTagger.estimate("src/test/resources/gene.dev", "src/test/resources/gene_dev.p3.out", expectationMap, tagResults, true);
            wordTagger.estimate("src/test/resources/gene.test", "src/test/resources/gene_test.p3.out", expectationMap, tagResults, true);

        }

        @Test
        @Ignore
        public void testGenerateMultipleRareClassTags() throws Exception {
            wordTagger.init("src/test/resources/gene.train");
            TagResults tagResults = wordTagger.getTagResults();
            Map<Sentence.WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();
            assertEquals(new Integer(2),taggedWords.get(new Sentence.WordTag("revascularisation","O")));
            assertEquals(399996,tagResults.getWords().size());

            List<String> reducedCountWords = wordTagger.replaceLessFrequentWordTags("src/test/resources/reduced_count_multiple_rare.out",tagResults,true);
        }

        @Test
        public void deduceRareSubclass(){
            assertEquals("_NUMERIC_",((ViterbiAlgorithmWordTagger)wordTagger).deduceRareSubclass("abc3abc"));
            assertEquals("_ALL_CAPITALS_",((ViterbiAlgorithmWordTagger)wordTagger).deduceRareSubclass("ABCABC"));
            assertEquals("_LAST_CAPITAL_",((ViterbiAlgorithmWordTagger)wordTagger).deduceRareSubclass("abcabC"));
            assertEquals("_RARE_",((ViterbiAlgorithmWordTagger)wordTagger).deduceRareSubclass("rare"));
        }

        public void printMap(Map map){
            Iterator iter = map.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry entry = (Map.Entry) iter.next();
                LOG.info("Entry: Key: " + entry.getKey() + " Value: " + entry.getValue());
            }
        }
}
