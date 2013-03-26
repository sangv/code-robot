package reader;

import domain.Sentence;
import domain.Sentence.WordTag;

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
    public List<Sentence> read(String location) throws IOException{
        List<Sentence> sentences = new ArrayList<Sentence>();
        List<String> input = getContents(location);
        List<WordTag> wordTags = initSentences();
        for(String str: input){
            if(str == null || str.length() == 0) {
                sentences.add(new Sentence(completeSentences(wordTags)));
                wordTags = initSentences();
            } else {
                String[] splits = str.split(" ");
                WordTag wordTag = new WordTag(splits[0],splits[1]);
                wordTags.add(wordTag);
            }
        }
        sentences.add(new Sentence(completeSentences(wordTags)));//calling it once after for the last sentence
        return sentences;
    }

    @Override
    public List<List<String>> readSentences(String location) throws IOException{
        List<List<String>> sentences = new ArrayList<List<String>>();
        List<String> input = getContents(location);
        List<String> sentence = new ArrayList<String>();
        for(String str: input){
            if(str == null || str.length() == 0) {
                sentences.add(sentence);
                sentence = new ArrayList<String>();
            } else {
                sentence.add(str);
            }
        }
        sentences.add(sentence);//calling it once after for the last sentence
        return sentences;
    }

    @Override
    public List<String> getContents(String fileLocation) throws IOException {
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
