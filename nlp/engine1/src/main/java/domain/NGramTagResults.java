package domain;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * The NGramTagResults reresents the results after training the WordTagger
 *
 * @author Sang Venkatraman
 *
 */
public class NGramTagResults extends TagResults {

    Map<NGramTag,Integer> unigramTagCountMap =  new LinkedHashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> bigramTagCountMap =  new LinkedHashMap<NGramTag, Integer>();

    Map<NGramTag,Integer> trigramTagCountMap =  new LinkedHashMap<NGramTag, Integer>();

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

}
