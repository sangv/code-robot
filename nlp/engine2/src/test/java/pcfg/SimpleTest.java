package pcfg;

import domain.Sentence.WordTag;
import domain.TagResults;
import org.apache.commons.lang.StringUtils;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    TagResults tagResults = new TagResults();

    @Test
    public void loadTree() throws Exception{

        List<String> tree = sentenceReader.getContents("src/test/resources/pcfg/tree.example");
        assertNotNull(tree);
        ArrayNode inputArray = (ArrayNode) objectMapper.readTree(tree.get(0));

        Node parentNode = new Node();
        parseJSON(inputArray, parentNode);
        ArrayNode outputArrayNode = rewriteTree(parentNode.getLeftNode(),null);
        String outputString = objectMapper.writeValueAsString(outputArrayNode).replaceAll(",",", ");
        LOG.info("Output String {}",outputString);
        assertEquals(tree.get(0),outputString);
    }

    public ArrayNode rewriteTree(Node node, ArrayNode outputArrayNode){

        ArrayNode childArrayNode = objectMapper.createArrayNode();
        if(node.getWord() != null){
            childArrayNode.add(node.getEmission());
            childArrayNode.add(node.getWord());
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
            rewriteTree(childNode,childArrayNode);
        }

        return outputArrayNode;
    }

    protected void parseJSON(ArrayNode inputArray, Node parentNode) throws IOException {

        boolean containsArrayNodes = checkContainsChildArrayNodes(inputArray);
        if(!containsArrayNodes && inputArray.size() == 2){
            Node node = parseWordAndEmission(inputArray);
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
                parseJSON(arrayNode,node);
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

    protected Node parseWordAndEmission(ArrayNode inputArray) throws IOException {
        Iterator<JsonNode> iter = inputArray.getElements();

        JsonNode tagNode = iter.next();
        JsonNode wordNode = iter.next();

        String word = wordNode.getTextValue();
        String tag = tagNode.getTextValue();

        WordTag wordTag = new WordTagWithPath(word,tag,null);
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


    public List<String> getLowOccurenceWords(TagResults tagResults){
        Map<WordTag,Integer> taggedWords = tagResults.getWordTagCountMap();
        Map<String,Integer> wordCountMap = tagResults.getWordCountMap();
        Integer countOfRareAndIGene = 0;
        Integer countOfRareAndO = 0;
        List<String> toBeReplacedWords = new ArrayList<String>();
        Iterator<Map.Entry<String,Integer>> iter = wordCountMap.entrySet().iterator();
        while(iter.hasNext()){
            Map.Entry<String,Integer> entry = iter.next();
            if(entry.getValue() < 5){
                for(int j=0; j< entry.getValue(); j++){
                    toBeReplacedWords.add(entry.getKey());
                }
                int incrementIGeneBy = taggedWords.containsKey(new WordTag(entry.getKey(),"I-GENE")) ? taggedWords.get(new WordTag(entry.getKey(),"I-GENE")):0;
                countOfRareAndIGene = countOfRareAndIGene+incrementIGeneBy;
                int incrementOBy = taggedWords.containsKey(new WordTag(entry.getKey(),"O")) ? taggedWords.get(new WordTag(entry.getKey(),"O")):0;
                countOfRareAndO = countOfRareAndO + incrementOBy;
            }
        }
        return toBeReplacedWords;
    }

    public List<String> replaceLessFrequentWordTags(String outputFileLocation, TagResults tagResults, boolean rareSubClasses) throws Exception {

        List<String> toBeReplacedWords = getLowOccurenceWords(tagResults);
        List<String> fixedWordsList = tagResults.getWords();
        List<String> fixedWordTagsList = tagResults.getWordTags();

        List<String> newWordTagsList = new ArrayList<String>();
        for(String wordTag: fixedWordTagsList){
            newWordTagsList.add(wordTag);
        }

        for(String toBeReplacedWord: toBeReplacedWords){
            //don't have to worry about empty lines because they wont make it here
            int index = fixedWordsList.indexOf(toBeReplacedWord);
            String rareClass = "_RARE_";

            fixedWordsList.set(index,rareClass);
            newWordTagsList.set(index,rareClass + " " + fixedWordTagsList.get(index).split(" ")[1]);
        }

        outputWriter.write(outputFileLocation, false, newWordTagsList);
        return newWordTagsList;
    }

    protected String removeLastParentPath(String parentPath){
        String[] split = parentPath.split("/");
        return StringUtils.removeEnd(parentPath,"/"+split[split.length-1]);
    }
}
