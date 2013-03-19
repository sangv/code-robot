package domain;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author Sang Venkatraman
 *
 */
public class WordTagCounts {

    Map<WordTag,Integer> wordTagCountMap = new HashMap<WordTag, Integer>();

    Map<NGramTag,Integer> oneGramCountMap =  new HashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> twoGramCountMap =  new HashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> threeGramCountMap =  new HashMap<NGramTag, Integer>();

    public Map<NGramTag, Integer> getThreeGramCountMap() {
        return threeGramCountMap;
    }

    public void setThreeGramCountMap(Map<NGramTag, Integer> threeGramCountMap) {
        this.threeGramCountMap = threeGramCountMap;
    }

    public Map<WordTag, Integer> getWordTagCountMap() {
        return wordTagCountMap;
    }

    public void setWordTagCountMap(Map<WordTag, Integer> wordTagCountMap) {
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
}
