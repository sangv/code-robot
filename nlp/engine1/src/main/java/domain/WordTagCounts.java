package domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author Sang Venkatraman
 *
 */
public class WordTagCounts {

    Map<TaggedSentence.WordTag,Integer> wordTagCountMap = new LinkedHashMap<TaggedSentence.WordTag, Integer>();

    Map<String,Integer> tagCountMap = new LinkedHashMap<String, Integer>();

    Map<String,Integer> wordCountMap = new LinkedHashMap<String, Integer>();

    Map<NGramTag,Integer> oneGramCountMap =  new LinkedHashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> twoGramCountMap =  new LinkedHashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> threeGramCountMap =  new LinkedHashMap<NGramTag, Integer>();

    List<String> words = new ArrayList<String>();

    List<String> tags = new ArrayList<String>();

    List<String> wordTags = new ArrayList<String>();

    public Map<NGramTag, Integer> getThreeGramCountMap() {
        return threeGramCountMap;
    }

    public void setThreeGramCountMap(Map<NGramTag, Integer> threeGramCountMap) {
        this.threeGramCountMap = threeGramCountMap;
    }

    public Map<TaggedSentence.WordTag, Integer> getWordTagCountMap() {
        return wordTagCountMap;
    }

    public void setWordTagCountMap(Map<TaggedSentence.WordTag, Integer> wordTagCountMap) {
        this.wordTagCountMap = wordTagCountMap;
    }

    public Map<NGramTag, Integer> getOneGramCountMap() {
        return oneGramCountMap;
    }

    public void setOneGramCountMap(Map<NGramTag, Integer> oneGramCountMap) {
        this.oneGramCountMap = oneGramCountMap;
    }

    public Map<NGramTag, Integer> getTwoGramCountMap() {
        return twoGramCountMap;
    }

    public void setTwoGramCountMap(Map<NGramTag, Integer> twoGramCountMap) {
        this.twoGramCountMap = twoGramCountMap;
    }

    public Map<String, Integer> getTagCountMap() {
        return tagCountMap;
    }

    public Map<String, Integer> getWordCountMap() {
        return wordCountMap;
    }

    public List<String> getWords() {
        return words;
    }

    public List<String> getWordTags() {
        return wordTags;
    }

    public List<String> getTags() {
        return tags;
    }
}
