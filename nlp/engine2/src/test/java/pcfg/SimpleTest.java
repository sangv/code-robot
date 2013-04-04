package pcfg;

import domain.Sentence.*;
import domain.TagResults;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
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
        printJSON(inputArray, null);
        //LOG.info(objectMapper.defaultPrettyPrintingWriter().writeValueAsString(inputArray));

    }

    protected void printJSON(ArrayNode inputArray, String parentPath) throws IOException {
        Iterator<JsonNode> iter = inputArray.getElements();
        boolean containsArrayNodes = false;
        while(iter.hasNext()){
            if(iter.next() instanceof ArrayNode){
                containsArrayNodes = true;
                break;
            }
        }
        if(!containsArrayNodes && inputArray.size() == 2){
            parseWordAndEmission(inputArray,parentPath);
            String[] split = parentPath.split("/");
            parentPath = StringUtils.removeEnd(parentPath,split[split.length-1]);
        } else {
            iter = inputArray.getElements();
            while(iter.hasNext()){
                JsonNode jsonNode = iter.next();
                if(jsonNode instanceof TextNode){
                    parentPath = parentPath != null? parentPath + "/" + jsonNode.getTextValue() : "/" + jsonNode.getTextValue();
                } else if (jsonNode instanceof ArrayNode ){
                    printJSON((ArrayNode)jsonNode,parentPath);
                }
            }
        }
        /*while(iter.hasNext()){
            JsonNode jsonNode = iter.next();
            if(jsonNode instanceof TextNode){
                parentPath = parentPath != null? parentPath + "/" + jsonNode.getTextValue() : "/" + jsonNode.getTextValue();
            }
            else if(jsonNode instanceof ArrayNode){
                boolean containsArrayNodes = false;
                Iterator<JsonNode> iterator = inputArray.getElements();
                while(iterator.hasNext()){
                    if(iterator.next() instanceof ArrayNode){
                        containsArrayNodes = true;
                        break;
                    }
                }
                if(!containsArrayNodes && ((ArrayNode)jsonNode).size() == 2){
                    parseWordAndEmission((ArrayNode)jsonNode,parentPath);
                } else {
                    printJSON((ArrayNode)jsonNode,parentPath);
                }
            }
        } */
        /*List<ArrayNode> arrayNodes = new ArrayList<ArrayNode>();
        while(iter.hasNext()){
            JsonNode jsonNode = iter.next();
            if(jsonNode instanceof TextNode){
                parentPath = parentPath != null? parentPath + "/" + jsonNode.getTextValue() : "/" + jsonNode.getTextValue();
                LOG.info("parentPath: {}",parentPath);
            } else if (jsonNode instanceof ArrayNode) {
                arrayNodes.add((ArrayNode)jsonNode);
                //printJSON((ArrayNode)jsonNode,node);
            }
        }
        for(ArrayNode arrayNode: arrayNodes){
            printJSON(arrayNode,parentPath);
        } */
    }

    protected void parseWordAndEmission(ArrayNode inputArray,String parentPath) throws IOException {
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
        LOG.info("Fringe nodes " + wordTag);
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
}
