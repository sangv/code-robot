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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class WordTaggerTest {

    private WordTagger wordTagger = new NGramWordTagger();

    private OutputWriter outputWriter = new FileOutputWriter();

    private SentenceReader sentenceReader = new FileBasedSentenceReader();

    private static final Logger LOG = LoggerFactory.getLogger(WordTaggerTest.class);

    @Before
    public void setUp() throws Exception {
        //wordTagger = new NGramWordTagger();
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
        Map<WordTag,Float> originalExpectationsMap = wordTagger.calculateExpectations(originalTagResults.getTagCountMap(), originalTagResults.getWordTagCountMap());
        wordTagger.invalidate();

        wordTagger.init("src/test/resources/reduced_count.out");
        TagResults tagResults = wordTagger.getTagResults();
        assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(new WordTag("_RARE_","O")));
        Map<Sentence.WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();

        Map<WordTag,Float> expectationsMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(), taggedWords);
        //List<String> estimatedWordTags = wordTagger.estimate("src/test/resources/gene.dev","src/test/resources/gene_dev.p1.out",taggedWords,expectationsMap);
        //assertTrue(estimatedWordTags.contains("BACKGROUND O"));
        List<String> estimatedWordTags = wordTagger.estimate("src/test/resources/gene.dev","src/test/resources/gene_dev.p1.out",taggedWords,expectationsMap);
        List<String> estimatedTestWordTags = wordTagger.estimate("src/test/resources/gene.test","src/test/resources/gene_test.p1.out",taggedWords,expectationsMap);
    }

    @Test
    public void calculateQFunctions() throws IOException {

        wordTagger.init("src/test/resources/reduced_count.out");
        TagResults tagResults = wordTagger.getTagResults();
        Map<WordTag,Float> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
        assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(new WordTag("_RARE_","O")));

        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
        Map<String,Float> qFunction = wordTagger.calculateQFunction(tagResults);
        printMap(qFunction);
        assertNotNull(qFunction);
        assertEquals(21,qFunction.size());
        String[] existingTags = new String[]{"O","*","*"};


        WordTag rareWithTagO = new WordTag("_RARE_","O");

        float expectationOfRAREGivenO =  (float)tagResults.getWordTagCountMap().get(rareWithTagO)/(float)tagResults.getTagCountMap().get("O");
        assertEquals(0.9456404F, qFunction.get(existingTags[0] + "Given" + existingTags[1] + "And" + existingTags[2]));

        assertEquals(new Integer(28781),tagResults.getWordTagCountMap().get(rareWithTagO));
        assertEquals(new Integer(345128),tagResults.getTagCountMap().get("O"));

        assertEquals(0.08339225F,expectationMap.get(rareWithTagO));

        Map<String, Float> piMap = new LinkedHashMap<String,Float>();
        piMap.put("pi(0,*,*)",1.0F);
        assertEquals(0.078859076F,piMap.get("pi(0,*,*)")*qFunction.get(existingTags[0] + "Given" + existingTags[1] + "And" + existingTags[2])*expectationOfRAREGivenO);
    }

    @Test
    public void testViterbiAlgorithm() throws Exception {

        wordTagger.init("src/test/resources/reduced_count.out");
        TagResults tagResults = wordTagger.getTagResults();
        Map<WordTag,Float> expectationMap = wordTagger.calculateExpectations(tagResults.getTagCountMap(),tagResults.getWordTagCountMap());
        Map<String,Float> qFunction = wordTagger.calculateQFunction(tagResults);

        Map<String, Float> piMap = new LinkedHashMap<String,Float>();
        piMap.put("pi(0,*,*)",1.0F);

        List<String> sentence = sentenceReader.getContents("src/test/resources/gene.dev_one");
        assertEquals(23,sentence.size());
        String[] words = sentence.subList(0,3).toArray(new String[]{});

        /*pi(2,I-GENE,O) = 1.98251948938e-06, argmax = *

        prob = 8.72732885782e-07, pi(1,*,O) = 0.0788647918553, q(I-GENE|*,O) = 0.0454510615467, e(mutations|I-GENE) = 0.000243474873393

        pi(2,O,I-GENE) = 8.72732885782e-07, argmax = *

        prob = 3.27105462354e-05, pi(1,*,O) = 0.0788647918553, q(O|*,O) = 0.954319000537, e(mutations|O) = 0.000434621357873

        pi(2,O,O) = 3.27105462354e-05, argmax = *

        prob = 5.50290682609e-10, pi(2,I-GENE,I-GENE) = 1.69592746501e-06, q(I-GENE|I-GENE,I-GENE) = 0.605770411295, e(in|I-GENE) = 0.000535644721465

        prob = 2.70133799337e-10, pi(2,O,I-GENE) = 8.72732885782e-07, q(I-GENE|O,I-GENE) = 0.577857502518, e(in|I-GENE) = 0.000535644721465
        */
        for(int k=1; k<=words.length; k++){
               if(k == 1){
                   String[][] tagsArray = new String[][]{{"*","*","O"}, {"*","*","I-GENE"}};
                   for(String[] tagArray: tagsArray){
                        String key = "pi("+k+","+tagArray[1]+","+tagArray[2]+")";
                        WordTag wordTag = new WordTag(words[k-1],tagArray[2]);
                        float expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : expectationMap.get(new WordTag("_RARE_",tagArray[2]));
                        piMap.put(key,piMap.get("pi(0,*,*)")*qFunction.get(tagArray[2] + "Given" + tagArray[0] + "And" + tagArray[1])*expectation);
                   }
                } else if(k==2) {
                    String[] tags = {"O","I-GENE"};
                    for(int u=0; u < tags.length; u++){
                        for(int v=0; v<tags.length; v++){
                            String key = "pi("+k+","+tags[u]+","+tags[v]+")";
                            WordTag wordTag = new WordTag(words[k-1],tags[v]);
                            float expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : expectationMap.get(new WordTag("_RARE_",tags[v]));
                            piMap.put(key,piMap.get("pi(1,*,"+tags[u]+")")*qFunction.get(tags[v] + "Given" + "*" + "And" + tags[u])*expectation);
                        }
                    }
            } else {
                   String[] tags = {"O","I-GENE"};
                   for(int u=0; u < tags.length; u++){
                       for(int v=0; v<tags.length; v++){
                           String key = "pi("+k+","+tags[u]+","+tags[v]+")";
                           WordTag wordTag = new WordTag(words[k-1],tags[v]);
                           float expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : expectationMap.get(new WordTag("_RARE_",tags[v]));
                           piMap.put(key,piMap.get("pi("+ new Integer(k-1) +","+tags[u]+"," + tags[v]+ ")")*qFunction.get(tags[u] + "Given" + tags[u] + "And" + tags[v])*expectation);
                       }
                   }
               }
            /*

pi(3,I-GENE,I-GENE) = 5.50290682609e-10, argmax = I-GENE

prob = 1.55631734149e-08, pi(2,I-GENE,I-GENE) = 1.69592746501e-06, q(O|I-GENE,I-GENE) = 0.393779414774, e(in|O) = 0.0233043972092

prob = 8.58447090439e-09, pi(2,O,I-GENE) = 8.72732885782e-07, q(O|O,I-GENE) = 0.422079556898, e(in|O) = 0.0233043972092

pi(3,I-GENE,O) = 1.55631734149e-08, argmax = I-GENE

prob = 2.23001925771e-10, pi(2,I-GENE,O) = 1.98251948938e-06, q(I-GENE|I-GENE,O) = 0.20999759384, e(in|I-GENE) = 0.000535644721465

prob = 6.55622210846e-10, pi(2,O,O) = 3.27105462354e-05, q(I-GENE|O,O) = 0.0374187290185, e(in|I-GENE) = 0.000535644721465

pi(3,O,I-GENE) = 6.55622210846e-10, argmax = O

prob = 3.14605445825e-08, pi(2,I-GENE,O) = 1.98251948938e-06, q(O|I-GENE,O) = 0.680943214629, e(in|O) = 0.0233043972092

prob = 7.04857112563e-07, pi(2,O,O) = 3.27105462354e-05, q(O|O,O) = 0.924645831286, e(in|O) = 0.0233043972092

pi(3,O,O) = 7.04857112563e-07, argmax = O

Tag sequence: ['O', 'O', 'O', 'STOP'], prob = 2.67390644875e-08

[('STAT5A', 'O'), ('mutations', 'O'), ('in', 'O'), (None, 'STOP')]
             */
        }

        printMap(piMap);
        assertEquals(1.0F,piMap.get("pi(0,*,*)"));
        assertEquals(0.078859076F,piMap.get("pi(1,*,O)"));
        assertEquals(0.011541574F,piMap.get("pi(1,*,I-GENE)"));
        assertEquals(3.2708176E-5F,piMap.get("pi(2,O,O)"));
        assertEquals(8.7266966E-7F,piMap.get("pi(2,O,I-GENE)"));
        assertEquals(1.9823758E-6F,piMap.get("pi(2,I-GENE,O)"));
        assertEquals(1.6958046E-6F,piMap.get("pi(2,I-GENE,I-GENE)"));


        assertEquals(5.502508E-10F,piMap.get("pi(3,I-GENE,I-GENE)"));
        assertEquals(7.0480604E-7F,piMap.get("pi(3,O,O)"));
        assertEquals(6.5562221E-10F,piMap.get("pi(3,O,I-GENE)"));
        assertEquals(1.5563173E-08F,piMap.get("pi(3,I-GENE,O)"));

    }

    @Ignore
    @Test
    public void testReplaceLessFrequentWordTags() throws Exception {
        wordTagger.init("src/test/resources/gene.train");
        TagResults tagResults = wordTagger.getTagResults();
        Map<WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();
        assertEquals(new Integer(2),taggedWords.get(new WordTag("revascularisation","O")));
        assertEquals(399996,tagResults.getWords().size());

        List<String> reducedCountWords = wordTagger.replaceLessFrequentWordTags("src/test/resources/reduced_count.out",tagResults);

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