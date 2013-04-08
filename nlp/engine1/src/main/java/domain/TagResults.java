package domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This ${TYPE}
 *
 * @author Sang Venkatraman
 */
public class TagResults {

    private Map<Sentence.WordTag,Integer> wordTagCountMap = new LinkedHashMap<Sentence.WordTag, Integer>();

    private Map<String,Integer> tagCountMap = new LinkedHashMap<String, Integer>();

    private Map<String,Integer> wordCountMap = new LinkedHashMap<String, Integer>();

    private List<String> words = new ArrayList<String>();

    private List<String> tags = new ArrayList<String>();

    private List<String> wordTags = new ArrayList<String>();

    /**
     * Get count of word tags
     * @return
     */
    public Map<Sentence.WordTag, Integer> getWordTagCountMap() {
        return wordTagCountMap;
    }

    /**
     * Get count of tags keyed by the tag
     * @return
     */
    public Map<String, Integer> getTagCountMap() {
        return tagCountMap;
    }

    /**
     * Get count of words keyed by the word
     * @return
     */
    public Map<String, Integer> getWordCountMap() {
        return wordCountMap;
    }

    /**
     * Get all unique words in result
     * @return
     */
    public List<String> getWords() {
        return words;
    }

    /**
     * Get all wordtags as a string
     * @return
     */
    public List<String> getWordTags() {
        return wordTags;
    }

    /**
     * Get all the tags
     * @return
     */
    public List<String> getTags() {
        return tags;
    }
}
