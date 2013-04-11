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
import pcfg.service.CKYEstimator;
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

    private CKYEstimator ckyEstimator = new CKYEstimator();

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
    public void testSimpleExample() throws Exception {


        Map<String,Map<String,Double>> qFunctionY1Y2GivenX = new LinkedHashMap<String,Map<String, Double>>();
        Map<String,Double> childMap =new LinkedHashMap<String,Double>();
        childMap.put("NP_AND_VP",1.0);
        qFunctionY1Y2GivenX.put("S",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("Vt_AND_NP",0.8);
        childMap.put("VP_AND_PP",0.2);
        qFunctionY1Y2GivenX.put("VP",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("DT_AND_NN",0.8);
        childMap.put("NP_AND_PP",0.2);
        qFunctionY1Y2GivenX.put("NP",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("IN_AND_NP",1.0);
        qFunctionY1Y2GivenX.put("PP",childMap);

        Map<String,Map<String,Double>> qFunctionWordGivenX = new LinkedHashMap<String,Map<String, Double>>();
        childMap =new LinkedHashMap<String,Double>();
        childMap.put("sleeps",1.0);
        qFunctionWordGivenX.put("Vi",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("saw",1.0);
        qFunctionWordGivenX.put("Vt",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("man",0.1);
        childMap.put("woman",0.1);
        childMap.put("telescope",0.3);
        childMap.put("dog",0.5);
        qFunctionWordGivenX.put("NN",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("the",1.0);
        qFunctionWordGivenX.put("DT",childMap);

        childMap =new LinkedHashMap<String,Double>();
        childMap.put("with",0.6);
        childMap.put("in",0.4);
        qFunctionWordGivenX.put("IN",childMap);


        String sentence = "the dog saw the man with the telescope";

        DynamicProgrammingResults dynamicProgrammingResults = ckyEstimator.calculatePiMap(sentence.split(" "),new LinkedHashSet<String>(Arrays.asList(new String[]{"S","VP","NP","PP","Vt","DT","IN","Vi","NN"})),qFunctionY1Y2GivenX,qFunctionWordGivenX,null);
        Map<String, Double> piMap =  dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap =  dynamicProgrammingResults.getMaxBackPointerMap();

        assertEquals(17,piMap.size());
        assertEquals(9,maxBackPointerMap.size());
        assertEquals(7.372800000000003E-4,piMap.get("1,8,S"));
    }

    @Test
    public void loadReplacedTrainingwords() throws Exception {


        List<String> trainingData = sentenceReader.getContents("src/test/resources/pcfg/parse_train.dat");
        List<Node> sentences = new ArrayList<Node>();
        CKYTagResults tagResults = trainData(trainingData,sentences);

        List<String> counts = sentenceReader.getContents("src/test/resources/pcfg/parse_train.counts.out");
        Set<String> tags = new LinkedHashSet<String>();

        for(String result: counts){
            String[] split = result.split(" ");
            if("NONTERMINAL".equals(split[1])){
                tagResults.getNonTerminalCountMap().put(split[2], new Integer(split[0]));
                tags.add(split[2]);
            } else if("UNARYRULE".equals(split[1])){
                tagResults.getUnaryRuleCountMap().put(new WordTag(split[3], split[2]), new Integer(split[0]));
            } else if("BINARYRULE".equals(split[1])){
                tagResults.getBinaryRuleCountMap().put(new NGramTag(3, split[2], split[3], split[4]), new Integer(split[0]));
            }
        }

        //Calculate QFunctions
        Map<String,Map<String,Double>> qFunctionY1Y2GivenX = new LinkedHashMap<String,Map<String, Double>>();
        ckyEstimator.calculateY1Y2GivenX(tagResults.getNonTerminalCountMap(),tagResults.getBinaryRuleCountMap(),qFunctionY1Y2GivenX);

        Map<String,Map<String,Double>> qFunctionWordGivenX = new LinkedHashMap<String,Map<String, Double>>();
        ckyEstimator.calculateWordGivenX(tagResults.getNonTerminalCountMap(),tagResults.getUnaryRuleCountMap(),qFunctionWordGivenX);

        List<String> testData = sentenceReader.getContents("src/test/resources/pcfg/parse_dev.dat");
        String sentence = "What are geckos ?";
        //for(String sentence: testData){
        DynamicProgrammingResults dynamicProgrammingResults = ckyEstimator.calculatePiMap(sentence.split(" "),tags,qFunctionY1Y2GivenX,qFunctionWordGivenX,tagResults);
        Map<String, Double> piMap =  dynamicProgrammingResults.getPiMap();
        Map<String, String> maxBackPointerMap =  dynamicProgrammingResults.getMaxBackPointerMap();
        Node parentNode = ckyEstimator.calculateFinalString(dynamicProgrammingResults, sentence.split(" "),tags);
        System.out.println("Printing parent node: " + parentNode);
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

}