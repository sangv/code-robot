package reader;

import domain.TaggedSentence;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class provides a file based implementation of SentenceReader
 *
 * @author Sang Venkatraman
 *
 */
public class FileBasedSentenceReader implements SentenceReader {

    @Override
    public List<TaggedSentence> read(String location) throws IOException{
        List<TaggedSentence> sentences = new ArrayList<TaggedSentence>();
        List<String> input = getContents(location);
        List<TaggedSentence.WordTag> wordTags = initSentences();
        for(String str: input){
            if(str == null || str.length() == 0) {
                sentences.add(new TaggedSentence(completeSentences(wordTags)));
                wordTags = initSentences();
            } else {
                String[] splits = str.split(" ");
                TaggedSentence.WordTag wordTag = new TaggedSentence.WordTag(splits[0],splits[1]);
                wordTags.add(wordTag);
            }
        }
        sentences.add(new TaggedSentence(completeSentences(wordTags)));//calling it once after for the last sentence
        return sentences;
    }

    protected List<String> getContents(String fileLocation) throws IOException {
        FileInputStream fin =  new FileInputStream(fileLocation);
        BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
        List<String> strings = new ArrayList<String>();
        String thisLine;
        while ((thisLine = myInput.readLine()) != null) {
            strings.add(thisLine);
        }
        return strings;
    }

    protected List<TaggedSentence.WordTag> initSentences(){
        List<TaggedSentence.WordTag> wordTags = new LinkedList<TaggedSentence.WordTag>();
        wordTags.add(new TaggedSentence.WordTag("None","*"));
        wordTags.add(new TaggedSentence.WordTag("None","*"));
        return wordTags;
    }

    protected List<TaggedSentence.WordTag> completeSentences(List<TaggedSentence.WordTag> wordTags){
        wordTags.add(new TaggedSentence.WordTag("None","STOP"));
        return wordTags;
    }
}
