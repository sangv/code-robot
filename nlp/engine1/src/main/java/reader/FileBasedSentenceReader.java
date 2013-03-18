package reader;

import domain.WordTag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sang
 * Date: 3/18/13
 * Time: 3:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileBasedSentenceReader implements SentenceReader {

    @Override
    public List<List<WordTag>> read(String location) throws IOException{
        List<List<WordTag>> sentences = new ArrayList<List<WordTag>>();
        List<String> input = getContents(location);
        List<WordTag> wordTags = initSentences();
        for(String str: input){
            if(str == null || str.length() == 0) {
                sentences.add(completeSentences(wordTags));
                wordTags = initSentences();
            } else {
                String[] splits = str.split(" ");
                WordTag wordTag = new WordTag(splits[0],splits[1]);
                wordTags.add(wordTag);
            }
        }
        sentences.add(completeSentences(wordTags));//calling it once after for the last sentence
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

    protected List<WordTag> initSentences(){
        List<WordTag> wordTags = new LinkedList<WordTag>();
        wordTags.add(new WordTag("None","*"));
        wordTags.add(new WordTag("None","*"));
        return wordTags;
    }

    protected List<WordTag> completeSentences(List<WordTag> wordTags){
        wordTags.add(new WordTag("None","STOP"));
        return wordTags;
    }
}
