package domain;

import domain.Sentence.WordTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * The TagResults reresents the results after training the WordTagger
 *
 * @author Sang Venkatraman
 *
 */
public class TagResults {

    Map<WordTag,Integer> wordTagCountMap = new LinkedHashMap<Sentence.WordTag, Integer>();

    Map<String,Integer> tagCountMap = new LinkedHashMap<String, Integer>();

    Map<String,Integer> wordCountMap = new LinkedHashMap<String, Integer>();

    Map<NGramTag,Integer> unigramTagCountMap =  new LinkedHashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> bigramTagCountMap =  new LinkedHashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> trigramTagCountMap =  new LinkedHashMap<NGramTag, Integer>();

    List<String> words = new ArrayList<String>();

    List<String> tags = new ArrayList<String>();

    List<String> wordTags = new ArrayList<String>();

    /**
     * Get trigram count map
     * @return
     */
    public Map<NGramTag, Integer> getTrigramTagCountMap() {
        return trigramTagCountMap;
    }

    /**
     * Get unigram count map
     * @return
     */
    public Map<NGramTag, Integer> getUnigramTagCountMap() {
        return unigramTagCountMap;
    }

    /**
     * Get bigram count map
     * @return
     */
    public Map<NGramTag, Integer> getBigramTagCountMap() {
        return bigramTagCountMap;
    }

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
