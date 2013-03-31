package pcfg;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.FileBasedSentenceReader;
import reader.SentenceReader;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertNotNull;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class SimpleTest {


    private SentenceReader sentenceReader = new FileBasedSentenceReader();

    private static final Logger LOG = LoggerFactory.getLogger(SimpleTest.class);

    @Test
    public void loadTree() throws Exception{

        List<String> tree = sentenceReader.getContents("src/test/resources/pcfg/tree.example");
        assertNotNull(tree);
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode inputArray = (ArrayNode) objectMapper.readTree(tree.get(0));
        assertNotNull(inputArray);
        printJSON(objectMapper,inputArray);
        //LOG.info(objectMapper.defaultPrettyPrintingWriter().writeValueAsString(inputArray));

    }

    protected void printJSON(ObjectMapper objectMapper, ArrayNode inputArray) throws IOException {
        Iterator<JsonNode> iter = inputArray.getElements();
        while(iter.hasNext()){
            JsonNode jsonNode = iter.next();
            if(jsonNode instanceof ArrayNode){
                printJSON(objectMapper,(ArrayNode)jsonNode);
            } else {
                LOG.info("Blah: " + objectMapper.writeValueAsString(jsonNode));
            }

        }
    }
}
