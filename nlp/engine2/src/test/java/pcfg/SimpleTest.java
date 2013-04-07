package pcfg;

import domain.Sentence.WordTag;
import domain.TagResults;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.TextNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        TagResults tagResults = new TagResults();
        Node parentNode = new Node();
        parseJSON(inputArray, parentNode,tagResults);
        ArrayNode outputArrayNode = rewriteTree(parentNode.getLeftNode(),null,null);
        String outputString = objectMapper.writeValueAsString(outputArrayNode).replaceAll(",",", ");
        LOG.info("Output String {}",outputString);
        assertEquals(tree.get(0),outputString);
    }

    @Test
    public void replaceRareTrainingWords() throws Exception {

        TagResults tagResults = new TagResults();
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

    protected void parseJSON(ArrayNode inputArray, Node parentNode, TagResults tagResults) throws IOException {

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

    protected Node parseWordAndEmission(ArrayNode inputArray, TagResults tagResults) throws IOException {
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


    public Set<String> getLowOccurrenceWords(TagResults tagResults){

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
