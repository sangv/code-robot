package pcfg;

import domain.DynamicProgrammingResults;
import domain.NGramTag;
import domain.Sentence.WordTag;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.TextNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pcfg.domain.CKYTagResults;
import reader.FileBasedSentenceReader;
import reader.SentenceReader;
import writer.FileOutputWriter;
import writer.OutputWriter;

import java.io.IOException;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class SimpleTest {


    private SentenceReader sentenceReader = new FileBasedSentenceReader();

    private OutputWriter outputWriter = new FileOutputWriter();


    private static final Logger LOG = LoggerFactory.getLogger(SimpleTest.class);

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void loadTree() throws Exception{

        List<String> tree = sentenceReader.getContents("src/test/resources/pcfg/tree.example");
        assertNotNull(tree);
        ArrayNode inputArray = (ArrayNode) objectMapper.readTree(tree.get(0));

        CKYTagResults tagResults = new CKYTagResults();
        Node parentNode = new Node();
        parseJSON(inputArray, parentNode,tagResults);
        ArrayNode outputArrayNode = rewriteTree(parentNode.getLeftNode(),null,null);
        String outputString = objectMapper.writeValueAsString(outputArrayNode).replaceAll(",",", ");
        LOG.info("Output String {}",outputString);
        assertEquals(tree.get(0),outputString);
    }

    @Test
    public void replaceRareTrainingWords() throws Exception {

        CKYTagResults tagResults = new CKYTagResults();
        List<String> trainingData = sentenceReader.getContents("src/test/resources/pcfg/parse_train.dat");
        List<Node> sentences = new ArrayList<Node>();

        for(String train: trainingData){
            ArrayNode inputArray = (ArrayNode) objectMapper.readTree(train);
            Node parentNode = new Node();
            parseJSON(inputArray, parentNode,tagResults);
            sentences.add(parentNode.getLeftNode());
        }

        Set<String> rareWords = getLowOccurrenceWords(tagResults);
        List<String> newWordTagsList = new ArrayList<String>();

        for(Node node:sentences){
            ArrayNode outputArrayNode = rewriteTree(node,null,rareWords);
            String outputString = objectMapper.writeValueAsString(outputArrayNode).replaceAll(",",", ");
            newWordTagsList.add(outputString);
        }

        outputWriter.write("src/test/resources/pcfg/parse_train_reduced.dat", false, newWordTagsList);
    }

    @Test
    public void loadReplacedTrainingwords() throws Exception {

        CKYTagResults tagResults = new CKYTagResults();
        List<String> results = sentenceReader.getContents("src/test/resources/pcfg/parse_train.counts.out");
        for(String result: results){
            String[] split = result.split(" ");
            if("NONTERMINAL".equals(split[1])){
                tagResults.getNonTerminalCountMap().put(split[2], new Integer(split[0]));
            } else if("UNARYRULE".equals(split[1])){
                tagResults.getUnaryRuleCountMap().put(new WordTag(split[1], split[2]), new Integer(split[0]));
            } else if("BINARYRULE".equals(split[1])){
                tagResults.getBinaryRuleCountMap().put(new NGramTag(3, split[1], split[2], split[3]), new Integer(split[0]));
            }
        }

        //Calculate QFunctions
        Map<String,Double> qFunctionY1Y2GivenX = new LinkedHashMap<String,Double>();
        calculateY1Y2GivenX(tagResults.getTags().toArray(new String[]{}),tagResults.getNonTerminalCountMap(),tagResults.getBinaryRuleCountMap(),qFunctionY1Y2GivenX);

        Map<String,Double> qFunctionWordGivenX = new LinkedHashMap<String,Double>();
        calculateWordGivenX(tagResults.getTags().toArray(new String[]{}),tagResults.getNonTerminalCountMap(),tagResults.getWords(),tagResults.getWordTagCountMap(),qFunctionWordGivenX);


    }

    public ArrayNode rewriteTree(Node node, ArrayNode outputArrayNode, Set<String> rareWords){

        ArrayNode childArrayNode = objectMapper.createArrayNode();
        if(node.getWord() != null){
            childArrayNode.add(node.getEmission());
            if(rareWords != null && rareWords.contains(node.getWord())){
            childArrayNode.add("_RARE_");
            } else {
                childArrayNode.add(node.getWord());
            }
        }  else {
            childArrayNode.add(node.getEmission());
        }
        if(outputArrayNode == null){
            outputArrayNode = childArrayNode;
        } else {
            outputArrayNode.add(childArrayNode);
        }

        List<Node> childNodes = new ArrayList<Node>();
        if(node.getLeftNode() != null){
            childNodes.add(node.getLeftNode());
        }
        if(node.getRightNode() != null){
            childNodes.add(node.getRightNode());
        }
        for(Node childNode: childNodes){
            rewriteTree(childNode,childArrayNode,rareWords);
        }

        return outputArrayNode;
    }

    protected void parseJSON(ArrayNode inputArray, Node parentNode, CKYTagResults tagResults) throws IOException {

        boolean containsArrayNodes = checkContainsChildArrayNodes(inputArray);
        if(!containsArrayNodes && inputArray.size() == 2){
            Node node = parseWordAndEmission(inputArray,tagResults);
            attachNode(node, parentNode);
        } else {
            Iterator<JsonNode> iter = inputArray.getElements();
            Node node = parentNode;
            List<ArrayNode> arrayNodes = new ArrayList<ArrayNode>();
            while(iter.hasNext()){
                JsonNode jsonNode = iter.next();
                if(jsonNode instanceof TextNode){
                    node = new Node(null,jsonNode.getTextValue());
                    attachNode(node, parentNode);
                } else if (jsonNode instanceof ArrayNode ){
                    arrayNodes.add((ArrayNode)jsonNode);
                }
            }
            for(ArrayNode arrayNode: arrayNodes){
                parseJSON(arrayNode,node,tagResults);
            }

        }
    }

    private void attachNode(Node node, Node parentNode) {
        if(parentNode.getLeftNode() == null){
            parentNode.setLeftNode(node);
        } else {
            parentNode.setRightNode(node);
        }
    }

    private boolean checkContainsChildArrayNodes(ArrayNode inputArray) {
        boolean containsArrayNodes = false;
        Iterator<JsonNode> iter = inputArray.getElements();
        while(iter.hasNext()){
            if(iter.next() instanceof ArrayNode){
                containsArrayNodes = true;
                break;
            }
        }
        return containsArrayNodes;
    }

    protected Node parseWordAndEmission(ArrayNode inputArray, CKYTagResults tagResults) throws IOException {
        Iterator<JsonNode> iter = inputArray.getElements();

        JsonNode tagNode = iter.next();
        JsonNode wordNode = iter.next();

        String word = wordNode.getTextValue();
        String tag = tagNode.getTextValue();

        WordTag wordTag = new WordTag(word,tag);
        updateCountMap(tagResults.getWordTagCountMap(),wordTag);
        updateCountMap(tagResults.getWordCountMap(),word);
        updateCountMap(tagResults.getTagCountMap(),tag);
        tagResults.getTags().add(tag);
        tagResults.getWords().add(word);
        tagResults.getWordTags().add(word + " " + tag);

        Node node = new Node(word,tag);
        return node;
    }

    protected synchronized void updateCountMap(Map map, Object key){
        int count = (Integer) (map.containsKey(key) ? map.get(key) : 0);
        map.put(key, ++count);
    }


    public Set<String> getLowOccurrenceWords(CKYTagResults tagResults){

        Map<String,Integer> wordCountMap = tagResults.getWordCountMap();

        Set<String> toBeReplacedWords = new LinkedHashSet<String>();
        Iterator<Map.Entry<String,Integer>> iter = wordCountMap.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry<String,Integer> entry = iter.next();
            if(entry.getValue() < 5){
                for(int j=0; j< entry.getValue(); j++){
                    toBeReplacedWords.add(entry.getKey());
                }
            }
        }
        return toBeReplacedWords;
    }

    protected void calculateXGivenY1Y2Counts(Node node,Map<String,Double> qFunctionMap){
        String emission = node.getEmission();
        if(node.getWord() == null){//implying that this is a non-terminal
           String leftNodeEmission = node.getLeftNode() != null? node.getLeftNode().getEmission():null;
           String rightNodeEmission = node.getRightNode() != null? node.getRightNode().getEmission():null;
            if(leftNodeEmission != null && rightNodeEmission != null){

            }
        }

    }


    protected void calculateY1Y2GivenX(String[] keyTags,Map<String,Integer> nonTerminalTagCounts, Map<NGramTag,Integer> binaryRuleCounts, Map<String,Double> qFunctionMap){

        for(int i=0; i< keyTags.length; i++){
            String tag = keyTags[i];
            for(int j=0; j< keyTags.length; j++){
                for(int k=0; k< keyTags.length; k++){
                NGramTag binaryRuleTag = new NGramTag(3,keyTags[i],keyTags[j],keyTags[k]);

                int numerator = binaryRuleCounts.containsKey(binaryRuleTag) ? binaryRuleCounts.get(binaryRuleTag) : 0;
                    int denominator = nonTerminalTagCounts.containsKey(tag) ? nonTerminalTagCounts.get(tag) : 0;
                double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
                if(qFunction > 0) {
                    qFunctionMap.put(keyTags[i] + "Given" + keyTags[j] + "And" + keyTags[k],qFunction);
                }
                }
            }
        }
    }

    protected void calculateWordGivenX(String[] keyTags,Map<String,Integer> nonTerminalTagCounts, List<String> words, Map<WordTag,Integer> unaryRuleCounts, Map<String,Double> qFunctionMap){

        for(int i=0; i< keyTags.length; i++){
            String tag = keyTags[i];
            for(String word:words){

                    WordTag wordTag = new WordTag(word,tag);

                    int numerator = unaryRuleCounts.containsKey(wordTag) ? unaryRuleCounts.get(wordTag) : 0;
                    int denominator = nonTerminalTagCounts.containsKey(tag) ? nonTerminalTagCounts.get(tag) : 0;
                    double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
                    if(qFunction > 0) {
                        qFunctionMap.put(word + "Given" + keyTags[i],qFunction);
                    }

            }
        }
    }

    protected DynamicProgrammingResults calculatePiMap(String[] words, Map<String, Double> qFunctionY1Y2GivenX, Map<String,Double> qFunctionWordGivenX, Map<WordTag, Double> expectationMap, CKYTagResults tagResults, boolean useRareSubclasses){

        DynamicProgrammingResults dynamicProgrammingResults = new DynamicProgrammingResults();
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();
        Set<Map.Entry<NGramTag,Integer>> binaryRuleMap = tagResults.getBinaryRuleCountMap().entrySet();

        for(Map.Entry<NGramTag,Integer> ngramEntry: binaryRuleMap){
            String X = ngramEntry.getKey().getTag();
            String Y = ngramEntry.getKey().getOthers()[0];
            String Z = ngramEntry.getKey().getOthers()[1];
            for(int i=0; i < words.length; i++){
                double qValue = qFunctionWordGivenX.containsKey(words[i]+"Given"+X) ? qFunctionWordGivenX.get(words[i] + "Given" + X) : 0.0;
                piMap.put("pi("+i+","+i+","+X+")",qValue);
                for(int j=i+1; j<words.length; j++){
                    double currentMax = 0.0F;
                    for(int s=0; s<words.length; s++){
                        String key = "pi("+i+","+j+","+X+")";

                        /*double expectation = 0.0F;
                        if(tagResults.getWordCountMap().containsKey(words[k-1]) && tagResults.getWordCountMap().get(words[k-1]) >= 5){
                            expectation = expectationMap.containsKey(wordTag) ? expectationMap.get(wordTag) : 0.0F;
                        } else {
                            String rareClass = "_RARE_";
                            expectation = expectationMap.get(new WordTag(rareClass,tags[v]));
                        }*/
                        Double pivalue1 = piMap.containsKey("pi("+ i +","+s+"," + Y+ ")") ? piMap.get("pi("+ i +","+s+"," + Y+ ")") : 0.0F;
                        Double pivalue2 = piMap.containsKey("pi("+ new Integer(s+1) +","+j+"," + Z+ ")") ? piMap.get("pi("+ new Integer(s+1) +","+j+"," + Z+ ")") : 0.0F;
                        Double pivalue =  qFunctionY1Y2GivenX.get(X + "Given" + Y + "And" + Z)*pivalue1*pivalue2;

                        if(pivalue > currentMax){ //TODO revisit
                            currentMax = pivalue;
                            piMap.put(key,pivalue);
                            maxBackPointerMap.put(i+","+j+","+X,s+"_"+Y+"_"+Z);
                        }
                    }
                }
            }
        }
        return dynamicProgrammingResults;
    }



}
