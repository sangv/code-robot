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


        List<String> trainingData = sentenceReader.getContents("src/test/resources/pcfg/parse_train.dat");
        List<Node> sentences = new ArrayList<Node>();
        CKYTagResults tagResults = trainData(trainingData,sentences);


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

        List<String> counts = sentenceReader.getContents("src/test/resources/pcfg/parse_train.counts.out");
        CKYTagResults tagResults = new CKYTagResults();
        Set<String> tags = new LinkedHashSet<String>();

        for(String result: counts){
            String[] split = result.split(" ");
            if("NONTERMINAL".equals(split[1])){
                tagResults.getNonTerminalCountMap().put(split[2], new Integer(split[0]));
                tags.add(split[2]);
            } else if("UNARYRULE".equals(split[1])){
                tagResults.getUnaryRuleCountMap().put(new WordTag(split[3], split[2]), new Integer(split[0]));
                tags.add(split[2]);
            } else if("BINARYRULE".equals(split[1])){
                tagResults.getBinaryRuleCountMap().put(new NGramTag(3, split[2], split[3], split[4]), new Integer(split[0]));
                tags.addAll(Arrays.asList(new String[]{split[2], split[3], split[4]}));
            }
        }

        //Calculate QFunctions
        Map<String,Double> qFunctionY1Y2GivenX = new LinkedHashMap<String,Double>();
        calculateY1Y2GivenX(tagResults.getNonTerminalCountMap(),tagResults.getBinaryRuleCountMap(),qFunctionY1Y2GivenX);

        Map<String,Double> qFunctionWordGivenX = new LinkedHashMap<String,Double>();
        calculateWordGivenX(tagResults.getNonTerminalCountMap(),tagResults.getUnaryRuleCountMap(),qFunctionWordGivenX);

        List<String> testData = sentenceReader.getContents("src/test/resources/pcfg/parse_dev.dat");
        String sentence = "What was the monetary value of the Nobel Peace Prize in 1989 ?";
        //for(String sentence: testData){
            DynamicProgrammingResults dynamicProgrammingResults = calculatePiMap(sentence.split(" "),tags,qFunctionY1Y2GivenX,qFunctionWordGivenX,tagResults,false);
            Map<String, Double> piMap =  dynamicProgrammingResults.getPiMap();
            Map<String, String> maxBackPointerMap =  dynamicProgrammingResults.getMaxBackPointerMap();
        //}

    }

    private CKYTagResults trainData(List<String> trainingData, List<Node> sentences) throws Exception {

        CKYTagResults tagResults = new CKYTagResults();
        for(String train: trainingData){
            ArrayNode inputArray = (ArrayNode) objectMapper.readTree(train);
            Node parentNode = new Node();
            parseJSON(inputArray, parentNode,tagResults);
            sentences.add(parentNode.getLeftNode());
        }
        return tagResults;
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


    protected void calculateY1Y2GivenX(Map<String,Integer> nonTerminalTagCounts, Map<NGramTag,Integer> binaryRuleCounts, Map<String,Double> qFunctionMap){

        for (Map.Entry<NGramTag,Integer> entry: binaryRuleCounts.entrySet()){
            NGramTag binaryRuleTag = entry.getKey();
            int numerator = entry.getValue();
            int denominator = nonTerminalTagCounts.containsKey(binaryRuleTag.getTag()) ? nonTerminalTagCounts.get(binaryRuleTag.getTag()) : 0;
            double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
            if(qFunction > 0) {
                qFunctionMap.put(binaryRuleTag.getTag() + "Implies" + binaryRuleTag.getOthers()[0] + "And" + binaryRuleTag.getOthers()[1],qFunction);
            }
        }
    }

    protected void calculateWordGivenX(Map<String,Integer> nonTerminalTagCounts, Map<WordTag,Integer> unaryRuleCounts, Map<String,Double> qFunctionMap){

        for (Map.Entry<WordTag,Integer> entry: unaryRuleCounts.entrySet()){
            WordTag wordTag = entry.getKey();
            int numerator = entry.getValue();
            int denominator = nonTerminalTagCounts.containsKey(wordTag.getTag()) ? nonTerminalTagCounts.get(wordTag.getTag()) : 0;
            double qFunction = denominator > 0? (double)numerator/(double)denominator : 0.0F;
            if(qFunction > 0) {
                qFunctionMap.put(wordTag.getTag() + "Implies" + wordTag.getWord(),qFunction);
            }
        }
    }

    protected DynamicProgrammingResults calculatePiMap(String[] words, Set<String> tags, Map<String, Double> qFunctionY1Y2GivenX, Map<String,Double> qFunctionWordGivenX, CKYTagResults tagResults, boolean useRareSubclasses){

        DynamicProgrammingResults dynamicProgrammingResults = new DynamicProgrammingResults();
        Map<String, Double> piMap = dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap = dynamicProgrammingResults.getMaxBackPointerMap();
        Set<Map.Entry<NGramTag,Integer>> binaryRuleMap = tagResults.getBinaryRuleCountMap().entrySet();


            for(int i=0; i < words.length; i++){
                for(String tag: tags){
                    String X = tag;
                    double qValue = qFunctionWordGivenX.containsKey(X+"Implies"+words[i]) ? qFunctionWordGivenX.get(X+"Implies"+words[i]) : 0.0;
                    piMap.put("pi("+i+","+i+","+X+")",qValue);
                /*for(int j=i+1; j<words.length; j++){
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
                        /*Double pivalue1 = piMap.containsKey("pi("+ i +","+s+"," + Y+ ")") ? piMap.get("pi("+ i +","+s+"," + Y+ ")") : 0.0F;
                        Double pivalue2 = piMap.containsKey("pi("+ new Integer(s+1) +","+j+"," + Z+ ")") ? piMap.get("pi("+ new Integer(s+1) +","+j+"," + Z+ ")") : 0.0F;
                        Double pivalue =  qFunctionY1Y2GivenX.get(X + "Implies" + Y + "And" + Z)*pivalue1*pivalue2;

                        if(pivalue > currentMax){
                            currentMax = pivalue;
                            piMap.put(key,pivalue);
                            maxBackPointerMap.put(i+","+j+","+X,s+"_"+Y+"_"+Z);
                        }
                    }
                }*/
            }
        }
        return dynamicProgrammingResults;
    }



}
