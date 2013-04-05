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

        //String[] node = objectMapper.readValue(tree.get(0),String[].class);
        assertNotNull(inputArray);
        List<WordTag> wordTags = new ArrayList<WordTag>();
        /*printJSON(inputArray, null, wordTags);
        assertEquals(wordTags.get(0),new WordTagWithPath("There","DET","/S/NP"));
        assertEquals(wordTags.get(1),new WordTagWithPath("is","VERB","/S/S/VP"));
        assertEquals(wordTags.get(2),new WordTagWithPath("no","DET","/S/S/VP/VP/NP"));
        assertEquals(wordTags.get(3),new WordTagWithPath("asbestos","NOUN","/S/S/VP/VP/NP"));
        assertEquals(wordTags.get(7), new WordTagWithPath("now", "ADV", "/S/S/VP/VP/VP/ADVP"));
        assertEquals(wordTags.get(8),new WordTagWithPath(".",".","/S/S"));*/
        Node parentNode = new Node();
        parseJSON(inputArray, parentNode, wordTags);

        rewriteTree(parentNode.getLeftNode());

        LOG.info("Printing json eqn {}", objectMapper.writeValueAsString(parentNode.getLeftNode()));

    }

    public void rewriteTree(Node node){

        LOG.info("{} {}",new Object[]{node.getEmission(),node.getWord()});
        //List<Node> childNodes = new ArrayList<Node>();
        if(node.getLeftNode() != null){
            rewriteTree(node.getLeftNode());
        }
        if(node.getRightNode() != null){
            rewriteTree(node.getRightNode());
        }
        /*for(Node childNode: childNodes){
            depth = depth+1;
            rewriteTree(childNode,depth);
        } */

    }

    @Test
    public void rewriteTree(){
        List<WordTagWithPath> wordPaths = new ArrayList<WordTagWithPath>();
        wordPaths.add(new WordTagWithPath("There","DET","/S/NP"));
        wordPaths.add(new WordTagWithPath("is","VERB","/S/S/VP"));
        wordPaths.add(new WordTagWithPath("no","DET","S/S/VP/VP/NP"));
        wordPaths.add(new WordTagWithPath("asbestos","NOUN","/S/S/VP/VP/NP"));

        //assertEquals("[\"S\", [\"NP\", [\"DET\", \"There\"]], [\"S\", [\"VP\", [\"VERB\", \"is\"], [\"VP\", [\"NP\", [\"DET\", \"no\"], [\"NOUN\", \"asbestos\"]], [\"VP\", [\"PP\", [\"ADP\", \"in\"], [\"NP\", [\"PRON\", \"our\"], [\"NOUN\", \"products\"]]], [\"ADVP\", [\"ADV\", \"now\"]]]]], [\".\", \".\"]]]",null);
    }

    public String produceJSON(List<WordTagWithPath> wordPaths){
        for(WordTagWithPath wordTag: wordPaths){

        }
        return null;
    }


    protected void parseJSON(ArrayNode inputArray, Node parentNode, List<WordTag> wordTags) throws IOException {

        boolean containsArrayNodes = checkContainsChildArrayNodes(inputArray);
        if(!containsArrayNodes && inputArray.size() == 2){
            Node node = parseWordAndEmission(inputArray);
            if(parentNode.getLeftNode() == null){
                parentNode.setLeftNode(node);
            } else {
                parentNode.setRightNode(node);
            }
        } else {
            Iterator<JsonNode> iter = inputArray.getElements();
            Node node = parentNode;
            List<ArrayNode> arrayNodes = new ArrayList<ArrayNode>();
            while(iter.hasNext()){
                JsonNode jsonNode = iter.next();
                if(jsonNode instanceof TextNode){
                    node = new Node(null,jsonNode.getTextValue());
                    if(parentNode.getLeftNode() == null){
                        parentNode.setLeftNode(node);
                    } else {
                        parentNode.setRightNode(node);
                    }
                } else if (jsonNode instanceof ArrayNode ){
                    arrayNodes.add((ArrayNode)jsonNode);
                }
            }
            for(ArrayNode arrayNode: arrayNodes){
                parseJSON(arrayNode,node,wordTags);
            }

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

    protected void printJSON(ArrayNode inputArray, String parentPath, List<WordTag> wordTags) throws IOException {
        Iterator<JsonNode> iter = inputArray.getElements();
        boolean containsArrayNodes = false;
        while(iter.hasNext()){
            if(iter.next() instanceof ArrayNode){
                containsArrayNodes = true;
                break;
            }
        }
        if(!containsArrayNodes && inputArray.size() == 2){
            wordTags.add(parseWordAndEmission(inputArray, parentPath));
            parentPath = removeLastParentPath(parentPath);
        } else {
            iter = inputArray.getElements();
            List<ArrayNode> arrayNodes = new ArrayList<ArrayNode>();
            while(iter.hasNext()){
                JsonNode jsonNode = iter.next();
                if(jsonNode instanceof TextNode){
                    parentPath = parentPath != null? parentPath + "/" + jsonNode.getTextValue() : "/" + jsonNode.getTextValue();

                } else if (jsonNode instanceof ArrayNode ){
                    arrayNodes.add((ArrayNode)jsonNode);

                }
            }
            for(ArrayNode arrayNode: arrayNodes){
                printJSON(arrayNode,parentPath,wordTags);
            }
            parentPath = removeLastParentPath(parentPath);
        }
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

        //LOG.info("Fringe nodes " + word + " " + tag);
        LOG.info("Fringe nodes {} ", new Object[]{wordTag});
        Node node = new Node(word,tag);
        return node;
    }

    protected WordTag parseWordAndEmission(ArrayNode inputArray,String parentPath) throws IOException {
        Iterator<JsonNode> iter = inputArray.getElements();

        JsonNode tagNode = iter.next();
        JsonNode wordNode = iter.next();

        String word = wordNode.getTextValue();
        String tag = tagNode.getTextValue();

        WordTag wordTag = new WordTagWithPath(word,tag,parentPath);
        updateCountMap(tagResults.getWordTagCountMap(),wordTag);
        updateCountMap(tagResults.getWordCountMap(),word);
        updateCountMap(tagResults.getTagCountMap(),tag);
        tagResults.getTags().add(tag);
        tagResults.getWords().add(word);
        tagResults.getWordTags().add(word + " " + tag);

        //LOG.info("Fringe nodes " + word + " " + tag);
        LOG.info("Fringe nodes {} ", new Object[]{wordTag});
        return wordTag;
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
